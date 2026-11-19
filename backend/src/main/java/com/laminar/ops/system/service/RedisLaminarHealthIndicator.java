package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

import java.util.Properties;

/**
 * 应用级 Redis 健康检查：PING、延迟、内存占用、慢查询条数。
 * 该检查会出现在 /actuator/health 中，Redis 不可用时应用健康状态为 DOWN。
 */
@Slf4j
@Component("redisLaminarHealthIndicator")
@RequiredArgsConstructor
public class RedisLaminarHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redis;

    @Override
    public Health health() {
        long started = System.currentTimeMillis();
        try {
            String pong = redis.execute((RedisCallback<String>) RedisConnection::ping);
            long latency = System.currentTimeMillis() - started;
            Properties info = redis.execute((RedisCallback<Properties>) connection -> connection.info("memory"));
            long used = parseLong(info == null ? null : info.getProperty("used_memory"));
            long max = parseLong(info == null ? null : info.getProperty("maxmemory"));
            long slowLog = slowLogCount();
            Health.Builder builder = "PONG".equalsIgnoreCase(pong) ? Health.up() : Health.down();
            return builder
                    .withDetail("latencyMs", latency)
                    .withDetail("usedMemoryBytes", used)
                    .withDetail("maxMemoryBytes", max)
                    .withDetail("memoryPercent", max > 0 ? Math.round(used * 1000.0 / max) / 10.0 : 0)
                    .withDetail("slowLogCount", slowLog)
                    .withDetail("slowLogWarning", slowLog > 0)
                    .build();
        } catch (Exception e) {
            return Health.down(e).withDetail("latencyMs", System.currentTimeMillis() - started).build();
        }
    }

    public long slowLogCount() {
        try {
            Long count = redis.execute((RedisCallback<Long>) connection -> {
                Object nativeConnection = connection.getNativeConnection();
                if (!(nativeConnection instanceof StatefulRedisConnection<?, ?> stateful)) {
                    return null;
                }
                @SuppressWarnings({"rawtypes", "unchecked"})
                RedisCommands<String, String> commands = (RedisCommands) stateful.sync();
                CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
                args.add("LEN");
                Long value = commands.dispatch(CommandType.SLOWLOG, new IntegerOutput<>(StringCodec.UTF8), args);
                return value;
            });
            return count == null ? 0 : count;
        } catch (Exception e) {
            log.warn("Redis SLOWLOG 读取失败: {}", e.getMessage());
            return -1;
        }
    }

    private long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
