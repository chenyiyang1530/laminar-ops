"""
Modbus TCP 采集示例：读取模拟器，换算工程值，上报现有平台。

运行：
    python tools/modbus_collector.py

默认连接 127.0.0.1:1502，上报到 127.0.0.1:8080。
"""
import json
import os
import socket
import struct
import sys
import time
import urllib.error
import urllib.request

MODBUS_HOST = "127.0.0.1"
MODBUS_PORT = 1502
UNIT_ID = 1

API_BASE = "http://127.0.0.1:8080"
API_TOKEN = os.getenv("MONITOR_TOKEN", "")

# 地址按 0 开始；现场常见 40001 对应这里的 0。
POINTS = [
    {"equipmentCode": "AHU-01", "pointCode": "AHU-01-TEMP", "address": 0, "scale": 0.1, "unit": "℃"},
    {"equipmentCode": "AHU-01", "pointCode": "AHU-01-HUM", "address": 1, "scale": 0.1, "unit": "%RH"},
    {"equipmentCode": "AHU-01", "pointCode": "AHU-01-DP", "address": 2, "scale": 1.0, "unit": "Pa"},
    {"equipmentCode": "AHU-01", "pointCode": "AHU-01-WIND", "address": 3, "scale": 0.1, "unit": "m/s"},
    {"equipmentCode": "AHU-01", "pointCode": "AHU-01-PART", "address": 4, "scale": 1.0, "unit": "粒/L"},
]


def read_holding_registers(host, port, unit, start, count=1):
    transaction_id = int(time.time() * 1000) & 0xFFFF
    request = struct.pack(">HHHBBHH", transaction_id, 0, 6, unit, 3, start, count)
    with socket.create_connection((host, port), timeout=3) as sock:
        sock.sendall(request)
        response = sock.recv(256)
    if len(response) < 9:
        raise RuntimeError("Modbus 响应过短")
    tid, protocol, length, resp_unit = struct.unpack(">HHHB", response[:7])
    function = response[7]
    if tid != transaction_id or protocol != 0 or resp_unit != unit:
        raise RuntimeError("Modbus 响应头不匹配")
    if function & 0x80:
        raise RuntimeError("Modbus 异常码: " + str(response[8]))
    byte_count = response[8]
    values = []
    for i in range(byte_count // 2):
        values.append(struct.unpack(">H", response[9 + i * 2:11 + i * 2])[0])
    return values


def post_value(equipment_code, point_code, value):
    payload = json.dumps({
        "equipmentCode": equipment_code,
        "pointCode": point_code,
        "value": round(value, 2),
    }).encode("utf-8")
    request = urllib.request.Request(
        API_BASE + "/api/open/monitor/report",
        data=payload,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-Monitor-Token": API_TOKEN,
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            body = json.loads(response.read().decode("utf-8"))
            if body.get("code") != 200:
                print(f"[api] 上报失败 {point_code}: {body}")
                return False
            return True
    except urllib.error.URLError as e:
        print(f"[api] 网络异常 {point_code}: {e}")
        return False


def main():
    once = "--once" in sys.argv
    while True:
        print("\n[collector] 开始一轮采集")
        for point in POINTS:
            try:
                raw = read_holding_registers(
                    MODBUS_HOST, MODBUS_PORT, UNIT_ID, point["address"], 1)[0]
                value = raw * point["scale"]
                ok = post_value(point["equipmentCode"], point["pointCode"], value)
                print(f"[collector] {point['pointCode']}: raw={raw} -> {value}{point['unit']} {'OK' if ok else 'FAIL'}")
            except Exception as e:
                print(f"[collector] {point['pointCode']} 失败: {e}")
        if once:
            break
        time.sleep(5)


if __name__ == "__main__":
    main()
