package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 启动时把历史明文敏感参数迁移为 AES-GCM 密文。
 * 已是 ENC: 前缀的数据不会重复加密。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysConfigEncryptionMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final SysConfigService sysConfig;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select config_id, config_value, default_value from sys_config where is_sensitive=1");
            int changed = 0;
            for (Map<String, Object> row : rows) {
                Long id = ((Number) row.get("config_id")).longValue();
                String value = row.get("config_value") == null ? null : String.valueOf(row.get("config_value"));
                String defaultValue = row.get("default_value") == null ? null : String.valueOf(row.get("default_value"));
                String encryptedValue = value == null ? null : sysConfig.encrypt(value);
                String encryptedDefault = defaultValue == null ? null : sysConfig.encrypt(defaultValue);
                if (!java.util.Objects.equals(value, encryptedValue)
                        || !java.util.Objects.equals(defaultValue, encryptedDefault)) {
                    jdbc.update("update sys_config set config_value=?, default_value=? where config_id=?",
                            encryptedValue, encryptedDefault, id);
                    changed++;
                }
            }
            List<Map<String, Object>> logs = jdbc.queryForList(
                    "select l.log_id, l.old_value, l.new_value from sys_config_log l " +
                            "join sys_config c on c.config_id=l.config_id where c.is_sensitive=1");
            for (Map<String, Object> log : logs) {
                Long id = ((Number) log.get("log_id")).longValue();
                String oldValue = log.get("old_value") == null ? null : String.valueOf(log.get("old_value"));
                String newValue = log.get("new_value") == null ? null : String.valueOf(log.get("new_value"));
                String encryptedOld = oldValue == null ? null : sysConfig.encrypt(oldValue);
                String encryptedNew = newValue == null ? null : sysConfig.encrypt(newValue);
                if (!java.util.Objects.equals(oldValue, encryptedOld)
                        || !java.util.Objects.equals(newValue, encryptedNew)) {
                    jdbc.update("update sys_config_log set old_value=?, new_value=? where log_id=?",
                            encryptedOld, encryptedNew, id);
                }
            }
            sysConfig.invalidateAll();
            if (changed > 0) log.info("敏感参数加密迁移完成，共 {} 项", changed);
        } catch (Exception e) {
            log.warn("敏感参数加密迁移跳过: {}", e.getMessage());
        }
    }
}
