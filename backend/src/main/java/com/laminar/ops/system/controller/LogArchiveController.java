package com.laminar.ops.system.controller;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.LogRetentionJob;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
public class LogArchiveController {

    private final LogRetentionJob logRetentionJob;

    @PostMapping("/archive/run")
    @Perm("system:config:edit")
    public Result<Map<String, Object>> run() {
        if (!AuthContext.require().isSuperAdmin()) {
            throw BizException.forbidden("仅超级管理员可执行日志归档");
        }
        return Result.ok(logRetentionJob.cleanup());
    }
}
