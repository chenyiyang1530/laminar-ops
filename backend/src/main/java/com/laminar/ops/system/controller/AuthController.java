package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginBody body, HttpServletRequest request) {
        return Result.ok(authService.login(body.getCompanyCode(), body.getUsername(), body.getPassword(),
                body.getUuid(), body.getCode(), request));
    }

    @GetMapping("/captcha")
    public Result<Map<String, Object>> captcha() {
        return Result.ok(authService.captcha());
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.ok(authService.info());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(AuthContext.userId());
        return Result.ok();
    }

    @GetMapping("/session/status")
    public Result<Void> sessionStatus() {
        return Result.ok();
    }

    @PutMapping("/preferences")
    public Result<Void> updatePreferences(@RequestBody Map<String, Object> body) {
        authService.updatePreferences(AuthContext.userId(), body);
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordBody body) {
        authService.changePassword(AuthContext.userId(), body.getOldPassword(), body.getNewPassword());
        return Result.ok();
    }

    @Data
    public static class LoginBody {
        private String companyCode;
        @NotBlank(message = "请输入账号")
        private String username;
        @NotBlank(message = "请输入密码")
        private String password;
        private String uuid;
        private String code;
    }

    @Data
    public static class ChangePasswordBody {
        private String oldPassword;
        private String newPassword;
    }
}
