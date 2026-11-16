package com.laminar.ops.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统级告警：写入平台管理员站内消息，并对相同错误做短时间抑制，避免刷屏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAlertService {

    private static final long SUPPRESS_MS = 5 * 60_000L;
    private final JdbcTemplate jdbc;
    private final Map<String, Long> lastAlert = new ConcurrentHashMap<>();

    public void error(String fingerprint, String title, String content) {
        try {
            long now = System.currentTimeMillis();
            Long last = lastAlert.get(fingerprint);
            if (last != null && now - last < SUPPRESS_MS) return;
            lastAlert.put(fingerprint, now);
            if (lastAlert.size() > 500) {
                lastAlert.entrySet().removeIf(e -> now - e.getValue() > SUPPRESS_MS);
            }
            jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key," +
                            "receiver_id,receiver_role,is_read,create_time) " +
                            "values('system',?,?, '1', 'system', 0, null, 0, 'super_admin', '0', now())",
                    title, content == null ? "" : content.substring(0, Math.min(content.length(), 950)));
        } catch (Exception e) {
            log.warn("系统告警写入失败: {}", e.getMessage());
        }
    }
}
