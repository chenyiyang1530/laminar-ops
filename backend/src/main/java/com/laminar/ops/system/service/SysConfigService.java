package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

/**
 * 读取 sys_config 中的系统参数；参数不存在或表未初始化时使用代码默认值。
 * 参数读多写少，做 60 秒本地缓存；Redis 中的版本号变化后多实例会主动失效。
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private static final long CACHE_TTL_MS = 60_000L;
    private static final String VERSION_KEY = "sys:config:version";
    private final JdbcTemplate jdbc;
    private final SysConfigCipher cipher;
    private final StringRedisTemplate redis;
    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();

    public String value(String key, String fallback) {
        long now = System.currentTimeMillis();
        long version = currentVersion();
        CachedValue cached = cache.get(key);
        if (cached != null && cached.version() == version && cached.expireAt() > now) {
            return cached.value();
        }
        String value = fallback;
        try {
            List<String> values = jdbc.queryForList(
                    "select config_value from sys_config where config_key=?", String.class, key);
            if (!values.isEmpty() && values.get(0) != null) {
                value = cipher.isEncrypted(values.get(0)) ? cipher.decrypt(values.get(0)) : values.get(0);
            }
        } catch (Exception e) {
            // 参数表尚未初始化或密钥不匹配时使用代码默认值
        }
        cache.put(key, new CachedValue(value, now + CACHE_TTL_MS, version));
        return value;
    }

    private long currentVersion() {
        try {
            String value = redis.opsForValue().get(VERSION_KEY);
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private void bumpVersion() {
        try {
            redis.opsForValue().increment(VERSION_KEY);
            redis.expire(VERSION_KEY, Duration.ofDays(30));
        } catch (Exception ignored) {
            // Redis 不可用时仅失效本机缓存
        }
    }

    public String encrypt(String plaintext) {
        return cipher.encrypt(plaintext);
    }

    public String decrypt(String stored) {
        return cipher.decrypt(stored);
    }

    public boolean isEncrypted(String value) {
        return cipher.isEncrypted(value);
    }

    public boolean bool(String key, boolean fallback) {
        String value = value(key, String.valueOf(fallback)).trim();
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "Y".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value);
    }

    public int intValue(String key, int fallback) {
        try {
            return Integer.parseInt(value(key, String.valueOf(fallback)).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public void invalidate(String key) {
        if (key != null) cache.remove(key);
        bumpVersion();
    }

    public void invalidateAll() {
        cache.clear();
        bumpVersion();
    }

    private record CachedValue(String value, long expireAt, long version) {
    }
}
