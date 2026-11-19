package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.NestedMultiOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("laminar_redis_health", this, indicator -> {
            try {
                return "UP".equals(indicator.health().getStatus().getCode()) ? 1 : 0;
            } catch (Exception e) {
                return 0;
            }
        }).description("Redis8 application health: 1=UP, 0=DOWN").register(meterRegistry);
        Gauge.builder("laminar_redis_slowlog_count", this, indicator -> Math.max(0, indicator.slowLogCount()))
                .description("Redis8 SLOWLOG length").register(meterRegistry);
    }

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

    /**
     * 读取最近一条慢查询，供告警正文使用。返回可读摘要，不返回完整命令参数。
     */
    public String slowLogSummary() {
        try {
            return redis.execute((RedisCallback<String>) connection -> {
                Object nativeConnection = connection.getNativeConnection();
                if (!(nativeConnection instanceof RedisClusterAsyncCommands<?, ?> stateful)) return "无法读取 SLOWLOG";
                @SuppressWarnings({"rawtypes", "unchecked"})
                RedisClusterAsyncCommands<String, String> commands = (RedisClusterAsyncCommands) stateful;
                CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
                args.add("GET").add(1);
                List<Object> rows = await(commands.dispatch(CommandType.SLOWLOG,
                        new NestedMultiOutput<>(StringCodec.UTF8), args));
                if (rows == null || rows.isEmpty()) return "暂无慢查询";
                Object first = rows.get(0);
                if (!(first instanceof List<?> entry) || entry.size() < 4) return "暂无慢查询";
                long id = entry.get(0) instanceof Number n ? n.longValue() : 0;
                long timestamp = entry.get(1) instanceof Number n ? n.longValue() : 0;
                long micros = entry.get(2) instanceof Number n ? n.longValue() : 0;
                String command = entry.get(3) instanceof List<?> parts
                        ? parts.stream().limit(5).map(this::text).reduce((a, b) -> a + " " + b).orElse("")
                        : text(entry.get(3));
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp * 1000));
                return String.format("id=%d，时间=%s，耗时=%.1fms，命令=%s", id, time, micros / 1000.0,
                        command.length() > 120 ? command.substring(0, 120) + "..." : command);
            });
        } catch (Exception e) {
            log.warn("Redis SLOWLOG 摘要读取失败: {}", e.getMessage());
            return "SLOWLOG 摘要读取失败";
        }
    }

    private <T> T await(io.lettuce.core.RedisFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String text(Object value) {
        if (value == null) return "";
        if (value instanceof ByteBuffer buffer) {
            ByteBuffer copy = buffer.duplicate();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    public long slowLogCount() {
        try {
            Long count = redis.execute((RedisCallback<Long>) connection -> {
                Object nativeConnection = connection.getNativeConnection();
                if (!(nativeConnection instanceof RedisClusterAsyncCommands<?, ?> stateful)) {
                    return null;
                }
                @SuppressWarnings({"rawtypes", "unchecked"})
                RedisClusterAsyncCommands<String, String> commands = (RedisClusterAsyncCommands) stateful;
                CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
                args.add("LEN");
                Long value = await(commands.dispatch(CommandType.SLOWLOG, new IntegerOutput<>(StringCodec.UTF8), args));
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
