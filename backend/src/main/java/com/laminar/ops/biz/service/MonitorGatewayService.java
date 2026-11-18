package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 设备接入网关配置与 Token 管理。 */
@Service
@RequiredArgsConstructor
public class MonitorGatewayService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final MonitorService monitorService;

    public List<Map<String, Object>> list() {
        LoginUser user = requireOperator();
        String sql = "select gateway_id as `gatewayId`, gateway_code as `gatewayCode`, " +
                "gateway_name as `gatewayName`, protocol, address, ip_whitelist as `ipWhitelist`, token, enabled, " +
                "date_format(last_seen,'%Y-%m-%d %H:%i:%s') as `lastSeen`, last_seen_ip as `lastSeenIp`, remark, " +
                "date_format(create_time,'%Y-%m-%d %H:%i') as `createTime` from biz_monitor_gateway ";
        Long companyId = user.getCompanyId();
        if (companyId == null) {
            return jdbc.queryForList(sql + "order by gateway_id desc");
        }
        return jdbc.queryForList(sql + "where company_id=? order by gateway_id desc", companyId);
    }

    public List<Map<String, Object>> options() {
        LoginUser user = requireOperator();
        String sql = "select gateway_id as `value`, gateway_name as `label` from biz_monitor_gateway where enabled='1' ";
        Long companyId = user.getCompanyId();
        if (companyId == null) {
            return jdbc.queryForList(sql + "order by gateway_id");
        }
        return jdbc.queryForList(sql + "and company_id=? order by gateway_id", companyId);
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        LoginUser user = requireOperator();
        Long companyId = user.getCompanyId();
        Long gatewayId = longOf(body.get("gatewayId"));
        String code = str(body.get("gatewayCode")).trim().toUpperCase();
        String name = str(body.get("gatewayName")).trim();
        if (code.isBlank() || name.isBlank()) throw BizException.badRequest("请填写网关编码和名称");
        Integer duplicate = jdbc.queryForObject(
                "select count(*) from biz_monitor_gateway where gateway_code=? and gateway_id<>?",
                Integer.class, code, gatewayId == null ? -1L : gatewayId);
        if (duplicate != null && duplicate > 0) throw BizException.badRequest("网关编码已存在");

        if (gatewayId == null) {
            String token = newToken();
            jdbc.update("insert into biz_monitor_gateway(company_id,gateway_code,gateway_name,protocol,address,ip_whitelist,token,enabled,remark,create_time) " +
                            "values(?,?,?,?,?,?,?,'1',?,now())",
                    companyId, code, name, str(body.get("protocol")), str(body.get("address")),
                    str(body.get("ipWhitelist")), token, str(body.get("remark")));
            gatewayId = jdbc.queryForObject("select last_insert_id()", Long.class);
            logToken(gatewayId, companyId, "CREATE");
        } else {
            assertOwned(gatewayId, companyId);
            jdbc.update("update biz_monitor_gateway set gateway_code=?,gateway_name=?,protocol=?,address=?,ip_whitelist=?,remark=?,update_time=now() where gateway_id=?",
                    code, name, str(body.get("protocol")), str(body.get("address")), str(body.get("ipWhitelist")),
                    str(body.get("remark")), gatewayId);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gatewayId", gatewayId);
        result.put("token", jdbc.queryForObject("select token from biz_monitor_gateway where gateway_id=?", String.class, gatewayId));
        return result;
    }

    public void setEnabled(Long gatewayId, String enabled) {
        assertOwned(gatewayId, requireOperator().getCompanyId());
        jdbc.update("update biz_monitor_gateway set enabled=?,update_time=now() where gateway_id=?",
                "1".equals(enabled) ? "1" : "0", gatewayId);
    }

    public String rotate(Long gatewayId) {
        LoginUser user = requireOperator();
        assertOwned(gatewayId, user.getCompanyId());
        String token = newToken();
        jdbc.update("update biz_monitor_gateway set token=?,update_time=now() where gateway_id=?", token, gatewayId);
        logToken(gatewayId, user.getCompanyId(), "ROTATE");
        return token;
    }

    public List<Map<String, Object>> tokenLogs(Long gatewayId) {
        LoginUser user = requireOperator();
        assertOwned(gatewayId, user.getCompanyId());
        return jdbc.queryForList(
                "select token_log_id as `tokenLogId`, action, operator, token_suffix as `tokenSuffix`, " +
                        "date_format(create_time,'%Y-%m-%d %H:%i:%s') as `createTime` " +
                        "from biz_monitor_token_log where gateway_id=? order by token_log_id desc limit 20",
                gatewayId);
    }

    @Transactional
    public void delete(Long gatewayId) {
        assertOwned(gatewayId, requireOperator().getCompanyId());
        jdbc.update("update biz_equipment set gateway_id=null where gateway_id=?", gatewayId);
        jdbc.update("delete from biz_monitor_gateway where gateway_id=?", gatewayId);
    }

    public List<Map<String, Object>> testEquipmentOptions() {
        LoginUser user = requireOperator();
        String sql = "select e.equipment_code as `value`, concat(e.equipment_name, ' · ', e.equipment_code) as `label` " +
                "from biz_equipment e where e.status='0' and exists (" +
                "select 1 from biz_monitor_point p where p.equipment_id=e.equipment_id and p.enabled='1') ";
        if (user.getCompanyId() == null) {
            return jdbc.queryForList(sql + "order by e.equipment_name limit 500");
        }
        return jdbc.queryForList(sql + "and e.company_id=? order by e.equipment_name limit 500", user.getCompanyId());
    }

    public List<Map<String, Object>> testPointOptions(String equipmentCode) {
        LoginUser user = requireOperator();
        String sql = "select p.point_code as `value`, concat(p.point_name, ' · ', p.point_code) as `label`, p.unit, " +
                "p.warn_lower as `warnLower`, p.warn_upper as `warnUpper`, " +
                "p.alarm_lower as `alarmLower`, p.alarm_upper as `alarmUpper`, " +
                "p.metric, p.precision, " +
                "(select d.value from biz_monitor_data d where d.point_id=p.point_id order by d.collect_time desc limit 1) as `latestValue`, " +
                "(select date_format(d.collect_time,'%Y-%m-%d %H:%i:%s') from biz_monitor_data d where d.point_id=p.point_id order by d.collect_time desc limit 1) as `latestTime` " +
                "from biz_monitor_point p join biz_equipment e on e.equipment_id=p.equipment_id " +
                "where e.equipment_code=? and p.enabled='1' ";
        if (user.getCompanyId() == null) {
            return jdbc.queryForList(sql + "order by p.point_name", equipmentCode);
        }
        return jdbc.queryForList(sql + "and e.company_id=? order by p.point_name", equipmentCode, user.getCompanyId());
    }

    public List<Map<String, Object>> testLogs(String equipmentCode, String result, String startDate, String endDate) {
        LoginUser user = requireOperator();
        StringBuilder sql = new StringBuilder(
                "select test_id as `testId`, equipment_code as `equipmentCode`, equipment_name as `equipmentName`, " +
                        "point_code as `pointCode`, point_name as `pointName`, test_value as `value`, unit, " +
                        "result, message, alarm_triggered as `alarmTriggered`, operator, " +
                        "date_format(create_time,'%Y-%m-%d %H:%i:%s') as `createTime` from biz_monitor_test_log where 1=1 ");
        List<Object> args = new java.util.ArrayList<>();
        if (user.getCompanyId() != null) {
            sql.append("and company_id=? ");
            args.add(user.getCompanyId());
        }
        if (equipmentCode != null && !equipmentCode.isBlank()) {
            sql.append("and equipment_code=? ");
            args.add(equipmentCode);
        }
        if ("1".equals(result) || "0".equals(result)) {
            sql.append("and result=? ");
            args.add(result);
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append("and create_time >= ? ");
            args.add(startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append("and create_time < date_add(?, interval 1 day) ");
            args.add(endDate + " 00:00:00");
        }
        sql.append("order by test_id desc limit 10");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> preview(String equipmentCode, String pointCode, double value) {
        requireOperator();
        return monitorService.preview(equipmentCode, pointCode, value);
    }

    public Map<String, Object> test(String equipmentCode, String pointCode, double value) {
        LoginUser user = requireOperator();
        Long companyId = user.getCompanyId();
        Map<String, Object> point = pointInfo(equipmentCode, pointCode, companyId);
        String equipmentName = point == null ? "" : str(point.get("equipment_name"));
        String pointName = point == null ? "" : str(point.get("point_name"));
        String unit = point == null ? "" : str(point.get("unit"));
        try {
            int count = monitorService.report(equipmentCode, pointCode, value);
            boolean alarm = point != null && activeAlarm(companyId, equipmentCode, pointCode);
            String threshold = thresholdText(point);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("count", count);
            result.put("equipmentCode", equipmentCode);
            result.put("equipmentName", equipmentName);
            result.put("pointCode", pointCode);
            result.put("pointName", pointName);
            result.put("value", value);
            result.put("unit", unit);
            result.put("alarmTriggered", alarm);
            result.put("thresholdText", threshold);
            result.put("message", alarm ? "上报成功，已触发告警" : "上报成功，当前值未触发告警");
            insertLog(companyId, equipmentCode, equipmentName, pointCode, pointName, value, unit, "1",
                    String.valueOf(result.get("message")), alarm ? "1" : "0", user.getUsername());
            return result;
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "上报失败" : e.getMessage();
            insertLog(companyId, equipmentCode, equipmentName, pointCode, pointName, value, unit, "0",
                    message, "0", user.getUsername());
            throw e;
        }
    }

    /** 网关上报鉴权：Token + IP 白名单；成功时刷新 last_seen。 */
    public boolean authenticate(String token, String ip) {
        if (token == null || token.isBlank()) return false;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select gateway_id, ip_whitelist from biz_monitor_gateway where token=? and enabled='1'", token);
        if (rows.isEmpty()) return false;
        Map<String, Object> row = rows.get(0);
        if (!ipAllowed(str(row.get("ip_whitelist")), ip)) return false;
        jdbc.update("update biz_monitor_gateway set last_seen=now(), last_seen_ip=? where gateway_id=?",
                ip, row.get("gateway_id"));
        return true;
    }

    private boolean ipAllowed(String whitelist, String ip) {
        if (whitelist == null || whitelist.isBlank()) return true;
        if (ip == null || ip.isBlank()) return false;
        for (String item : whitelist.split("[,，\\s]+")) {
            String rule = item.trim();
            if (rule.isEmpty()) continue;
            if (rule.endsWith("*") ? ip.startsWith(rule.substring(0, rule.length() - 1)) : ip.equals(rule)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> pointInfo(String equipmentCode, String pointCode, Long companyId) {
        String sql = "select p.point_id, p.point_name, p.point_code, p.unit, p.warn_lower, p.warn_upper, " +
                "p.alarm_lower, p.alarm_upper, e.equipment_name, e.company_id " +
                "from biz_monitor_point p join biz_equipment e on e.equipment_id=p.equipment_id " +
                "where e.equipment_code=? and p.point_code=? ";
        List<Map<String, Object>> rows = companyId == null
                ? jdbc.queryForList(sql, equipmentCode, pointCode)
                : jdbc.queryForList(sql + "and e.company_id=?", equipmentCode, pointCode, companyId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean activeAlarm(Long companyId, String equipmentCode, String pointCode) {
        String sql = "select count(*) from biz_alarm a join biz_equipment e on e.equipment_id=a.equipment_id " +
                "join biz_monitor_point p on p.point_id=a.point_id " +
                "where e.equipment_code=? and p.point_code=? and a.status in ('0','1','2') " +
                "and a.first_time >= date_sub(now(), interval 30 second) ";
        Integer count = companyId == null
                ? jdbc.queryForObject(sql, Integer.class, equipmentCode, pointCode)
                : jdbc.queryForObject(sql + "and e.company_id=?", Integer.class, equipmentCode, pointCode, companyId);
        return count != null && count > 0;
    }

    private String thresholdText(Map<String, Object> point) {
        if (point == null) return "";
        String unit = str(point.get("unit"));
        return "告警范围 " + bound(point.get("alarm_lower"), point.get("alarm_upper"), unit)
                + "；预警范围 " + bound(point.get("warn_lower"), point.get("warn_upper"), unit);
    }

    private String bound(Object lower, Object upper, String unit) {
        if (lower == null && upper == null) return "未配置";
        if (lower != null && upper != null) return lower + " ~ " + upper + unit;
        if (lower != null) return "≥ " + lower + unit;
        return "≤ " + upper + unit;
    }

    private void insertLog(Long companyId, String equipmentCode, String equipmentName, String pointCode,
                           String pointName, double value, String unit, String result, String message,
                           String alarmTriggered, String operator) {
        jdbc.update("insert into biz_monitor_test_log(company_id,equipment_code,equipment_name,point_code,point_name," +
                        "test_value,unit,result,message,alarm_triggered,operator,create_time) values(?,?,?,?,?,?,?,?,?,?,?,now())",
                companyId, equipmentCode, equipmentName, pointCode, pointName, value, unit, result,
                message != null && message.length() > 500 ? message.substring(0, 500) : message,
                alarmTriggered, operator);
    }

    private void logToken(Long gatewayId, Long companyId, String action) {
        String token = jdbc.queryForObject("select token from biz_monitor_gateway where gateway_id=?", String.class, gatewayId);
        String suffix = token == null || token.length() <= 8 ? "" : token.substring(token.length() - 8);
        jdbc.update("insert into biz_monitor_token_log(gateway_id,company_id,action,operator,token_suffix,create_time) " +
                        "values(?,?,?,?,?,now())",
                gatewayId, companyId, action, AuthContext.require().getUsername(), suffix);
    }

    private void assertOwned(Long gatewayId, Long companyId) {
        if (companyId == null) return;
        Integer count = jdbc.queryForObject(
                "select count(*) from biz_monitor_gateway where gateway_id=? and company_id=?",
                Integer.class, gatewayId, companyId);
        if (count == null || count == 0) throw BizException.forbidden("无权操作其他公司网关");
    }

    private LoginUser requireOperator() {
        LoginUser user = AuthContext.require();
        if (user.isHospitalUser()) throw BizException.forbidden("院方账号无权管理设备接入");
        return user;
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "gw_" + HexFormat.of().formatHex(bytes);
    }

    private static Long longOf(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.valueOf(String.valueOf(value));
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
