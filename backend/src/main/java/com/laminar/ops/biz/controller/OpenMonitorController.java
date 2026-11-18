package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.MonitorGatewayService;
import com.laminar.ops.biz.service.MonitorService;
import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 设备网关数据上报通道（免登录，Token 校验）。
 * 网关按点位推送：equipmentCode + pointCode + value。
 */
@RestController
@RequestMapping("/api/open/monitor")
@RequiredArgsConstructor
public class OpenMonitorController {

    private final MonitorService monitorService;
    private final MonitorGatewayService gatewayService;

    @Value("${laminar.monitor.token:}")
    private String token;

    @PostMapping("/report")
    public Result<Integer> report(@RequestHeader(value = "X-Monitor-Token", required = false) String headerToken,
                                  @RequestBody Map<String, Object> body) {
        checkToken(headerToken);
        String equipmentCode = String.valueOf(body.getOrDefault("equipmentCode", "")).trim();
        String pointCode = String.valueOf(body.getOrDefault("pointCode", "")).trim();
        if (equipmentCode.isBlank() || pointCode.isBlank()) {
            throw BizException.badRequest("缺少 equipmentCode 或 pointCode");
        }
        Object value = body.get("value");
        if (value == null) throw BizException.badRequest("缺少 value");
        double numeric;
        try {
            numeric = Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw BizException.badRequest("value 必须是数字");
        }
        return Result.ok(monitorService.report(equipmentCode, pointCode, numeric));
    }

    /** 网关批量上报；一次最多 500 个点位，减少现场弱网下的请求次数。 */
    @PostMapping("/report/batch")
    public Result<Integer> batchReport(@RequestHeader(value = "X-Monitor-Token", required = false) String headerToken,
                                       @RequestBody List<Map<String, Object>> points) {
        checkToken(headerToken);
        if (points == null || points.isEmpty()) throw BizException.badRequest("points 不能为空");
        if (points.size() > 500) throw BizException.badRequest("单次最多上报 500 个点位");
        int count = 0;
        for (Map<String, Object> body : points) {
            String equipmentCode = String.valueOf(body.getOrDefault("equipmentCode", "")).trim();
            String pointCode = String.valueOf(body.getOrDefault("pointCode", "")).trim();
            Object value = body.get("value");
            if (equipmentCode.isBlank() || pointCode.isBlank() || value == null) continue;
            try {
                count += monitorService.report(equipmentCode, pointCode, Double.parseDouble(String.valueOf(value)));
            } catch (NumberFormatException ignored) {
                // 单点格式错误不影响同一批次其他点位。
            }
        }
        return Result.ok(count);
    }

    private void checkToken(String headerToken) {
        if (gatewayService.authenticate(headerToken)) return;
        if (token != null && !token.isBlank() && token.equals(headerToken)) return;
        throw BizException.forbidden("设备接入 Token 无效");
    }
}
