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
                "gateway_name as `gatewayName`, protocol, address, token, enabled, " +
                "date_format(last_seen,'%Y-%m-%d %H:%i:%s') as `lastSeen`, remark, " +
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
            jdbc.update("insert into biz_monitor_gateway(company_id,gateway_code,gateway_name,protocol,address,token,enabled,remark,create_time) " +
                            "values(?,?,?,?,?,?,'1',?,now())",
                    companyId, code, name, str(body.get("protocol")), str(body.get("address")), token,
                    str(body.get("remark")));
            gatewayId = jdbc.queryForObject("select last_insert_id()", Long.class);
        } else {
            assertOwned(gatewayId, companyId);
            jdbc.update("update biz_monitor_gateway set gateway_code=?,gateway_name=?,protocol=?,address=?,remark=?,update_time=now() where gateway_id=?",
                    code, name, str(body.get("protocol")), str(body.get("address")), str(body.get("remark")), gatewayId);
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
        assertOwned(gatewayId, requireOperator().getCompanyId());
        String token = newToken();
        jdbc.update("update biz_monitor_gateway set token=?,update_time=now() where gateway_id=?", token, gatewayId);
        return token;
    }

    @Transactional
    public void delete(Long gatewayId) {
        assertOwned(gatewayId, requireOperator().getCompanyId());
        jdbc.update("update biz_equipment set gateway_id=null where gateway_id=?", gatewayId);
        jdbc.update("delete from biz_monitor_gateway where gateway_id=?", gatewayId);
    }

    public Map<String, Object> test(String equipmentCode, String pointCode, double value) {
        requireOperator();
        int count = monitorService.report(equipmentCode, pointCode, value);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("equipmentCode", equipmentCode);
        result.put("pointCode", pointCode);
        result.put("value", value);
        return result;
    }

    /** 网关上报鉴权：返回 true 时同时刷新 last_seen。 */
    public boolean authenticate(String token) {
        if (token == null || token.isBlank()) return false;
        List<Long> ids = jdbc.queryForList(
                "select gateway_id from biz_monitor_gateway where token=? and enabled='1'", Long.class, token);
        if (ids.isEmpty()) return false;
        jdbc.update("update biz_monitor_gateway set last_seen=now() where gateway_id=?", ids.get(0));
        return true;
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
