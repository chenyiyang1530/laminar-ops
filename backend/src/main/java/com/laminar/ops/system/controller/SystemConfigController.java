package com.laminar.ops.system.controller;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.SysConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 平台参数设置。仅超级管理员可用，不承载医院/公司业务参数。
 */
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final JdbcTemplate jdbc;
    private final SysConfigService sysConfig;

    private static final Set<String> RESERVED_NOTIFY_KEYS = Set.of(
            "platform.notify.emailEnabled", "platform.notify.smsEnabled");

    private void superAdminOnly() {
        if (!AuthContext.require().isSuperAdmin()) {
            throw BizException.forbidden("只有系统管理员可以维护参数设置");
        }
    }

    @GetMapping("/page")
    @Perm("system:config:list")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String configName,
                                            @RequestParam(required = false) String configKey,
                                            @RequestParam(required = false) String groupCode) {
        superAdminOnly();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        List<Object> args = new ArrayList<>();
        if (configName != null && !configName.isBlank()) {
            where.append(" and config_name like ? ");
            args.add("%" + configName.trim() + "%");
        }
        if (configKey != null && !configKey.isBlank()) {
            where.append(" and config_key like ? ");
            args.add("%" + configKey.trim() + "%");
        }
        if (groupCode != null && !groupCode.isBlank()) {
            where.append(" and group_code = ? ");
            args.add(groupCode.trim());
        }
        Long total = jdbc.queryForObject("select count(*) from sys_config" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(Math.max(1, pageSize));
        pageArgs.add(Math.max(0, (Math.max(1, pageNum) - 1) * Math.max(1, pageSize)));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select config_id as `configId`, config_name as `configName`, config_key as `configKey`, " +
                        "group_code as `groupCode`, " +
                        "case when is_sensitive=1 then '******' else config_value end as `configValue`, value_type as `valueType`, " +
                        "min_value as `minValue`, max_value as `maxValue`, " +
                        "case when is_sensitive=1 then '******' else default_value end as `defaultValue`, " +
                        "options_json as `optionsJson`, is_sensitive as `isSensitive`, " +
                        "sort, status, config_type as `configType`, remark, " +
                        "date_format(create_time,'%Y-%m-%d %H:%i') as `createTime`, " +
                        "date_format(update_time,'%Y-%m-%d %H:%i') as `updateTime` " +
                        "from sys_config" + where + " order by group_code, sort, config_id limit ? offset ?",
                pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    @Perm("system:config:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        superAdminOnly();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select config_id as `configId`, config_name as `configName`, config_key as `configKey`, " +
                        "group_code as `groupCode`, config_value as `configValue`, value_type as `valueType`, " +
                        "min_value as `minValue`, max_value as `maxValue`, " +
                        "default_value as `defaultValue`, options_json as `optionsJson`, is_sensitive as `isSensitive`, " +
                        "sort, status, config_type as `configType`, remark from sys_config where config_id=?", id);
        if (rows.isEmpty()) throw BizException.badRequest("参数不存在");
        Map<String, Object> row = rows.get(0);
        if (intOf(row.get("isSensitive"), 0) == 1) {
            row.put("configValue", sysConfig.decrypt(str(row.get("configValue"))));
            row.put("defaultValue", sysConfig.decrypt(str(row.get("defaultValue"))));
        }
        return Result.ok(row);
    }

    @GetMapping("/logs")
    @Perm("system:config:list")
    public Result<List<Map<String, Object>>> logs(@RequestParam(required = false) Long configId,
                                                   @RequestParam(defaultValue = "50") int limit) {
        superAdminOnly();
        String where = configId == null ? "" : " where config_id=" + configId;
        return Result.ok(jdbc.queryForList(
                "select log_id as `logId`, config_id as `configId`, config_key as `configKey`, " +
                        "case when (select is_sensitive from sys_config c where c.config_id=sys_config_log.config_id)=1 " +
                        "  then '******' else old_value end as `oldValue`, " +
                        "case when (select is_sensitive from sys_config c where c.config_id=sys_config_log.config_id)=1 " +
                        "  then '******' else new_value end as `newValue`, operator_name as `operatorName`, " +
                        "ip, date_format(create_time,'%Y-%m-%d %H:%i:%s') as `createTime` " +
                        "from sys_config_log" + where + " order by log_id desc limit " + Math.max(1, Math.min(200, limit))));
    }

    @PostMapping
    @Perm("system:config:add")
    public Result<Long> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        superAdminOnly();
        String name = str(body.get("configName"));
        String key = str(body.get("configKey"));
        String value = str(body.get("configValue"));
        String type = str(body.getOrDefault("valueType", "STRING")).toUpperCase();
        if (name.isBlank()) throw BizException.badRequest("请填写参数名称");
        if (!key.matches("^[A-Za-z][A-Za-z0-9_.-]{2,99}$")) {
            throw BizException.badRequest("参数键名需以字母开头，3-100 位字母、数字、下划线、点或横线");
        }
        validateValue(type, value);
        validateReservedNotify(key, value);
        BigDecimal minValue = decimal(body.get("minValue"));
        BigDecimal maxValue = decimal(body.get("maxValue"));
        validateRange(type, value, minValue, maxValue);
        String defaultValue = str(body.getOrDefault("defaultValue", value));
        validateValue(type, defaultValue);
        validateRange(type, defaultValue, minValue, maxValue);
        boolean sensitive = intOf(body.get("isSensitive"), 0) == 1;
        String storedValue = sensitive ? sysConfig.encrypt(value) : value;
        String storedDefault = sensitive ? sysConfig.encrypt(defaultValue) : defaultValue;
        try {
            jdbc.update("insert into sys_config(config_name,config_key,group_code,config_value,value_type," +
                            "min_value,max_value,default_value,options_json,is_sensitive,sort,status,config_type,remark,create_time,update_time) " +
                            "values(?,?,?,?,?,?,?,?,?,?,?,?,'N',?,now(),now())",
                    name, key, str(body.getOrDefault("groupCode", "platform")), storedValue, type,
                    minValue, maxValue, storedDefault, str(body.get("optionsJson")),
                    sensitive ? 1 : 0, intOf(body.get("sort"), 0),
                    str(body.getOrDefault("status", "0")), str(body.get("remark")));
        } catch (DuplicateKeyException e) {
            throw BizException.badRequest("参数键名已存在");
        }
        Long id = jdbc.queryForObject("select config_id from sys_config where config_key=?", Long.class, key);
        writeLog(id, key, null, value, request);
        sysConfig.invalidate(key);
        return Result.ok(id);
    }

    @PutMapping("/{id}")
    @Perm("system:config:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        superAdminOnly();
        List<Map<String, Object>> oldRows = jdbc.queryForList(
                "select config_key, config_value, min_value, max_value, is_sensitive from sys_config where config_id=?", id);
        if (oldRows.isEmpty()) throw BizException.badRequest("参数不存在");
        String oldKey = str(oldRows.get(0).get("config_key"));
        String oldValue = sysConfig.decrypt(str(oldRows.get(0).get("config_value")));
        String name = str(body.get("configName"));
        String key = str(body.get("configKey"));
        String value = str(body.get("configValue"));
        String type = str(body.getOrDefault("valueType", "STRING")).toUpperCase();
        if (name.isBlank()) throw BizException.badRequest("请填写参数名称");
        if (!key.matches("^[A-Za-z][A-Za-z0-9_.-]{2,99}$")) {
            throw BizException.badRequest("参数键名格式不正确");
        }
        validateValue(type, value);
        validateReservedNotify(key, value);
        BigDecimal minValue = body.containsKey("minValue")
                ? decimal(body.get("minValue")) : decimal(oldRows.get(0).get("min_value"));
        BigDecimal maxValue = body.containsKey("maxValue")
                ? decimal(body.get("maxValue")) : decimal(oldRows.get(0).get("max_value"));
        validateRange(type, value, minValue, maxValue);
        String defaultValue = str(body.getOrDefault("defaultValue", value));
        validateValue(type, defaultValue);
        validateRange(type, defaultValue, minValue, maxValue);
        boolean sensitive = intOf(body.get("isSensitive"), 0) == 1;
        String storedValue = sensitive ? sysConfig.encrypt(value) : value;
        String storedDefault = sensitive ? sysConfig.encrypt(defaultValue) : defaultValue;
        try {
            int rows = jdbc.update("update sys_config set config_name=?,config_key=?,group_code=?,config_value=?," +
                            "value_type=?,min_value=?,max_value=?,default_value=?,options_json=?,is_sensitive=?,sort=?,status=?,remark=?,update_time=now() " +
                            "where config_id=?",
                    name, key, str(body.getOrDefault("groupCode", "platform")), storedValue, type,
                    minValue, maxValue, storedDefault, str(body.get("optionsJson")),
                    sensitive ? 1 : 0, intOf(body.get("sort"), 0),
                    str(body.getOrDefault("status", "0")), str(body.get("remark")), id);
            if (rows == 0) throw BizException.badRequest("参数不存在");
        } catch (DuplicateKeyException e) {
            throw BizException.badRequest("参数键名已存在");
        }
        if (!oldValue.equals(value) || !oldKey.equals(key)) {
            writeLog(id, key, oldValue, value, request);
        }
        sysConfig.invalidate(oldKey);
        sysConfig.invalidate(key);
        return Result.ok();
    }

    @PostMapping("/{id}/reset")
    @Perm("system:config:edit")
    public Result<Void> reset(@PathVariable Long id, HttpServletRequest request) {
        superAdminOnly();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select config_key, config_value, default_value, value_type, min_value, max_value, is_sensitive from sys_config where config_id=?", id);
        if (rows.isEmpty()) throw BizException.badRequest("参数不存在");
        String key = str(rows.get(0).get("config_key"));
        String oldValue = sysConfig.decrypt(str(rows.get(0).get("config_value")));
        String defaultValue = sysConfig.decrypt(str(rows.get(0).get("default_value")));
        String type = str(rows.get(0).get("value_type"));
        validateValue(type, defaultValue);
        validateRange(type, defaultValue, decimal(rows.get(0).get("min_value")), decimal(rows.get(0).get("max_value")));
        boolean sensitive = intOf(rows.get(0).get("is_sensitive"), 0) == 1;
        String storedDefault = sensitive ? sysConfig.encrypt(defaultValue) : defaultValue;
        jdbc.update("update sys_config set config_value=?, update_time=now() where config_id=?", storedDefault, id);
        writeLog(id, key, oldValue, defaultValue, request);
        sysConfig.invalidate(key);
        return Result.ok();
    }

    @DeleteMapping("/{ids}")
    @Perm("system:config:remove")
    public Result<Void> remove(@PathVariable String ids, HttpServletRequest request) {
        superAdminOnly();
        for (String part : ids.split(",")) {
            if (part.isBlank()) continue;
            Long id = Long.valueOf(part.trim());
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select config_key, config_value from sys_config where config_id=?", id);
            if (!rows.isEmpty()) writeLog(id, str(rows.get(0).get("config_key")),
                    sysConfig.decrypt(str(rows.get(0).get("config_value"))), null, request);
            jdbc.update("delete from sys_config where config_id=?", id);
        }
        sysConfig.invalidateAll();
        return Result.ok();
    }

    private void validateReservedNotify(String key, String value) {
        if (!RESERVED_NOTIFY_KEYS.contains(key)) return;
        boolean enabled = "true".equalsIgnoreCase(value) || "1".equals(value) || "on".equalsIgnoreCase(value);
        if (enabled) {
            throw BizException.badRequest("邮件/短信通知服务尚未接入，当前仅保留开关，不能开启");
        }
    }

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception e) {
            throw BizException.badRequest("范围值必须是数字");
        }
    }

    private void validateRange(String type, String value, BigDecimal min, BigDecimal max) {
        if (!"INTEGER".equals(type) && !"DECIMAL".equals(type)) return;
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw BizException.badRequest("最小值不能大于最大值");
        }
        BigDecimal number;
        try {
            number = new BigDecimal(value.trim());
        } catch (Exception e) {
            throw BizException.badRequest("参数值必须是数字");
        }
        if (min != null && number.compareTo(min) < 0) {
            throw BizException.badRequest("参数值不能小于 " + min.stripTrailingZeros().toPlainString());
        }
        if (max != null && number.compareTo(max) > 0) {
            throw BizException.badRequest("参数值不能大于 " + max.stripTrailingZeros().toPlainString());
        }
    }

    private void validateValue(String type, String value) {
        if (value.length() > 500) throw BizException.badRequest("参数值不能超过 500 字符");
        switch (type) {
            case "BOOLEAN" -> {
                if (!List.of("true", "false", "1", "0", "y", "n", "on", "off").contains(value.toLowerCase())) {
                    throw BizException.badRequest("布尔参数只允许 true/false");
                }
            }
            case "INTEGER" -> {
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw BizException.badRequest("整数参数格式不正确");
                }
            }
            case "DECIMAL" -> {
                try {
                    new java.math.BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw BizException.badRequest("小数参数格式不正确");
                }
            }
            case "SECRET", "STRING", "ENUM" -> {
                // 文本类型，长度已校验
            }
            default -> throw BizException.badRequest("不支持的参数类型：" + type);
        }
    }

    private void writeLog(Long configId, String key, String oldValue, String newValue, HttpServletRequest request) {
        LoginUser user = AuthContext.require();
        try {
            Integer sensitive = jdbc.queryForObject(
                    "select is_sensitive from sys_config where config_id=?", Integer.class, configId);
            if (sensitive != null && sensitive == 1) {
                oldValue = oldValue == null ? null : sysConfig.encrypt(oldValue);
                newValue = newValue == null ? null : sysConfig.encrypt(newValue);
            }
        } catch (Exception ignored) {
            // 删除后查不到参数时按非敏感记录
        }
        jdbc.update("insert into sys_config_log(config_id,config_key,old_value,new_value,operator_id,operator_name,ip,create_time) " +
                        "values(?,?,?,?,?,?,?,now())",
                configId, key, oldValue, newValue, user.getUserId(), user.getNickName(), clientIp(request));
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private int intOf(Object value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
