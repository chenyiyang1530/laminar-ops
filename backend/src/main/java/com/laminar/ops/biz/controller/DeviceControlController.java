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
    @Perm("monitor:server:view")
    public Result<List<Map<String, Object>>> equipmentOptions() {
        return Result.ok(controlService.equipmentOptions());
    }

    @GetMapping("/config/{equipmentId}")
    @Perm("monitor:server:view")
    public Result<Map<String, Object>> config(@PathVariable Long equipmentId) {
        return Result.ok(controlService.config(equipmentId));
    }

    @PostMapping("/config")
    @Perm("monitor:server:view")
    public Result<Map<String, Object>> saveConfig(@RequestBody Map<String, Object> body) {
        return Result.ok(controlService.saveConfig(body));
    }

    @GetMapping("/status/{equipmentId}")
    @Perm("monitor:server:view")
    public Result<Map<String, Object>> status(@PathVariable Long equipmentId) {
        return Result.ok(controlService.status(equipmentId));
    }

    @PostMapping("/command/{equipmentId}")
    @Perm("monitor:server:view")
    public Result<Map<String, Object>> command(@PathVariable Long equipmentId,
                                               @RequestBody Map<String, Object> body) {
        return Result.ok(controlService.execute(equipmentId,
                (String) body.get("action"),
                (String) body.get("requestId"),
                (String) body.get("reason"),
                (String) body.get("confirmText")));
    }

    @GetMapping("/history/{equipmentId}")
    @Perm("monitor:server:view")
    public Result<List<Map<String, Object>>> history(@PathVariable Long equipmentId,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(controlService.history(equipmentId, Math.min(Math.max(limit, 1), 100)));
    }
}
