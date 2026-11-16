package com.laminar.ops.system.controller;

import com.laminar.ops.common.UploadCleanupService;
import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system/upload")
@RequiredArgsConstructor
public class UploadCleanupController {

    private final UploadCleanupService cleanupService;

    @PostMapping("/cleanup")
    @Perm("system:config:edit")
    public Result<Map<String, Object>> cleanup(@RequestParam(defaultValue = "true") boolean dryRun) {
        if (!AuthContext.require().isSuperAdmin()) {
            throw BizException.forbidden("仅超级管理员可执行上传清理");
        }
        return Result.ok(cleanupService.cleanup(dryRun));
    }
}
