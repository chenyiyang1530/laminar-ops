package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.AlarmService;
import com.laminar.ops.biz.service.DashboardService;
import com.laminar.ops.biz.service.MessageService;
import com.laminar.ops.biz.service.MonitorService;
import com.laminar.ops.biz.service.WorkorderService;
import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 医院前台端接口：仅开放本院数据查看、报修、验收确认、消息。
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

    private final DashboardService dashboardService;
    private final MonitorService monitorService;
    private final AlarmService alarmService;
    private final WorkorderService workorderService;
    private final MessageService messageService;
    private final SysConfigService sysConfig;
    private final JdbcTemplate jdbc;

    private Long hid() {
        LoginUser user = AuthContext.require();
        if (!user.isHospitalUser() || user.getHospitalId() == null) {
            throw BizException.forbidden("非院方账号");
        }
        return user.getHospitalId();
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        LoginUser user = AuthContext.require();
        Long departmentId = isDepartmentScoped(user) ? user.getDepartmentId() : null;
        return Result.ok(dashboardService.overview(List.of(hid()), null, departmentId));
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        LoginUser user = AuthContext.require();
        Long hospitalId = hid();
        Map<String, Object> hospital = jdbc.queryForMap(
                "select hospital_name as `hospitalName`, hospital_code as `hospitalCode`, address, phone from biz_hospital where hospital_id=?",
                hospitalId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("nickName", user.getNickName());
        result.put("username", user.getUsername());
        result.put("departmentId", user.getDepartmentId());
        result.put("pageSize", Math.max(1, sysConfig.intValue("platform.defaultPageSize", 10)));
        if (user.getDepartmentId() != null) {
            List<String> names = jdbc.queryForList(
                    "select department_name from biz_department where department_id=?", String.class, user.getDepartmentId());
            result.put("departmentName", names.isEmpty() ? null : names.get(0));
        }
        result.putAll(hospital);
        return Result.ok(result);
    }

    @GetMapping("/monitor")
    public Result<Map<String, Object>> monitor() {
        LoginUser user = AuthContext.require();
        Long departmentId = isDepartmentScoped(user) ? user.getDepartmentId() : null;
        return Result.ok(monitorService.overview(List.of(hid()), null, departmentId));
    }

    private boolean isDepartmentScoped(LoginUser user) {
        return user.isHospitalUser() && user.getDepartmentId() != null
                && (user.getRoleKeys().contains("hospital_sub_admin")
                || user.getRoleKeys().contains("hospital_dept"));
    }

    @GetMapping("/equipment-options")
    public Result<List<Map<String, Object>>> equipmentOptions() {
        LoginUser user = AuthContext.require();
        Long departmentId = isDepartmentScoped(user) ? user.getDepartmentId() : null;
        String sql = "select equipment_id as `value`, equipment_name as `label` from biz_equipment " +
                "where hospital_id=? and status='0'";
        if (departmentId != null) sql += " and department_id=?";
        sql += " order by equipment_id";
        return Result.ok(departmentId == null
                ? jdbc.queryForList(sql, hid())
                : jdbc.queryForList(sql, hid(), departmentId));
    }

    @GetMapping("/campus-options")
    public Result<List<Map<String, Object>>> campusOptions() {
        return Result.ok(jdbc.queryForList(
                "select campus_id as `value`, campus_name as `label` from biz_campus " +
                        "where hospital_id=? and status='0' order by campus_id", hid()));
    }

    @GetMapping("/department-options")
    public Result<List<Map<String, Object>>> departmentOptions(
            @RequestParam(required = false) Long campusId) {
        LoginUser user = AuthContext.require();
        Long departmentId = isDepartmentScoped(user) ? user.getDepartmentId() : null;
        StringBuilder sql = new StringBuilder(
                "select department_id as `value`, department_name as `label` from biz_department " +
                        "where hospital_id=? and status='0'");
        List<Object> args = new java.util.ArrayList<>();
        args.add(hid());
        if (campusId != null) {
            sql.append(" and campus_id=?");
            args.add(campusId);
        }
        if (departmentId != null) {
            sql.append(" and department_id=?");
            args.add(departmentId);
        }
        sql.append(" order by department_id");
        return Result.ok(jdbc.queryForList(sql.toString(), args.toArray()));
    }

    @GetMapping("/monitor/equipment/{id}")
    public Result<Map<String, Object>> equipment(@PathVariable Long id) {
        List<Long> hospitals = jdbc.queryForList("select hospital_id from biz_equipment where equipment_id=?", Long.class, id);
        if (hospitals.isEmpty() || !hid().equals(hospitals.get(0))) {
            throw BizException.forbidden("无权查看其他医院设备");
        }
        return Result.ok(monitorService.equipmentDetail(id));
    }

    @GetMapping("/alarms")
    public Result<Map<String, Object>> alarms(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam Map<String, String> params) {
        return Result.ok(alarmService.page(pageNum, pageSize, params));
    }

    @GetMapping("/alarms/{id}")
    public Result<Map<String, Object>> alarm(@PathVariable Long id) {
        Map<String, Object> detail = alarmService.detail(id);
        Object h = ((Map<?, ?>) detail.get("alarm")).get("hospital_id");
        if (h == null || !hid().equals(((Number) h).longValue())) {
            throw BizException.forbidden("无权查看其他医院告警");
        }
        return Result.ok(detail);
    }

    @GetMapping("/workorders")
    public Result<Map<String, Object>> workorders(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam Map<String, String> params) {
        return Result.ok(workorderService.page(pageNum, pageSize, params, false));
    }

    @GetMapping("/workorders/{id}")
    public Result<Map<String, Object>> workorder(@PathVariable Long id) {
        return Result.ok(workorderService.detail(id));
    }

    @PostMapping("/workorders")
    public Result<Long> report(@RequestBody Map<String, Object> body) {
        body.put("hospitalId", hid());
        body.put("source", "hospital");
        return Result.ok(workorderService.create(body));
    }

    @PostMapping("/workorders/{id}/accept")
    public Result<Void> accept(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        workorderService.accept(id, String.valueOf(body.getOrDefault("result", "1")), (String) body.get("remark"));
        return Result.ok();
    }

    @GetMapping("/messages")
    public Result<Map<String, Object>> messages(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) String isRead) {
        return Result.ok(messageService.page(pageNum, pageSize, null, isRead));
    }

    @GetMapping("/messages/unread-count")
    public Result<Integer> unread() {
        return Result.ok(messageService.unreadCount());
    }

    @PutMapping("/messages/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        messageService.read(id);
        return Result.ok();
    }

    @PutMapping("/messages/read-all")
    public Result<Void> readAll() {
        messageService.readAll();
        return Result.ok();
    }
}
