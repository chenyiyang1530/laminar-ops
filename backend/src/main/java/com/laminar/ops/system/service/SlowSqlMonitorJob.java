package com.laminar.ops.system.service;

import com.laminar.ops.common.SystemAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 基于 MySQL performance_schema 的慢 SQL 巡检。
 * 需要数据库开启 performance_schema（MySQL 8 默认开启）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlowSqlMonitorJob {

    private final JdbcTemplate jdbc;
    private final SystemAlertService systemAlertService;

    @Value("${laminar.monitor.slow-sql-enabled:true}")
    private boolean enabled;

    @Value("${laminar.monitor.slow-sql-threshold-ms:1000}")
    private long thresholdMs;

    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void checkSlowSql() {
        if (!enabled || thresholdMs <= 0) return;
        try {
            long thresholdPico = thresholdMs * 1_000_000_000L;
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select digest_text as `sql`, count_star as `cnt`, avg_timer_wait as `avg`, max_timer_wait as `max` " +
                            "from performance_schema.events_statements_summary_by_digest " +
                            "where schema_name=database() and avg_timer_wait > ? and count_star > 0 " +
                            "order by avg_timer_wait desc limit 5", thresholdPico);
            for (Map<String, Object> row : rows) {
                String sql = String.valueOf(row.get("sql"));
                if (sql == null || sql.isBlank()) continue;
                long cnt = ((Number) row.get("cnt")).longValue();
                double avgMs = ((Number) row.get("avg")).doubleValue() / 1_000_000_000.0;
                double maxMs = ((Number) row.get("max")).doubleValue() / 1_000_000_000.0;
                String shortSql = sql.replaceAll("\\s+", " ").trim();
                if (shortSql.length() > 220) shortSql = shortSql.substring(0, 220) + "...";
                systemAlertService.error("slow-sql:" + Integer.toHexString(sql.hashCode()),
                        "慢 SQL 告警",
                        "执行 " + cnt + " 次，平均 " + String.format("%.0f", avgMs) + "ms，最大 "
                                + String.format("%.0f", maxMs) + "ms\n" + shortSql);
            }
        } catch (Exception e) {
            log.debug("慢 SQL 巡检不可用: {}", e.getMessage());
        }
    }
}
