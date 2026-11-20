# 远程控制（PLC）

## 当前能力

- 设备控制配置：品牌、协议、地址、超时。
- 模拟器协议：无硬件即可验证启动/停止和命令审计。
- Modbus TCP：读保持寄存器/线圈，写单寄存器/单线圈。
- `S7_GATEWAY` 协议已预留，但尚未实现，需要边缘网关。
- 启动前检查就地/远程、联锁、故障码；启动后轮询运行反馈。
- 每条命令记录操作人、原因、确认文本、前后状态和结果。

## 入口

```text
监测中心 → 远程控制
```

接口：

```http
GET  /api/control/equipment/options
GET  /api/control/config/{equipmentId}
POST /api/control/config
GET  /api/control/status/{equipmentId}
POST /api/control/command/{equipmentId}
GET  /api/control/history/{equipmentId}
```

## 配置建议

### 模拟器

```text
品牌：任意
协议：SIMULATOR
启动地址：100
停止地址：101
运行反馈：102
就地/远程：103
联锁：104
故障码：105
```

### Modbus TCP

```text
协议：MODBUS_TCP
PLC IP：S7-200 SMART 或 LOGO! 的 IP
端口：502
区域：HOLDING 或 COIL
```

地址按 PLC 程序中的 V 区/寄存器映射填写，必须由 PLC 工程师确认。

### 经典 S7-200 / 早期 LOGO!

```text
S7_GATEWAY
```

需要外接边缘网关转换 S7/RS485 协议，当前未实现。

## 安全边界

- 平台只发起命令，不替代 PLC 的启动顺序和联锁。
- PLC 必须保留就地优先、消防、急停和故障停机。
- 运行反馈未确认时命令记为 TIMEOUT，不能显示成功。
- 生产使用前必须进行单机试点和现场安全验收。
- 当前命令权限沿用 `monitor:server:view`，正式上线建议拆分为独立 `device:control` 权限并接入第二人复核。

