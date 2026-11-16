package com.laminar.ops.system.controller;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.ConfigKeyRotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class ConfigKeyRotationController {

    private final ConfigKeyRotationService rotationService;

    @PostMapping("/rotate-key")
    @Perm("system:config:edit")
    public Result<Map<String, Object>> rotate(@RequestBody Map<String, String> body) {
        if (!AuthContext.require().isSuperAdmin()) {
            throw BizException.forbidden("仅超级管理员可轮换密钥");
        }
        int changed = rotationService.rotate(body.get("newSecret"));
        return Result.ok(Map.of("changed", changed, "message", "已完成，请使用新 CONFIG_SECRET 重启"));
    }
}
