package com.laminar.ops.system.controller;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.BackupJob;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupJob backupJob;

    @PostMapping("/run")
    @Perm("system:config:edit")
    public Result<String> run() {
        if (!AuthContext.require().isSuperAdmin()) {
            throw BizException.forbidden("仅超级管理员可执行备份");
        }
        return Result.ok(backupJob.runNow());
    }
}
