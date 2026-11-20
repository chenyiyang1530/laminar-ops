package com.laminar.ops.biz.control;

import java.util.Map;

/** 远程控制协议驱动。地址只能来自受管控制配置。 */
public interface ControlDriver {
    String protocol();

    int read(DeviceControlConfig config, int address) throws Exception;

    void write(DeviceControlConfig config, int address, int value) throws Exception;

    default Map<String, Object> diagnostics(DeviceControlConfig config) {
        return Map.of();
    }
}
