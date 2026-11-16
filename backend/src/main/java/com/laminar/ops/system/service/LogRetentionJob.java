package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台日志保留策略：先把超期日志归档，再删除原表数据。
 * 归档表和原表结构一致，并额外记录 archived_at。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogRetentionJob {

    private final JdbcTemplate jdbc;
    private final SysConfigService sysConfig;

    @Scheduled(cron = "0 30 3 * * ?")
    public void scheduled() {
        cleanup();
    }

    @Transactional
    public java.util.Map<String, Object> cleanup() {
        int loginDays = Math.max(0, sysConfig.intValue("platform.login.logRetentionDays", 180));
        int auditDays = Math.max(0, sysConfig.intValue("platform.audit.logRetentionDays", 180));
        int configDays = Math.max(0, sysConfig.intValue("platform.config.logRetentionDays", 180));

        int login = archiveAndDelete("sys_login_log", "sys_login_log_archive", "login_time", loginDays);
        int audit = archiveAndDelete("sys_oper_log", "sys_oper_log_archive", "oper_time", auditDays);
        int config = archiveAndDelete("sys_config_log", "sys_config_log_archive", "create_time", configDays);

        if (login + audit + config > 0) {
            log.info("日志归档完成：登录 {} 条，操作 {} 条，参数 {} 条", login, audit, config);
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("loginArchived", login);
        result.put("auditArchived", audit);
        result.put("configArchived", config);
        return result;
    }

    private int archiveAndDelete(String source, String archive, String timeColumn, int days) {
        if (days <= 0) return 0;
        ensureArchiveTable(source, archive);
        String condition = "`" + timeColumn + "` < date_sub(now(), interval " + days + " day)";
        int archived = jdbc.update(
                "insert ignore into `" + archive + "` select *, now() from `" + source + "` where " + condition);
        int deleted = jdbc.update("delete from `" + source + "` where " + condition);
        if (archived != deleted) {
            log.warn("[日志归档] {}.{} 归档 {} 条，删除 {} 条，数量不一致", source, timeColumn, archived, deleted);
        }
        return deleted;
    }

    private void ensureArchiveTable(String source, String archive) {
        jdbc.execute("create table if not exists `" + archive + "` like `" + source + "`");
        Integer hasColumn = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_schema=database() " +
                        "and table_name=? and column_name='archived_at'", Integer.class, archive);
        if (hasColumn == null || hasColumn == 0) {
            jdbc.execute("alter table `" + archive + "` add column `archived_at` datetime null");
        }
    }
}
