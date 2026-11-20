package com.laminar.ops.biz.control;

/** Modbus TCP 驱动，适用于 S7-200 SMART、LOGO! 8.1+ 或带 Modbus TCP 的网关。 */
public class ModbusTcpDriver implements ControlDriver {

    @Override
    public String protocol() {
        return "MODBUS_TCP";
    }

    @Override
    public int read(DeviceControlConfig config, int address) throws Exception {
        ModbusTcpClient client = client(config);
        return "COIL".equalsIgnoreCase(config.area()) ? client.readCoil(address) : client.readHolding(address);
    }

    @Override
    public void write(DeviceControlConfig config, int address, int value) throws Exception {
        ModbusTcpClient client = client(config);
        if ("COIL".equalsIgnoreCase(config.area())) {
            client.writeCoil(address, value != 0);
        } else {
            client.writeRegister(address, value);
        }
    }

    private ModbusTcpClient client(DeviceControlConfig config) {
        if (config.host() == null || config.host().isBlank()) {
            throw new IllegalStateException("未配置 PLC IP 地址");
        }
        return new ModbusTcpClient(config.host(), config.port(), config.unitId(), config.timeoutMs());
    }
}
