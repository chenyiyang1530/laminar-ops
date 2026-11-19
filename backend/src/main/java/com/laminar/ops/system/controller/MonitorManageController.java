package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.RedisLaminarHealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@RestController
@RequestMapping("/api/system/monitor")
@RequiredArgsConstructor
public class MonitorManageController {

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;
    private final RedisLaminarHealthIndicator redisHealth;

    @GetMapping("/online")
    @Perm("monitor:server:view")
    public Result<List<Map<String, Object>>> online() {
        if (!AuthContext.require().isSuperAdmin()) {
            throw BizException.forbidden("只有系统管理员可以查看在线用户");
        }
        Set<String> keys = new LinkedHashSet<>();
        redis.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions().match("online:*").count(200).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return null;
        });
        List<Map<String, Object>> list = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                String v = redis.opsForValue().get(key);
                if (v == null) continue;
                String[] parts = v.split("\\|", -1);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", key.substring("online:".length()));
                row.put("username", parts.length > 0 ? parts[0] : "");
                row.put("nickName", parts.length > 1 ? parts[1] : "");
                row.put("ip", parts.length > 2 ? parts[2] : "");
                row.put("loginTime", parts.length > 3 ? parts[3] : "");
                row.put("location", parts.length > 4 ? parts[4] : "—");
                row.put("browser", parts.length > 5 ? parts[5] : "—");
                row.put("os", parts.length > 6 ? parts[6] : "—");
                row.put("ttl", redis.getExpire(key));
                list.add(row);
            }
        }
        return Result.ok(list);
    }

    @DeleteMapping("/online/{userId}")
    @Perm("monitor:server:view")
    public Result<Void> kick(@PathVariable Long userId) {
        if (!AuthContext.require().isSuperAdmin()) {
            throw BizException.forbidden("只有系统管理员可以强制退出在线用户");
        }
        redis.delete("online:" + userId);
        // 旧 JWT 仍是有效签名，用 12 小时墓碑覆盖令牌剩余有效期，防止被踢后继续访问。
        redis.opsForValue().set("kicked:" + userId, "1", java.time.Duration.ofHours(12));
        return Result.ok();
    }

    @GetMapping("/server")
    @Perm("monitor:server:view")
    public Result<Map<String, Object>> server() {
        Map<String, Object> map = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        map.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        map.put("javaVersion", System.getProperty("java.version"));
        map.put("javaVendor", System.getProperty("java.vendor"));
        map.put("processors", rt.availableProcessors());
        map.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        map.put("totalMemory", rt.totalMemory());
        map.put("freeMemory", rt.freeMemory());
        map.put("maxMemory", rt.maxMemory());
        map.put("appStart", ManagementFactory.getRuntimeMXBean().getStartTime());
        return Result.ok(map);
    }

    @GetMapping("/cache")
    @Perm("monitor:cache:view")
    public Result<Map<String, Object>> cache() {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            Properties info = redis.getConnectionFactory().getConnection().info();
            Map<String, String> plain = new LinkedHashMap<>();
            for (String name : info.stringPropertyNames()) {
                plain.put(name, info.getProperty(name));
            }
            map.put("info", plain);
            map.put("keyCount", plain.containsKey("db0") ? plain.get("db0") : "");
        } catch (Exception e) {
            map.put("error", e.getMessage());
        }
        return Result.ok(map);
    }

    @GetMapping("/redis-health")
    @Perm("monitor:cache:view")
    public Result<Map<String, Object>> redisHealth() {
        var health = redisHealth.health();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", health.getStatus().getCode());
        map.put("details", health.getDetails());
        return Result.ok(map);
    }

    @GetMapping("/data")
    @Perm("monitor:data:view")
    public Result<List<Map<String, Object>>> data() {
        return Result.ok(jdbc.queryForList(
                "select table_name as `tableName`, table_rows as `tableRows`, " +
                        "round((data_length+index_length)/1024/1024, 2) as `sizeMb`, table_comment as `tableComment` " +
                        "from information_schema.tables where table_schema='laminar_ops' order by table_rows desc"));
    }
}
