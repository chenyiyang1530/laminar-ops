package com.laminar.ops.biz.control;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 无硬件联调驱动：启动/停止只改变内存状态，用于先验证平台流程。 */
public class SimulatorDriver implements ControlDriver {

    private final ConcurrentHashMap<Long, AtomicInteger> running = new ConcurrentHashMap<>();

    @Override
    public String protocol() {
        return "SIMULATOR";
    }

    @Override
    public int read(DeviceControlConfig config, int address) {
        int value = running.computeIfAbsent(config.equipmentId(), id -> new AtomicInteger(0)).get();
        if (config.runStatusAddress() != null && address == config.runStatusAddress()) return value;
        if (config.localRemoteAddress() != null && address == config.localRemoteAddress()) return 1;
        if (config.interlockAddress() != null && address == config.interlockAddress()) return 1;
        if (config.faultAddress() != null && address == config.faultAddress()) return 0;
        return 0;
    }

    @Override
    public void write(DeviceControlConfig config, int address, int value) {
        if (config.startAddress() != null && address == config.startAddress()) {
            running.computeIfAbsent(config.equipmentId(), id -> new AtomicInteger(0)).set(1);
        } else if (config.stopAddress() != null && address == config.stopAddress()) {
            running.computeIfAbsent(config.equipmentId(), id -> new AtomicInteger(0)).set(0);
        }
    }
}
