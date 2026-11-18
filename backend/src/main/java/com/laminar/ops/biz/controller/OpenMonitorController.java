package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.MonitorGatewayService;
import com.laminar.ops.biz.service.MonitorService;
import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                                  @RequestBody Map<String, Object> body,
                                  HttpServletRequest request) {
        checkToken(headerToken, request);
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
        return Result.ok(monitorService.report(equipmentCode, pointCode, numeric, timestamp(body.get("timestamp"))));
    }

    /** 网关批量上报；一次最多 500 个点位，返回逐点结果。 */
    @PostMapping("/report/batch")
    public Result<Map<String, Object>> batchReport(@RequestHeader(value = "X-Monitor-Token", required = false) String headerToken,
                                                   @RequestBody List<Map<String, Object>> points,
                                                   HttpServletRequest request) {
        checkToken(headerToken, request);
        if (points == null || points.isEmpty()) throw BizException.badRequest("points 不能为空");
        if (points.size() > 500) throw BizException.badRequest("单次最多上报 500 个点位");
        int success = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (Map<String, Object> body : points) {
            String equipmentCode = String.valueOf(body.getOrDefault("equipmentCode", "")).trim();
            String pointCode = String.valueOf(body.getOrDefault("pointCode", "")).trim();
            Object value = body.get("value");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("equipmentCode", equipmentCode);
            item.put("pointCode", pointCode);
            item.put("value", value);
            try {
                if (equipmentCode.isBlank() || pointCode.isBlank() || value == null) {
                    throw BizException.badRequest("缺少设备、点位或数值");
                }
                double numeric = Double.parseDouble(String.valueOf(value));
                int n = monitorService.report(equipmentCode, pointCode, numeric, timestamp(body.get("timestamp")));
                success += n;
                item.put("success", true);
                item.put("count", n);
            } catch (RuntimeException e) {
                item.put("success", false);
                item.put("message", e.getMessage() == null ? "上报失败" : e.getMessage());
            }
            details.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", success);
        result.put("success", success);
        result.put("failed", points.size() - success);
        result.put("items", details);
        return Result.ok(result);
    }

    @GetMapping("/modbus-templates")
    public Result<List<Map<String, Object>>> modbusTemplates(
            @RequestHeader(value = "X-Monitor-Token", required = false) String headerToken,
            HttpServletRequest request) {
        checkToken(headerToken, request);
        return Result.ok(gatewayService.templatesForToken(headerToken));
    }

    private LocalDateTime timestamp(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return LocalDateTime.now();
        String text = String.valueOf(value).trim();
        try {
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {
                throw BizException.badRequest("timestamp 格式应为 yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    private void checkToken(String headerToken, HttpServletRequest request) {
        if (gatewayService.authenticate(headerToken, clientIp(request))) return;
        if (token != null && !token.isBlank() && token.equals(headerToken)) return;
        throw BizException.forbidden("设备接入 Token 无效或 IP 不在白名单");
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
