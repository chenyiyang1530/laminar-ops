# Redis8 Prometheus 告警

## 指标来源

应用启动后暴露：

```text
GET /actuator/prometheus
```

Redis8 相关指标：

```text
laminar_redis_health        1=UP，0=DOWN
laminar_redis_slowlog_count SLOWLOG 当前条数
```

`laminar_redis_health` 来自应用级 `redisLaminar` 健康检查，包含 PING、延迟、内存和 SLOWLOG 读取。

## Prometheus 抓取配置

```yaml
scrape_configs:
  - job_name: laminar-ops
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['127.0.0.1:8080']
```

如果生产环境对 actuator 加了认证，在 `scrape_configs` 中增加 `basic_auth` 或 `authorization`，不要关闭认证。

## 告警规则

将 `deploy/prometheus/redis8-alerts.yml` 加入 Prometheus：

```yaml
rule_files:
  - /etc/prometheus/rules/redis8-alerts.yml
```

规则包含：

```text
Redis8ApplicationHealthDown   健康指标为 0，持续 2 分钟
Redis8SlowLogDetected         SLOWLOG 大于 0，持续 5 分钟
Redis8HealthMetricMissing     指标缺失，持续 5 分钟
```

## 告警正文

慢查询平台告警会自动附带最近一条 SLOWLOG 摘要：

```text
id=...，时间=...，耗时=...ms，命令=...
```

命令参数最多保留前 5 个，最多 120 个字符，避免把敏感参数完整写入告警。
