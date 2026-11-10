package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.EngineerSelfService;
import com.laminar.ops.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/engineer/me")
@RequiredArgsConstructor
public class EngineerSelfController {

    private final EngineerSelfService engineerSelfService;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(engineerSelfService.status());
    }

    @PutMapping("/on-duty")
    public Result<Void> onDuty(@RequestBody Map<String, Object> body) {
        Object value = body.get("onDuty");
        engineerSelfService.setOnDuty(value != null && Boolean.parseBoolean(String.valueOf(value)));
        return Result.ok();
    }
}
