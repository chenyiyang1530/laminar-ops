package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.DeviceControlService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/control")
@RequiredArgsConstructor
public class DeviceControlController {

    private final DeviceControlService controlService;

    @GetMapping("/equipment/options")
    @Perm("device:control:view")
    public Result<List<Map<String, Object>>> equipmentOptions() {
        return Result.ok(controlService.equipmentOptions());
    }

    @GetMapping("/equipment/context")
    @Perm("device:control:view")
    public Result<Map<String, Object>> equipmentContext() {
        return Result.ok(controlService.equipmentContext());
    }

    @GetMapping("/config/{equipmentId}")
    @Perm("device:control:view")
    public Result<Map<String, Object>> config(@PathVariable Long equipmentId) {
        return Result.ok(controlService.config(equipmentId));
    }

    @PostMapping("/config")
    @Perm("device:control:view")
    public Result<Map<String, Object>> saveConfig(@RequestBody Map<String, Object> body) {
        return Result.ok(controlService.saveConfig(body));
    }

    @GetMapping("/status/{equipmentId}")
    @Perm("device:control:view")
    public Result<Map<String, Object>> status(@PathVariable Long equipmentId) {
        return Result.ok(controlService.status(equipmentId));
    }

    @PostMapping("/command/{equipmentId}")
    @Perm("device:control")
    public Result<Map<String, Object>> command(@PathVariable Long equipmentId,
                                               @RequestBody Map<String, Object> body) {
        return Result.ok(controlService.execute(equipmentId,
                (String) body.get("action"),
                (String) body.get("requestId"),
                (String) body.get("reason"),
                (String) body.get("confirmText")));
    }

    @PostMapping("/command/{commandId}/review")
    @Perm("device:control:approve")
    public Result<Map<String, Object>> reviewCommand(@PathVariable Long commandId,
                                                     @RequestBody Map<String, Object> body) {
        boolean pass = body.get("pass") == null || Boolean.parseBoolean(String.valueOf(body.get("pass")));
        return Result.ok(controlService.reviewCommand(commandId, pass,
                (String) body.get("remark"), (String) body.get("confirmText")));
    }

    @GetMapping("/test/{equipmentId}")
    @Perm("device:control:view")
    public Result<Map<String, Object>> test(@PathVariable Long equipmentId) {
        return Result.ok(controlService.testConnection(equipmentId));
    }

    @GetMapping("/scope/users")
    @Perm("device:control:scope")
    public Result<List<Map<String, Object>>> scopeUsers() {
        return Result.ok(controlService.scopeUsers());
    }

    @GetMapping("/scope/{userId}")
    @Perm("device:control:scope")
    public Result<Map<String, Object>> scope(@PathVariable Long userId) {
        return Result.ok(controlService.scope(userId));
    }

    @PostMapping("/scope/{userId}")
    @Perm("device:control:scope")
    public Result<Void> saveScope(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        controlService.saveScope(userId, body);
        return Result.ok();
    }

    @PostMapping("/scope/{userId}/import")
    @Perm("device:control:scope")
    public Result<Integer> importScope(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        return Result.ok(controlService.importScope(userId, body));
    }

    @GetMapping("/history/{equipmentId}")
    @Perm("device:control:view")
    public Result<List<Map<String, Object>>> history(@PathVariable Long equipmentId,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(controlService.history(equipmentId, Math.min(Math.max(limit, 1), 100)));
    }

    @GetMapping("/audit/monthly")
    @Perm("device:control:view")
    public Result<Map<String, Object>> monthlyAudit(@RequestParam String month) {
        return Result.ok(controlService.monthlyAudit(month));
    }
}

