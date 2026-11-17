package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件巡检查询、趋势和导出数据读取。
 */
@Service
@RequiredArgsConstructor
public class FileCheckQueryService {

    private final JdbcTemplate jdbc;

    public Map<String, Object> pageRuns(int pageNum, int pageSize) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select run_id as runId, trigger_type as triggerType, status, checked, missing, recovered, " +
                        "start_time as startTime, end_time as endTime, error_msg as errorMsg " +
                        "from sys_file_check_run order by run_id desc limit ? offset ?", pageSize, offset);
        Integer total = jdbc.queryForObject("select count(*) from sys_file_check_run", Integer.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> pageItems(int pageNum, int pageSize, String status, String keyword,
                                         Long companyId, Long hospitalId, String fileType, String severity, Long itemId) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (status != null && !status.isBlank()) { where.append(" and i.status=?"); args.add(status); }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (i.file_path like ? or i.table_name like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (companyId != null) { where.append(" and i.company_id=?"); args.add(companyId); }
        if (hospitalId != null) { where.append(" and i.hospital_id=?"); args.add(hospitalId); }
        if (fileType != null && !fileType.isBlank()) { where.append(" and i.file_type=?"); args.add(fileType); }
        if (severity != null && !severity.isBlank()) { where.append(" and i.severity=?"); args.add(severity); }
        if (itemId != null) { where.append(" and i.item_id=?"); args.add(itemId); }
        Integer total = jdbc.queryForObject("select count(*) from sys_file_check_item i" + where, Integer.class, args.toArray());
        args.add(pageSize);
        args.add(offset);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select i.item_id as itemId, i.run_id as runId, i.table_name as tableName, i.record_id as recordId, " +
                        "i.file_column as fileColumn, i.file_path as filePath, i.file_type as fileType, i.severity, " +
                        "i.sla_hours as slaHours, i.sla_deadline as slaDeadline, i.escalation_level as escalationLevel, " +
                        "i.company_id as companyId, i.hospital_id as hospitalId, i.status, " +
                        "i.first_seen as firstSeen, i.last_seen as lastSeen, i.resolved_time as resolvedTime, " +
                        "c.company_name as companyName, h.hospital_name as hospitalName " +
                        "from sys_file_check_item i left join biz_company c on c.company_id=i.company_id " +
                        "left join biz_hospital h on h.hospital_id=i.hospital_id" + where +
                        " order by i.status asc, i.severity asc, i.last_seen desc limit ? offset ?", args.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public List<Map<String, Object>> exportRows(String status, String keyword, Long companyId,
                                                Long hospitalId, String fileType, String severity) {
        return (List<Map<String, Object>>) pageItems(1, 10000, status, keyword,
                companyId, hospitalId, fileType, severity, null).get("records");
    }

    public List<Map<String, Object>> trend(int days, Long companyId, Long hospitalId, String granularity) {
        int span = Math.max(1, Math.min(days, 365));
        String mode = granularity == null ? "day" : granularity.toLowerCase();
        String format = "month".equals(mode) ? "%Y-%m" : "week".equals(mode) ? "%x-W%v" : "%Y-%m-%d";
        String group = "date_format(first_seen,'" + format + "')";
        String resolvedGroup = "date_format(resolved_time,'" + format + "')";
        LocalDate start = LocalDate.now().minusDays(span - 1L);
        Map<String, long[]> data = new LinkedHashMap<>();
        if ("day".equals(mode)) {
            for (int i = 0; i < span; i++) {
                data.put(start.plusDays(i).toString(), new long[]{0, 0});
            }
        }
        List<Object> args = new ArrayList<>();
        args.add(start);
        StringBuilder scope = new StringBuilder();
        if (companyId != null) { scope.append(" and company_id=?"); args.add(companyId); }
        if (hospitalId != null) { scope.append(" and hospital_id=?"); args.add(hospitalId); }
        jdbc.queryForList("select " + group + " d, count(*) c from sys_file_check_item " +
                        "where first_seen>=?" + scope + " group by " + group, args.toArray())
                .forEach(row -> data.computeIfAbsent(String.valueOf(row.get("d")), k -> new long[]{0, 0})[0]
                        = ((Number) row.get("c")).longValue());
        jdbc.queryForList("select " + resolvedGroup + " d, count(*) c from sys_file_check_item " +
                        "where resolved_time is not null and resolved_time>=?" + scope + " group by " + resolvedGroup, args.toArray())
                .forEach(row -> data.computeIfAbsent(String.valueOf(row.get("d")), k -> new long[]{0, 0})[1]
                        = ((Number) row.get("c")).longValue());
        List<Map<String, Object>> result = new ArrayList<>();
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", entry.getKey());
            item.put("missing", entry.getValue()[0]);
            item.put("recovered", entry.getValue()[1]);
            result.add(item);
        });
        return result;
    }
}
