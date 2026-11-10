package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.system.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicVersionController {

    private final VersionService versionService;

    @Value("${app.demo-login-enabled:false}")
    private boolean demoLoginEnabled;

    @GetMapping("/version")
    public Result<Map<String, Object>> version() {
        return Result.ok(versionService.info());
    }

    @GetMapping("/features")
    public Result<Map<String, Object>> features() {
        return Result.ok(Map.of("demoLoginEnabled", demoLoginEnabled));
    }
}
