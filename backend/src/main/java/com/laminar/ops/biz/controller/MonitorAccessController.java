package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.MonitorGatewayService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor/access")
@RequiredArgsConstructor
public class MonitorAccessController {

    private final MonitorGatewayService gatewayService;

    @GetMapping("/gateways")
    @Perm("device:equipment:list")
    public Result<List<Map<String, Object>>> gateways() {
        return Result.ok(gatewayService.list());
    }

    @GetMapping("/gatewayOptions")
    @Perm("device:equipment:list")
    public Result<List<Map<String, Object>>> gatewayOptions() {
        return Result.ok(gatewayService.options());
    }

    @PostMapping("/gateways")
    @Perm("device:equipment:edit")
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        return Result.ok(gatewayService.save(body));
    }

    @PutMapping("/gateways/{id}/enabled")
    @Perm("device:equipment:edit")
    public Result<Void> enabled(@PathVariable Long id, @RequestParam String enabled) {
        gatewayService.setEnabled(id, enabled);
        return Result.ok();
    }

    @PostMapping("/gateways/{id}/rotate")
    @Perm("device:equipment:edit")
    public Result<Map<String, Object>> rotate(@PathVariable Long id) {
        return Result.ok(Map.of("token", gatewayService.rotate(id)));
    }

    @GetMapping("/gateways/{id}/tokenLogs")
    @Perm("device:equipment:list")
    public Result<List<Map<String, Object>>> tokenLogs(@PathVariable Long id) {
        return Result.ok(gatewayService.tokenLogs(id));
    }

    @DeleteMapping("/gateways/{id}")
    @Perm("device:equipment:edit")
    public Result<Void> delete(@PathVariable Long id) {
        gatewayService.delete(id);
        return Result.ok();
    }

    @GetMapping("/test/equipment")
    @Perm("device:equipment:list")
    public Result<List<Map<String, Object>>> testEquipment() {
        return Result.ok(gatewayService.testEquipmentOptions());
    }

    @GetMapping("/test/points")
    @Perm("device:equipment:list")
    public Result<List<Map<String, Object>>> testPoints(@RequestParam String equipmentCode) {
        return Result.ok(gatewayService.testPointOptions(equipmentCode));
    }

    @GetMapping("/test/logs")
    @Perm("device:equipment:list")
    public Result<List<Map<String, Object>>> testLogs(
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(gatewayService.testLogs(equipmentCode, result, startDate, endDate));
    }

    @PostMapping("/test/preview")
    @Perm("device:equipment:list")
    public Result<Map<String, Object>> preview(@RequestBody Map<String, Object> body) {
        String equipmentCode = String.valueOf(body.getOrDefault("equipmentCode", "")).trim();
        String pointCode = String.valueOf(body.getOrDefault("pointCode", "")).trim();
        double value;
        try {
            value = Double.parseDouble(String.valueOf(body.get("value")));
        } catch (NumberFormatException e) {
            throw new com.laminar.ops.common.BizException(400, "测试值必须是数字");
        }
        return Result.ok(gatewayService.preview(equipmentCode, pointCode, value));
    }

    @PostMapping("/test")
    @Perm("device:equipment:edit")
    public Result<Map<String, Object>> test(@RequestBody Map<String, Object> body) {
        String equipmentCode = String.valueOf(body.getOrDefault("equipmentCode", "")).trim();
        String pointCode = String.valueOf(body.getOrDefault("pointCode", "")).trim();
        if (equipmentCode.isBlank() || pointCode.isBlank()) {
            throw new com.laminar.ops.common.BizException(400, "请填写设备编码和点位编码");
        }
        double value;
        try {
            value = Double.parseDouble(String.valueOf(body.get("value")));
        } catch (NumberFormatException e) {
            throw new com.laminar.ops.common.BizException(400, "测试值必须是数字");
        }
        return Result.ok(gatewayService.test(equipmentCode, pointCode, value));
    }

    @GetMapping("/info")
    @Perm("device:equipment:list")
    public Result<Map<String, Object>> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("singleUrl", "/api/open/monitor/report");
        result.put("batchUrl", "/api/open/monitor/report/batch");
        result.put("tokenHeader", "X-Monitor-Token");
        result.put("maxBatch", 500);
        return Result.ok(result);
    }
}
