package com.laminar.ops.system.controller;

import com.laminar.ops.biz.task.DynamicJobScheduler;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.dto.RoleAssignLogQuery;
import com.laminar.ops.system.service.SystemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;
    private final DynamicJobScheduler jobScheduler;

    // ---------- 用户 ----------
    @GetMapping("/user/page")
    @Perm("system:user:list")
    public Result<Map<String, Object>> userPage(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam Map<String, String> params) {
        return Result.ok(systemService.userPage(pageNum, pageSize, params));
    }

    @GetMapping("/user/options")
    @Perm("customer:region:list")
    public Result<List<Map<String, Object>>> userOptions() {
        return Result.ok(systemService.userOptions());
    }

    @GetMapping("/user/roleOptions")
    @Perm("system:user:list")
    public Result<List<Map<String, Object>>> roleOptions() {
        return Result.ok(systemService.assignableRoles());
    }

    @GetMapping("/user/postOptions")
    @Perm("system:user:list")
    public Result<List<Map<String, Object>>> postOptions() {
        return Result.ok(systemService.postOptions());
    }

    @GetMapping("/user/engineerOptions")
    @Perm("system:user:list")
    public Result<List<Map<String, Object>>> engineerOptions(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long userId) {
        return Result.ok(systemService.engineerOptions(companyId, userId));
    }

    @GetMapping("/user/{id}")
    @Perm("system:user:list")
    public Result<Map<String, Object>> user(@PathVariable Long id) {
        return Result.ok(systemService.userDetail(id));
    }

    @PostMapping("/user")
    @Perm("system:user:add")
    public Result<Void> addUser(@RequestBody Map<String, Object> body) {
        systemService.saveUser(body);
        return Result.ok();
    }

    @PutMapping("/user")
    @Perm("system:user:edit")
    public Result<Void> editUser(@RequestBody Map<String, Object> body) {
        systemService.updateUser(body);
        return Result.ok();
    }

    @DeleteMapping("/user/{ids}")
    @Perm("system:user:remove")
    public Result<Void> deleteUser(@PathVariable Long[] ids) {
        systemService.deleteUser(ids);
        return Result.ok();
    }

    @PutMapping("/user/{id}/resetPwd")
    @Perm("system:user:edit")
    public Result<Void> resetPwd(@PathVariable Long id) {
        systemService.resetPwd(id);
        return Result.ok();
    }

    @PutMapping("/user/{id}/status")
    @Perm("system:user:edit")
    public Result<Void> userStatus(@PathVariable Long id, @RequestParam String status) {
        systemService.changeUserStatus(id, status);
        return Result.ok();
    }

    // ---------- 角色 ----------
    @GetMapping("/role/list")
    @Perm("system:role:list")
    public Result<List<Map<String, Object>>> roles() {
        return Result.ok(systemService.roleList());
    }

    @PostMapping("/role")
    @Perm("system:role:add")
    public Result<Void> addRole(@RequestBody Map<String, Object> body) {
        systemService.saveRole(body);
        return Result.ok();
    }

    @PutMapping("/role")
    @Perm("system:role:edit")
    public Result<Void> editRole(@RequestBody Map<String, Object> body) {
        systemService.updateRole(body);
        return Result.ok();
    }

    @DeleteMapping("/role/{id}")
    @Perm("system:role:remove")
    public Result<Void> deleteRole(@PathVariable Long id) {
        systemService.deleteRole(id);
        return Result.ok();
    }

    @GetMapping("/role/{id}/menus")
    @Perm("system:role:list")
    public Result<List<Long>> roleMenus(@PathVariable Long id) {
        return Result.ok(systemService.roleMenuIds(id));
    }

    @GetMapping("/role/{id}/assignmentLogs")
    @Perm("system:role:list")
    public Result<Map<String, Object>> roleAssignLogs(@PathVariable Long id,
                                                      @Valid RoleAssignLogQuery query) {
        return Result.ok(systemService.roleAssignLogs(id, query.getAction(), query.getPageNum(), query.getPageSize()));
    }

    @PutMapping("/role/{id}/menus")
    @Perm("system:role:edit")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        systemService.assignMenus(id, menuIds);
        return Result.ok();
    }

    // ---------- 菜单 / 部门 ----------
    @GetMapping("/menu/tree")
    @Perm("system:role:list")
    public Result<List<Map<String, Object>>> menuTree() {
        return Result.ok(systemService.menuTree());
    }

    @GetMapping("/dept/tree")
    @Perm("system:user:list")
    public Result<List<Map<String, Object>>> deptTree(@RequestParam(required = false) Long companyId) {
        return Result.ok(systemService.deptTree(companyId));
    }

    @PostMapping("/dept")
    @Perm("system:user:edit")
    public Result<Void> addDept(@RequestBody Map<String, Object> body) {
        systemService.saveDept(body);
        return Result.ok();
    }

    @PutMapping("/dept")
    @Perm("system:user:edit")
    public Result<Void> editDept(@RequestBody Map<String, Object> body) {
        systemService.updateDept(body);
        return Result.ok();
    }

    @DeleteMapping("/dept/{id}")
    @Perm("system:user:edit")
    public Result<Void> deleteDept(@PathVariable Long id) {
        systemService.deleteDept(id);
        return Result.ok();
    }

    // ---------- 日志 / 任务 ----------
    @GetMapping("/log/login")
    @Perm("system:loginlog:list")
    public Result<Map<String, Object>> loginLogs(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String username) {
        return Result.ok(systemService.loginLogPage(pageNum, pageSize, username));
    }

    @GetMapping("/log/oper")
    @Perm("system:operlog:list")
    public Result<Map<String, Object>> operLogs(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) String title) {
        return Result.ok(systemService.operLogPage(pageNum, pageSize, title));
    }

    @DeleteMapping("/log/{type}/{ids}")
    @Perm("system:operlog:remove")
    public Result<Void> deleteLogs(@PathVariable String type, @PathVariable Long[] ids) {
        systemService.deleteLogs(type, ids);
        return Result.ok();
    }

    @GetMapping("/job/list")
    @Perm("system:job:list")
    public Result<List<Map<String, Object>>> jobs() {
        return Result.ok(systemService.jobs());
    }

    @PutMapping("/job/{id}/status")
    @Perm("system:job:edit")
    public Result<Void> jobStatus(@PathVariable Long id, @RequestParam String status) {
        systemService.toggleJob(id, status);
        jobScheduler.reschedule(id);
        return Result.ok();
    }

    @PostMapping("/job/{id}/run")
    @Perm("system:job:edit")
    public Result<Void> runJob(@PathVariable Long id) {
        jobScheduler.runNow(id);
        return Result.ok();
    }
}
