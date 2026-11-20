package com.laminar.ops.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laminar.ops.biz.control.ControlDriver;
import com.laminar.ops.biz.control.DeviceControlConfig;
import com.laminar.ops.biz.control.ModbusTcpDriver;
import com.laminar.ops.biz.control.SimulatorDriver;
import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceControlService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Map<String, ControlDriver> drivers = Map.of(
            "SIMULATOR", new SimulatorDriver(),
            "MODBUS_TCP", new ModbusTcpDriver()
    );

    public List<Map<String, Object>> equipmentOptions() {
        LoginUser user = AuthContext.require();
        String sql = "select e.equipment_id as `value`, concat(e.equipment_name, ' · ', e.equipment_code) as `label`, " +
                "e.equipment_code as `equipmentCode`, c.config_id as `configId`, c.protocol, c.enabled " +
                "from biz_equipment e left join biz_device_control_config c on c.equipment_id=e.equipment_id where e.status='0' ";
        Long companyId = user.getCompanyId();
        if (companyId != null) return jdbc.queryForList(sql + "and e.company_id=? order by e.equipment_name", companyId);
        return jdbc.queryForList(sql + "order by e.equipment_name");
    }

    public Map<String, Object> config(Long equipmentId) {
        ensureCompany(requireEquipment(equipmentId));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select * from biz_device_control_config where equipment_id=?", equipmentId);
        if (rows.isEmpty()) return Map.of("configured", false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", true);
        result.put("config", toConfig(rows.get(0)));
        return result;
    }

    @Transactional
    public Map<String, Object> saveConfig(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Long equipmentId = longOf(body.get("equipmentId"));
        Map<String, Object> equipment = requireEquipment(equipmentId);
        ensureCompany(equipment);
        String brand = str(body.get("brand")).trim();
        String protocol = str(body.get("protocol")).trim().toUpperCase();
        if (brand.isBlank()) throw BizException.badRequest("请选择 PLC 品牌");
        if (!drivers.containsKey(protocol) && !"S7_GATEWAY".equals(protocol)) {
            throw BizException.badRequest("不支持的协议：" + protocol);
        }
        if ("MODBUS_TCP".equals(protocol) && str(body.get("host")).isBlank()) {
            throw BizException.badRequest("Modbus TCP 必须填写 PLC IP");
        }
        Integer start = intOf(body.get("startAddress"));
        Integer stop = intOf(body.get("stopAddress"));
        Integer run = intOf(body.get("runStatusAddress"));
        if (start == null || stop == null || run == null) {
            throw BizException.badRequest("启动、停止、运行状态地址不能为空");
        }
        String area = "COIL".equalsIgnoreCase(str(body.get("area"))) ? "COIL" : "HOLDING";
        int port = intOf(body.get("port")) == null ? 502 : intOf(body.get("port"));
        int unitId = intOf(body.get("unitId")) == null ? 1 : intOf(body.get("unitId"));
        int timeout = intOf(body.get("timeoutMs")) == null ? 3000 : Math.max(500, Math.min(intOf(body.get("timeoutMs")), 10000));
        List<Map<String, Object>> exists = jdbc.queryForList(
                "select config_id from biz_device_control_config where equipment_id=?", equipmentId);
        if (exists.isEmpty()) {
            jdbc.update("insert into biz_device_control_config(company_id,equipment_id,brand,protocol,enabled,host,port,unit_id,area," +
                            "start_address,stop_address,run_status_address,local_remote_address,interlock_address,fault_address," +
                            "write_value,timeout_ms,remark,create_by,create_time) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now())",
                    equipment.get("company_id"), equipmentId, brand, protocol, bool(body.get("enabled")) ? "1" : "0",
                    str(body.get("host")), port, unitId, area, start, stop, run,
                    intOf(body.get("localRemoteAddress")), intOf(body.get("interlockAddress")), intOf(body.get("faultAddress")),
                    intOf(body.get("writeValue")) == null ? 1 : intOf(body.get("writeValue")), timeout,
                    str(body.get("remark")), user.getNickName());
        } else {
            jdbc.update("update biz_device_control_config set brand=?,protocol=?,enabled=?,host=?,port=?,unit_id=?,area=?," +
                            "start_address=?,stop_address=?,run_status_address=?,local_remote_address=?,interlock_address=?," +
                            "fault_address=?,write_value=?,timeout_ms=?,remark=?,update_by=?,update_time=now() where equipment_id=?",
                    brand, protocol, bool(body.get("enabled")) ? "1" : "0", str(body.get("host")), port, unitId, area,
                    start, stop, run, intOf(body.get("localRemoteAddress")), intOf(body.get("interlockAddress")),
                    intOf(body.get("faultAddress")), intOf(body.get("writeValue")) == null ? 1 : intOf(body.get("writeValue")),
                    timeout, str(body.get("remark")), user.getNickName(), equipmentId);
        }
        return config(equipmentId);
    }

    public Map<String, Object> status(Long equipmentId) {
        ensureCompany(requireEquipment(equipmentId));
        Map<String, Object> result = new LinkedHashMap<>();
        DeviceControlConfig cfg = findConfig(equipmentId);
        if (cfg == null) {
            result.put("configured", false);
            return result;
        }
        result.put("configured", true);
        result.put("config", configMap(cfg));
        try {
            ControlDriver driver = driver(cfg.protocol());
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("runStatus", read(cfg, driver, cfg.runStatusAddress()));
            values.put("localRemote", read(cfg, driver, cfg.localRemoteAddress()));
            values.put("interlockOk", read(cfg, driver, cfg.interlockAddress()));
            values.put("faultCode", read(cfg, driver, cfg.faultAddress()));
            result.put("values", values);
            result.put("online", true);
        } catch (Exception e) {
            result.put("online", false);
            result.put("error", e.getMessage());
        }
        result.put("history", history(equipmentId, 20));
        return result;
    }

    public Map<String, Object> execute(Long equipmentId, String action, String requestId, String reason,
                                       String confirmText) {
        LoginUser user = AuthContext.require();
        String act = str(action).toUpperCase();
        if (!"START".equals(act) && !"STOP".equals(act)) throw BizException.badRequest("动作只能是 START 或 STOP");
        if (str(reason).isBlank()) throw BizException.badRequest("请填写操作原因");
        if (str(requestId).isBlank()) throw BizException.badRequest("缺少 requestId");
        DeviceControlConfig cfg = findConfig(equipmentId);
        if (cfg == null) throw BizException.badRequest("设备尚未配置远程控制");
        if (!cfg.enabled()) throw BizException.badRequest("远程控制未启用");
        Map<String, Object> equipment = requireEquipment(equipmentId);
        ensureCompany(equipment);
        if (!str(equipment.get("equipment_code")).equalsIgnoreCase(str(confirmText).trim())) {
            throw BizException.badRequest("确认文本必须与设备编码一致");
        }
        List<Map<String, Object>> existing = jdbc.queryForList(
                "select * from biz_control_command where request_id=?", requestId);
        if (!existing.isEmpty()) return toCommand(existing.get(0));
        String no = "CMD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        try {
            jdbc.update("insert into biz_control_command(command_no,company_id,equipment_id,equipment_name,action,status," +
                            "request_id,reason,confirm_text,operator_id,operator_name,create_time) " +
                            "values(?,?,?,?,?,? ,?,?,?,?,?,now())",
                    no, equipment.get("company_id"), equipmentId, equipment.get("equipment_name"), act, "PENDING",
                    requestId, reason, confirmText, user.getUserId(), user.getNickName());
        } catch (DuplicateKeyException e) {
            return toCommand(jdbc.queryForMap("select * from biz_control_command where request_id=?", requestId));
        }
        Long commandId = jdbc.queryForObject("select command_id from biz_control_command where request_id=?", Long.class, requestId);
        try {
            ControlDriver driver = driver(cfg.protocol());
            Map<String, Object> before = readStatus(cfg, driver);
            jdbc.update("update biz_control_command set before_status=?,after_status=? where command_id=?",
                    json(before), json(before), commandId);
            if ("START".equals(act)) {
                Integer local = read(cfg, driver, cfg.localRemoteAddress());
                Integer interlock = read(cfg, driver, cfg.interlockAddress());
                Integer fault = read(cfg, driver, cfg.faultAddress());
                if (local != null && local != 1) throw BizException.badRequest("PLC 处于就地模式，禁止远程启动");
                if (interlock != null && interlock != 1) throw BizException.badRequest("PLC 联锁条件不满足，禁止启动");
                if (fault != null && fault != 0) throw BizException.badRequest("PLC 存在故障码 " + fault + "，禁止启动");
                driver.write(cfg, cfg.startAddress(), cfg.writeValue());
            } else {
                driver.write(cfg, cfg.stopAddress(), cfg.writeValue());
            }
            int expected = "START".equals(act) ? 1 : 0;
            long deadline = System.currentTimeMillis() + cfg.timeoutMs();
            Integer after = null;
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(250);
                after = read(cfg, driver, cfg.runStatusAddress());
                if (after != null && after == expected) break;
            }
            Map<String, Object> status = readStatus(cfg, driver);
            if (after != null && after == expected) {
                jdbc.update("update biz_control_command set status='SUCCESS',after_status=?,detail_json=?,finish_time=now() where command_id=?",
                        json(status), json(status), commandId);
            } else {
                jdbc.update("update biz_control_command set status='TIMEOUT',after_status=?,detail_json=?,error_message=?,finish_time=now() where command_id=?",
                        json(status), json(status), "命令已下发，但在超时时间内未确认运行状态", commandId);
            }
        } catch (Exception e) {
            log.warn("远程控制失败 equipment={} action={}: {}", equipmentId, act, e.getMessage());
            jdbc.update("update biz_control_command set status='FAILED',error_message=?,finish_time=now() where command_id=?",
                    e.getMessage(), commandId);
        }
        return toCommand(jdbc.queryForMap("select * from biz_control_command where command_id=?", commandId));
    }

    public List<Map<String, Object>> history(Long equipmentId, int limit) {
        ensureCompany(requireEquipment(equipmentId));
        return jdbc.queryForList(
                "select command_id as `commandId`, command_no as `commandNo`, action, status, reason, operator_name as `operatorName`, " +
                        "before_status as `beforeStatus`, after_status as `afterStatus`, error_message as `errorMessage`, " +
                        "date_format(create_time,'%Y-%m-%d %H:%i:%s') as `createTime` " +
                        "from biz_control_command where equipment_id=? order by command_id desc limit ?", equipmentId, limit);
    }

    private Map<String, Object> readStatus(DeviceControlConfig cfg, ControlDriver driver) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("runStatus", read(cfg, driver, cfg.runStatusAddress()));
        map.put("localRemote", read(cfg, driver, cfg.localRemoteAddress()));
        map.put("interlockOk", read(cfg, driver, cfg.interlockAddress()));
        map.put("faultCode", read(cfg, driver, cfg.faultAddress()));
        return map;
    }

    private Integer read(DeviceControlConfig cfg, ControlDriver driver, Integer address) throws Exception {
        if (address == null) return null;
        return driver.read(cfg, address);
    }

    private ControlDriver driver(String protocol) {
        ControlDriver driver = drivers.get(protocol);
        if (driver == null) throw BizException.badRequest("协议 " + protocol + " 尚未接入，请配置边缘网关驱动");
        return driver;
    }

    private Map<String, Object> requireEquipment(Long equipmentId) {
        if (equipmentId == null) throw BizException.badRequest("请选择设备");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select equipment_id, equipment_code, equipment_name, company_id from biz_equipment where equipment_id=?",
                equipmentId);
        if (rows.isEmpty()) throw BizException.badRequest("设备不存在");
        return rows.get(0);
    }

    private void ensureCompany(Map<String, Object> equipment) {
        Long companyId = AuthContext.require().getCompanyId();
        if (companyId == null) return;
        Object equipmentCompany = equipment.get("company_id");
        if (equipmentCompany == null || !companyId.equals(((Number) equipmentCompany).longValue())) {
            throw BizException.forbidden("无权操作其他公司的设备");
        }
    }

    private DeviceControlConfig findConfig(Long equipmentId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select * from biz_device_control_config where equipment_id=?", equipmentId);
        return rows.isEmpty() ? null : toConfig(rows.get(0));
    }

    private DeviceControlConfig toConfig(Map<String, Object> row) {
        return new DeviceControlConfig(
                ((Number) row.get("config_id")).longValue(),
                row.get("company_id") == null ? null : ((Number) row.get("company_id")).longValue(),
                ((Number) row.get("equipment_id")).longValue(),
                str(row.get("brand")), str(row.get("protocol")), "1".equals(String.valueOf(row.get("enabled"))),
                str(row.get("host")), ((Number) row.get("port")).intValue(), ((Number) row.get("unit_id")).intValue(),
                str(row.get("area")), intOf(row.get("start_address")), intOf(row.get("stop_address")),
                intOf(row.get("run_status_address")), intOf(row.get("local_remote_address")),
                intOf(row.get("interlock_address")), intOf(row.get("fault_address")),
                ((Number) row.get("write_value")).intValue(), ((Number) row.get("timeout_ms")).intValue(),
                str(row.get("remark")));
    }

    private Map<String, Object> configMap(DeviceControlConfig cfg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("brand", cfg.brand());
        map.put("protocol", cfg.protocol());
        map.put("enabled", cfg.enabled());
        map.put("host", cfg.host());
        map.put("port", cfg.port());
        map.put("unitId", cfg.unitId());
        map.put("area", cfg.area());
        map.put("startAddress", cfg.startAddress());
        map.put("stopAddress", cfg.stopAddress());
        map.put("runStatusAddress", cfg.runStatusAddress());
        map.put("localRemoteAddress", cfg.localRemoteAddress());
        map.put("interlockAddress", cfg.interlockAddress());
        map.put("faultAddress", cfg.faultAddress());
        map.put("writeValue", cfg.writeValue());
        map.put("timeoutMs", cfg.timeoutMs());
        map.put("remark", cfg.remark());
        return map;
    }

    private Map<String, Object> toCommand(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("commandId", row.get("command_id"));
        map.put("commandNo", row.get("command_no"));
        map.put("action", row.get("action"));
        map.put("status", row.get("status"));
        map.put("reason", row.get("reason"));
        map.put("operatorName", row.get("operator_name"));
        map.put("beforeStatus", row.get("before_status"));
        map.put("afterStatus", row.get("after_status"));
        map.put("errorMessage", row.get("error_message"));
        return map;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static boolean bool(Object value) {
        return value != null && (Boolean.TRUE.equals(value) || "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value)));
    }

    private static Integer intOf(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Integer.valueOf(String.valueOf(value));
    }

    private static Long longOf(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.valueOf(String.valueOf(value));
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
