package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @GetMapping("/stats")
    @Perm("system:archive:edit")
    public Result<Map<String, Object>> stats(@RequestParam(defaultValue = "6") Integer months) {
        return Result.ok(archiveService.stats(months));
    }

    @PostMapping("/run")
    @Perm("system:archive:edit")
    public Result<Map<String, Object>> run(@RequestBody Map<String, Object> body) {
        Integer months = body.get("months") == null ? 6 : Integer.valueOf(String.valueOf(body.get("months")));
        boolean alarms = body.get("alarms") == null || Boolean.parseBoolean(String.valueOf(body.get("alarms")));
        boolean workorders = body.get("workorders") == null || Boolean.parseBoolean(String.valueOf(body.get("workorders")));
        return Result.ok(archiveService.run(months, alarms, workorders));
    }
}
