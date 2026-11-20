package com.laminar.ops.biz.control;

/** 受管的 PLC 控制配置快照。 */
public record DeviceControlConfig(
        Long configId,
        Long companyId,
        Long equipmentId,
        String brand,
        String protocol,
        boolean enabled,
        String host,
        int port,
        int unitId,
        String area,
        Integer startAddress,
        Integer stopAddress,
        Integer runStatusAddress,
        Integer localRemoteAddress,
        Integer interlockAddress,
        Integer faultAddress,
        int writeValue,
        int timeoutMs,
        String remark
) {
}
