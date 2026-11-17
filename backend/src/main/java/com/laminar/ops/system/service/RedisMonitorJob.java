package com.laminar.ops.system.service;

import com.laminar.ops.common.SystemAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Redis 内存、AOF 数据盘使用率巡检。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMonitorJob {

    private final StringRedisTemplate redis;
    private final SystemAlertService systemAlertService;

    @Value("${laminar.redis.monitor-enabled:true}")
    private boolean enabled;

    @Value("${laminar.redis.data-dir:D:/ideaWork/redis8-service/data}")
    private String dataDir;

    @Value("${laminar.redis.memory-warning-percent:80}")
    private int memoryWarningPercent;

    @Value("${laminar.redis.disk-warning-percent:85}")
    private int diskWarningPercent;

    @Scheduled(fixedDelay = 300_000L, initialDelay = 60_000L)
    public void check() {
        if (!enabled) return;
        checkMemory();
        checkDisk();
    }

    private void checkMemory() {
        try {
            Properties info = redis.execute((RedisCallback<Properties>) RedisConnection::info);
            if (info == null) return;
            long used = parseLong(info.getProperty("used_memory"));
            long max = parseLong(info.getProperty("maxmemory"));
            if (max <= 0) return;
            double percent = used * 100.0 / max;
            if (percent >= memoryWarningPercent) {
                systemAlertService.error("redis-memory-high", "Redis 内存告警",
                        String.format("Redis 已使用 %.1f%%：%s / %s，淘汰策略 %s",
                                percent, human(used), human(max), info.getProperty("maxmemory_policy")));
            }
        } catch (Exception e) {
            log.debug("Redis 内存巡检不可用: {}", e.getMessage());
        }
    }

    private void checkDisk() {
        try {
            Path path = Path.of(dataDir);
            if (!Files.exists(path)) return;
            FileStore store = Files.getFileStore(path);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            if (total <= 0) return;
            double usedPercent = (total - usable) * 100.0 / total;
            if (usedPercent >= diskWarningPercent) {
                systemAlertService.error("redis-disk-high", "Redis 磁盘告警",
                        String.format("Redis 数据盘已使用 %.1f%%：剩余 %s，目录 %s",
                                usedPercent, human(usable), dataDir));
            }
        } catch (Exception e) {
            log.debug("Redis 磁盘巡检不可用: {}", e.getMessage());
        }
    }

    private long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String human(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) return String.format("%.1f GB", bytes / 1024.0 / 1024 / 1024);
        if (bytes >= 1024L * 1024) return String.format("%.1f MB", bytes / 1024.0 / 1024);
        return String.format("%.1f KB", bytes / 1024.0);
    }
}
