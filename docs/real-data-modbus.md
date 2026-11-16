# Modbus 模拟采集到平台

这条链路用于在没有真实 PLC 的情况下验证：

```text
Modbus TCP 从站
→ Python 采集
→ 现有监控上报接口
→ 实时值/历史值/告警
```

## 1. 启动 Modbus 从站模拟器

仓库自带一个不依赖第三方库的模拟器：

```bash
cd D:\ideaWork\laminar-ops
py tools/modbus_slave_sim.py
```

默认：

```text
监听：0.0.0.0:1502
单元号：1
功能码：03 读保持寄存器
寄存器：0-4
```

寄存器含义：

| 地址 | 含义 | 缩放 |
|---|---|---|
| 0 | 送风温度 | ×0.1 |
| 1 | 回风湿度 | ×0.1 |
| 2 | 送风压差 | ×1 |
| 3 | 送风风速 | ×0.1 |
| 4 | 悬浮粒子 | ×1 |

> 常见 PLC 地址 40001 对应这里的地址 0。

## 2. 运行采集器

```bash
py tools/modbus_collector.py
```

只跑一轮：

```bash
py tools/modbus_collector.py --once
```

采集器会读取寄存器、换算工程值并调用：

```text
POST http://127.0.0.1:8080/api/open/monitor/report
Header: X-Monitor-Token: 环境变量 MONITOR_TOKEN
```

运行前设置网关 Token（不要使用代码默认值）：

```powershell
$env:MONITOR_TOKEN="你的网关Token"
```

请求体：

```json
{
  "equipmentCode": "AHU-01",
  "pointCode": "AHU-01-TEMP",
  "value": 23.0
}
```

## 3. 使用 Modbus Slave 软件代替自带模拟器

也可以用图形化软件：

```text
Connection: Modbus TCP/IP
Port: 502
Slave ID: 1
```

设置保持寄存器：

```text
40001 = 235   // 23.5℃
40002 = 520   // 52%RH
40003 = 12    // 12Pa
40004 = 25    // 2.5m/s
40005 = 3     // 3粒/L
```

把 `tools/modbus_collector.py` 的：

```python
MODBUS_PORT = 1502
```

改为：

```python
MODBUS_PORT = 502
```

然后运行采集器。

## 4. 当前点位编码

采集器使用的是现有示例点：

```text
AHU-01 / AHU-01-TEMP
AHU-01 / AHU-01-HUM
AHU-01 / AHU-01-DP
AHU-01 / AHU-01-WIND
AHU-01 / AHU-01-PART
```

如果点位不存在，接口会返回：

```text
设备或点位不存在
```

## 5. 验证结果

执行：

```bash
py tools/modbus_collector.py --once
```

应看到：

```text
AHU-01-TEMP: raw=230 -> 23.0℃ OK
AHU-01-HUM: raw=518 -> 51.8%RH OK
AHU-01-DP: raw=13 -> 13.0Pa OK
```

数据库验证：

```sql
select p.point_code, d.value, d.collect_time
from biz_monitor_data d
join biz_monitor_point p on p.point_id = d.point_id
join biz_equipment e on e.equipment_id = d.equipment_id
where e.equipment_code = 'AHU-01'
order by d.data_id desc
limit 10;
```

## 6. 生产环境不要用模拟器

生产环境应使用：

```text
pymodbus
python-snap7
Eclipse Milo
或其他工业协议库
```

并且增加：

```text
断线重连
断网缓存
批量上报
采集时间戳
网关身份认证
TLS
```

## 7. 常见问题

### 连接被拒绝

```text
模拟器没启动
端口不是 1502/502
防火墙拦截
```

### 读出来数值不对

检查：

```text
寄存器地址是否从 0 开始
是否用了 40001 偏移
INT16/UINT16/FLOAT32 类型
大小端
缩放系数
```

### 接口 403

检查：

```text
X-Monitor-Token 是否等于后端配置
laminar.monitor.token
```

### 接口 400：设备或点位不存在

检查设备编码和点位编码是否与 `biz_equipment`、`biz_monitor_point` 一致。
