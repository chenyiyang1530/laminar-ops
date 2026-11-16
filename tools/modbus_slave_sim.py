"""
无需第三方库的 Modbus TCP 从站模拟器（学习用）。
默认监听 127.0.0.1:1502，单元号 1，保持寄存器地址 0-4。

运行：
    python tools/modbus_slave_sim.py
"""
import socket
import socketserver
import struct
import threading
import time

HOST = "0.0.0.0"
PORT = 1502
UNIT_ID = 1

# 模拟工程值：温度 23.5℃、湿度 52%、压差 12Pa、风速 2.5m/s、粒子 3粒/L
# 注意：Modbus 地址 0 对应常见 PLC 的 40001。
registers = [235, 520, 12, 25, 3]
lock = threading.Lock()


def refresh():
    t = 0
    while True:
        with lock:
            # 让数值缓慢波动，便于观察实时刷新
            registers[0] = 230 + (t % 8)
            registers[1] = 510 + (t % 20)
            registers[2] = 10 + (t % 5)
            registers[3] = 23 + (t % 4)
            registers[4] = 2 + (t % 3)
        t += 1
        time.sleep(2)


class ModbusHandler(socketserver.BaseRequestHandler):
    def handle(self):
        client = self.client_address
        print(f"[slave] 客户端连接: {client}")
        try:
            while True:
                header = self.request.recv(12)
                if len(header) < 12:
                    return
                transaction_id, protocol_id, length, unit_id = struct.unpack(">HHHB", header[:7])
                function = header[7]
                start, quantity = struct.unpack(">HH", header[8:12])
                if protocol_id != 0 or unit_id != UNIT_ID:
                    return
                if function != 3:
                    # 只实现学习用的读保持寄存器 FC03
                    self.send_error(transaction_id, unit_id, function, 1)
                    continue
                with lock:
                    data = registers[start:start + quantity] if start < len(registers) else []
                payload = b"".join(struct.pack(">H", v & 0xFFFF) for v in data)
                body = struct.pack(">BB", function, len(payload)) + payload
                response = struct.pack(">HHHBB", transaction_id, 0, len(body) + 1, unit_id, function) + body[1:]
                self.request.sendall(response)
                print(f"[slave] 读 FC03 地址 {start} 数量 {quantity} -> {data}")
        except (ConnectionResetError, BrokenPipeError):
            pass
        finally:
            print(f"[slave] 客户端断开: {client}")

    def send_error(self, transaction_id, unit_id, function, code):
        body = struct.pack(">BB", function | 0x80, code)
        self.request.sendall(struct.pack(">HHH", transaction_id, 0, len(body) + 1) + bytes([unit_id]) + body)


class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


if __name__ == "__main__":
    threading.Thread(target=refresh, daemon=True).start()
    with Server((HOST, PORT), ModbusHandler) as server:
        print(f"[slave] Modbus TCP 模拟器已启动 {HOST}:{PORT} unit={UNIT_ID}")
        print("[slave] 寄存器: 0=温度x10, 1=湿度x10, 2=压差, 3=风速x10, 4=粒子")
        server.serve_forever()
