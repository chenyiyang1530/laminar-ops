package com.laminar.ops.system.controller;

import com.laminar.ops.common.FileExistenceCheckJob;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system/file-check")
@RequiredArgsConstructor
public class FileCheckController {

    private final FileExistenceCheckJob fileExistenceCheckJob;

    @PostMapping("/run")
    @Perm("system:config:edit")
    public Result<Map<String, Object>> run() {
        return Result.ok(fileExistenceCheckJob.check());
    }
}
