package com.laminar.ops.biz.controller;

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

    @Value("${laminar.monitor.token:laminar-gateway}")
    private String token;

    @PostMapping("/report")
    public Result<Integer> report(@RequestHeader(value = "X-Monitor-Token", required = false) String headerToken,
                                  @RequestBody Map<String, Object> body) {
        if (!token.equals(headerToken)) {
            throw BizException.forbidden("设备接入 Token 无效");
        }
        Object value = body.get("value");
        if (value == null) throw BizException.badRequest("缺少 value");
        int n = monitorService.report(String.valueOf(body.get("equipmentCode")),
                String.valueOf(body.get("pointCode")), Double.parseDouble(String.valueOf(value)));
        return Result.ok(n);
    }
}
