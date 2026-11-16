package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 敏感参数密钥轮换：用旧密钥解密，再用新密钥重新加密。
 * 调用完成后必须用新的 CONFIG_SECRET 重启应用。
 */
@Service
@RequiredArgsConstructor
public class ConfigKeyRotationService {

    private final JdbcTemplate jdbc;
    private final SysConfigCipher cipher;

    @Transactional
    public int rotate(String newSecret) {
        if (newSecret == null || newSecret.length() < 32) {
            throw BizException.badRequest("新密钥至少 32 位");
        }
        SysConfigCipher next = new SysConfigCipher(newSecret, "");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select config_id, config_value, default_value from sys_config where is_sensitive=1");
        int changed = 0;
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("config_id")).longValue();
            String value = row.get("config_value") == null ? null : String.valueOf(row.get("config_value"));
            String defaultValue = row.get("default_value") == null ? null : String.valueOf(row.get("default_value"));
            String plain = value == null ? null : cipher.decrypt(value);
            String defaultPlain = defaultValue == null ? null : cipher.decrypt(defaultValue);
            jdbc.update("update sys_config set config_value=?, default_value=? where config_id=?",
                    plain == null ? null : next.encrypt(plain),
                    defaultPlain == null ? null : next.encrypt(defaultPlain), id);
            changed++;
        }

        List<Map<String, Object>> logs = jdbc.queryForList(
                "select l.log_id, l.old_value, l.new_value from sys_config_log l " +
                        "join sys_config c on c.config_id=l.config_id where c.is_sensitive=1");
        for (Map<String, Object> log : logs) {
            Long id = ((Number) log.get("log_id")).longValue();
            String oldValue = log.get("old_value") == null ? null : String.valueOf(log.get("old_value"));
            String newValue = log.get("new_value") == null ? null : String.valueOf(log.get("new_value"));
            String oldPlain = oldValue == null ? null : decryptQuietly(oldValue);
            String newPlain = newValue == null ? null : decryptQuietly(newValue);
            jdbc.update("update sys_config_log set old_value=?, new_value=? where log_id=?",
                    oldPlain == null ? null : next.encrypt(oldPlain),
                    newPlain == null ? null : next.encrypt(newPlain), id);
        }
        return changed;
    }

    private String decryptQuietly(String value) {
        try {
            return cipher.decrypt(value);
        } catch (Exception e) {
            return value;
        }
    }
}
