ALTER TABLE biz_monitor_gateway
  ADD COLUMN ip_whitelist VARCHAR(1000) NULL COMMENT '允许上报的IP，逗号分隔，支持末尾*' AFTER address,
  ADD COLUMN last_seen_ip VARCHAR(64) NULL COMMENT '最后上报IP' AFTER last_seen;
