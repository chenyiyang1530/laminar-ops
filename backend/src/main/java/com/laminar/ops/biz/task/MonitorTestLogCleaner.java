package com.laminar.ops.biz.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每日清理过期的设备接入测试记录，默认保留 90 天。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorTestLogCleaner {

    private final JdbcTemplate jdbc;

    @Value("${laminar.monitor.test-log-retain-days:90}")
    private int retainDays;

    @Scheduled(cron = "0 30 2 * * ?")
    public void clean() {
        int days = retainDays <= 0 ? 90 : retainDays;
        int removed = jdbc.update("delete from biz_monitor_test_log where create_time < date_sub(now(), interval " + days + " day)");
        if (removed > 0) log.info("[设备接入] 清理 {} 条过期测试记录，保留 {} 天", removed, days);
    }
}
