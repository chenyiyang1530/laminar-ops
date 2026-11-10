package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.system.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/verify")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestBody Map<String, Object> body) {
        String type = body.get("type") == null ? null : String.valueOf(body.get("type"));
        String target = body.get("target") == null ? null : String.valueOf(body.get("target"));
        return Result.ok(verificationService.send(AuthContext.userId(), type, target));
    }

    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestBody Map<String, Object> body) {
        String type = body.get("type") == null ? null : String.valueOf(body.get("type"));
        String code = body.get("code") == null ? null : String.valueOf(body.get("code"));
        String target = body.get("target") == null ? null : String.valueOf(body.get("target"));
        verificationService.confirm(AuthContext.userId(), type, code, target);
        return Result.ok();
    }
}
