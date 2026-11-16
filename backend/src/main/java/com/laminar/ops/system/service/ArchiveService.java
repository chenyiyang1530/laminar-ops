package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final JdbcTemplate jdbc;

    @Value("${laminar.archive.retention-months:6}")
    private int autoRetentionMonths;

    @Transactional
    public void autoArchive() {
        if (autoRetentionMonths < 1) {
            log.info("[归档] 自动归档已关闭（retention-months={}）", autoRetentionMonths);
            return;
        }
        Map<String, Object> result = run(autoRetentionMonths, true, true);
        log.info("[归档] 自动归档完成，保留 {} 个月，结果：{}", autoRetentionMonths, result);
    }

    public Map<String, Object> stats(Integer months) {
        int m = normalizeMonths(months);
        ensureArchiveTable("biz_alarm", "biz_alarm_archive");
        ensureArchiveTable("biz_workorder", "biz_workorder_archive");
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(m);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", m);
        result.put("cutoff", cutoff);
        result.put("alarmEligible", count(
                "select count(*) from biz_alarm where status in ('3','4') and first_time < ?", cutoff));
        result.put("workorderEligible", count(
                "select count(*) from biz_workorder where status in ('5','6') and create_time < ?", cutoff));
        result.put("alarmClosed", count("select count(*) from biz_alarm where status in ('3','4')"));
        result.put("workorderClosed", count("select count(*) from biz_workorder where status in ('5','6')"));
        result.put("alarmArchived", count("select count(*) from biz_alarm_archive"));
        result.put("workorderArchived", count("select count(*) from biz_workorder_archive"));
        return result;
    }

    public Map<String, Object> archivedAlarms(int pageNum, int pageSize, String keyword) {
        ensureArchiveTable("biz_alarm", "biz_alarm_archive");
        int page = Math.max(1, pageNum);
        int size = Math.max(1, Math.min(100, pageSize));
        StringBuilder where = new StringBuilder(" where 1=1 ");
        List<Object> args = new java.util.ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (a.alarm_no like ? or a.content like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        Long total = jdbc.queryForObject("select count(*) from biz_alarm_archive a" + where, Long.class, args.toArray());
        List<Object> pageArgs = new java.util.ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((page - 1) * size);
        List<Map<String, Object>> records = jdbc.queryForList(
                "select a.alarm_id as `alarmId`, a.alarm_no as `alarmNo`, a.content, a.level, a.status, " +
                        "date_format(a.first_time,'%Y-%m-%d %H:%i') as `firstTime`, " +
                        "date_format(a.archived_at,'%Y-%m-%d %H:%i') as `archivedAt`, " +
                        "a.equipment_id as `equipmentId`, e.equipment_name as `equipmentName`, " +
                        "h.hospital_name as `hospitalName` " +
                        "from biz_alarm_archive a " +
                        "left join biz_equipment e on e.equipment_id=a.equipment_id " +
                        "left join biz_hospital h on h.hospital_id=a.hospital_id" +
                        where + " order by a.archived_at desc, a.alarm_id desc limit ? offset ?",
                pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> archivedWorkorders(int pageNum, int pageSize, String keyword) {
        ensureArchiveTable("biz_workorder", "biz_workorder_archive");
        int page = Math.max(1, pageNum);
        int size = Math.max(1, Math.min(100, pageSize));
        StringBuilder where = new StringBuilder(" where 1=1 ");
        List<Object> args = new java.util.ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (w.workorder_no like ? or w.title like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        Long total = jdbc.queryForObject("select count(*) from biz_workorder_archive w" + where, Long.class, args.toArray());
        List<Object> pageArgs = new java.util.ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((page - 1) * size);
        List<Map<String, Object>> records = jdbc.queryForList(
                "select w.workorder_id as `workorderId`, w.workorder_no as `workorderNo`, w.title, w.status, " +
                        "date_format(w.create_time,'%Y-%m-%d %H:%i') as `createTime`, " +
                        "date_format(w.finish_time,'%Y-%m-%d %H:%i') as `finishTime`, " +
                        "date_format(w.archived_at,'%Y-%m-%d %H:%i') as `archivedAt`, " +
                        "h.hospital_name as `hospitalName`, c.company_name as `companyName` " +
                        "from biz_workorder_archive w " +
                        "left join biz_hospital h on h.hospital_id=w.hospital_id " +
                        "left join biz_company c on c.company_id=w.company_id" +
                        where + " order by w.archived_at desc, w.workorder_id desc limit ? offset ?",
                pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    @Transactional
    public void restoreAlarm(Long id) {
        restore("biz_alarm", "biz_alarm_archive", "alarm_id", id);
    }

    @Transactional
    public void restoreWorkorder(Long id) {
        restore("biz_workorder", "biz_workorder_archive", "workorder_id", id);
    }

    private void restore(String table, String archiveTable, String pk, Long id) {
        ensureArchiveTable(table, archiveTable);
        Integer archived = jdbc.queryForObject(
                "select count(*) from `" + archiveTable + "` where `" + pk + "`=?", Integer.class, id);
        if (archived == null || archived == 0) throw BizException.badRequest("归档数据不存在");
        List<String> columns = jdbc.queryForList(
                "select column_name from information_schema.columns where table_schema=database() and table_name=? " +
                        "and extra not like '%GENERATED%' order by ordinal_position", String.class, table);
        String cols = columns.stream().map(c -> "`" + c + "`").reduce((a, b) -> a + "," + b).orElseThrow();
        try {
            jdbc.update("insert into `" + table + "` (" + cols + ") select " + cols +
                    " from `" + archiveTable + "` where `" + pk + "`=?", id);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw BizException.badRequest("业务表中已存在该数据，无法恢复");
        }
        jdbc.update("delete from `" + archiveTable + "` where `" + pk + "`=?", id);
    }

    @Transactional
    public Map<String, Object> runNow(Boolean alarms, Boolean workorders) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (Boolean.TRUE.equals(alarms)) {
            result.put("alarmArchived", archive(
                    "biz_alarm", "biz_alarm_archive", "status in ('3','4')"));
        }
        if (Boolean.TRUE.equals(workorders)) {
            result.put("workorderArchived", archive(
                    "biz_workorder", "biz_workorder_archive", "status in ('5','6')"));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> run(Integer months, Boolean alarms, Boolean workorders) {
        int m = normalizeMonths(months);
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(m);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", m);
        if (Boolean.TRUE.equals(alarms)) {
            result.put("alarmArchived", archive(
                    "biz_alarm", "biz_alarm_archive",
                    "status in ('3','4') and first_time < ?", cutoff));
        }
        if (Boolean.TRUE.equals(workorders)) {
            result.put("workorderArchived", archive(
                    "biz_workorder", "biz_workorder_archive",
                    "status in ('5','6') and create_time < ?", cutoff));
        }
        return result;
    }

    private int archive(String table, String archiveTable, String condition, Object... args) {
        ensureArchiveTable(table, archiveTable);
        List<String> columns = jdbc.queryForList(
                "select column_name from information_schema.columns " +
                        "where table_schema=database() and table_name=? and extra not like '%GENERATED%' " +
                        "order by ordinal_position", String.class, table);
        if (columns.isEmpty()) throw BizException.badRequest("源表不存在：" + table);
        String cols = columns.stream().map(c -> "`" + c + "`").reduce((a, b) -> a + "," + b).orElseThrow();
        String placeholders = columns.stream().map(c -> "`" + c + "`").reduce((a, b) -> a + "," + b).orElseThrow();
        int inserted = jdbc.update(
                "insert ignore into `" + archiveTable + "` (" + cols + ",archived_at) " +
                        "select " + placeholders + ",now() from `" + table + "` where " + condition,
                args);
        if (inserted == 0) return 0;
        return jdbc.update("delete from `" + table + "` where " + condition, args);
    }

    private void ensureArchiveTable(String source, String archive) {
        jdbc.execute("create table if not exists `" + archive + "` like `" + source + "`");
        // 源表后续新增字段时，已有归档表不会自动同步；按源表补齐缺失列。
        List<Map<String, Object>> missing = jdbc.queryForList(
                "select c.column_name as `name`, c.column_type as `type` from information_schema.columns c " +
                        "where c.table_schema=database() and c.table_name=? and c.extra not like '%GENERATED%' " +
                        "and not exists (select 1 from information_schema.columns a where a.table_schema=database() " +
                        "and a.table_name=? and a.column_name=c.column_name) order by c.ordinal_position",
                source, archive);
        for (Map<String, Object> column : missing) {
            jdbc.execute("alter table `" + archive + "` add column `" + column.get("name") + "` " +
                    column.get("type") + " null");
        }
        Integer hasColumn = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_schema=database() " +
                        "and table_name=? and column_name='archived_at'", Integer.class, archive);
        if (hasColumn == null || hasColumn == 0) {
            jdbc.execute("alter table `" + archive + "` add column `archived_at` datetime default null");
        }
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private int normalizeMonths(Integer months) {
        if (months == null) return 6;
        if (months < 1 || months > 24) throw BizException.badRequest("保留月份需在 1-24 之间");
        return months;
    }
}
