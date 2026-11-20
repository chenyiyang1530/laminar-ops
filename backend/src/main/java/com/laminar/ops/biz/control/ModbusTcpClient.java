package com.laminar.ops.biz.control;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 最小 Modbus TCP 客户端：只实现远程控制需要的读保持寄存器、写单寄存器、读线圈、写单线圈。
 * 不暴露任意寄存器写入接口，地址必须来自受管控制点配置。
 */
public class ModbusTcpClient {

    private static final AtomicInteger TRANSACTION = new AtomicInteger(1);
    private final String host;
    private final int port;
    private final int unitId;
    private final int timeoutMs;

    public ModbusTcpClient(String host, int port, int unitId, int timeoutMs) {
        this.host = host;
        this.port = port;
        this.unitId = unitId;
        this.timeoutMs = Math.max(500, timeoutMs);
    }

    public int readHolding(int address) throws Exception {
        byte[] response = request(3, address, 1, false);
        if (response.length < 11 || (response[7] & 0xFF) != 3) throw new IllegalStateException("Modbus 读寄存器响应异常");
        return ((response[9] & 0xFF) << 8) | (response[10] & 0xFF);
    }

    public int readCoil(int address) throws Exception {
        byte[] response = request(1, address, 1, false);
        if (response.length < 10 || (response[7] & 0xFF) != 1) throw new IllegalStateException("Modbus 读线圈响应异常");
        return response[9] & 0x01;
    }

    public void writeRegister(int address, int value) throws Exception {
        byte[] response = request(6, address, value, true);
        if (response.length < 12 || (response[7] & 0xFF) != 6) throw new IllegalStateException("Modbus 写寄存器响应异常");
    }

    public void writeCoil(int address, boolean on) throws Exception {
        request(5, address, on ? 0xFF00 : 0x0000, true);
    }

    private byte[] request(int function, int address, int value, boolean write) throws Exception {
        int transaction = TRANSACTION.getAndIncrement() & 0xFFFF;
        ByteBuffer pdu = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
        pdu.putShort((short) transaction);
        pdu.putShort((short) 0);
        pdu.putShort((short) 6);
        pdu.put((byte) unitId);
        pdu.put((byte) function);
        pdu.putShort((short) address);
        pdu.putShort((short) value);
        byte[] request = pdu.array();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            OutputStream out = socket.getOutputStream();
            out.write(request);
            out.flush();
            InputStream in = socket.getInputStream();
            byte[] header = in.readNBytes(7);
            if (header.length < 7) throw new IllegalStateException("Modbus 响应头不完整");
            int length = ((header[4] & 0xFF) << 8) | (header[5] & 0xFF);
            byte[] rest = in.readNBytes(length - 1);
            byte[] response = new byte[7 + rest.length];
            System.arraycopy(header, 0, response, 0, 7);
            System.arraycopy(rest, 0, response, 7, rest.length);
            if ((response[7] & 0x80) != 0) {
                int code = response.length > 8 ? response[8] & 0xFF : 0;
                throw new IllegalStateException("Modbus 异常码 " + code);
            }
            return response;
        }
    }
}
