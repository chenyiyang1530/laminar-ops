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
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(m);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", m);
        result.put("cutoff", cutoff);
        result.put("alarmEligible", count(
                "select count(*) from biz_alarm where status in ('3','4') and first_time < ?", cutoff));
        result.put("workorderEligible", count(
                "select count(*) from biz_workorder where status in ('5','6') and create_time < ?", cutoff));
        result.put("alarmArchived", count("select count(*) from biz_alarm_archive"));
        result.put("workorderArchived", count("select count(*) from biz_workorder_archive"));
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

    private int archive(String table, String archiveTable, String condition, LocalDateTime cutoff) {
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
                cutoff);
        if (inserted == 0) return 0;
        return jdbc.update("delete from `" + table + "` where " + condition, cutoff);
    }

    private void ensureArchiveTable(String source, String archive) {
        jdbc.execute("create table if not exists `" + archive + "` like `" + source + "`");
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
