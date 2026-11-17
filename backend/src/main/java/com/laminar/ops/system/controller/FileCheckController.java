package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.FileCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/file-check")
@RequiredArgsConstructor
public class FileCheckController {

    private final FileCheckService fileCheckService;

    @PostMapping("/run")
    @Perm("system:filecheck:run")
    public Result<Map<String, Object>> run() {
        return Result.ok(fileCheckService.run("manual"));
    }

    @GetMapping("/runs")
    @Perm("system:filecheck:list")
    public Result<Map<String, Object>> runs(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(fileCheckService.pageRuns(pageNum, pageSize));
    }

    @GetMapping("/items")
    @Perm("system:filecheck:list")
    public Result<Map<String, Object>> items(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "20") int pageSize,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long companyId,
                                              @RequestParam(required = false) Long hospitalId,
                                              @RequestParam(required = false) String fileType,
                                              @RequestParam(required = false) String severity,
                                              @RequestParam(required = false) Long itemId) {
        return Result.ok(fileCheckService.pageItems(pageNum, pageSize, status, keyword,
                companyId, hospitalId, fileType, severity, itemId));
    }

    @GetMapping("/trend")
    @Perm("system:filecheck:list")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(fileCheckService.trend(days));
    }

    @PostMapping("/items/{id}/recover")
    @Perm("system:filecheck:edit")
    public Result<Map<String, Object>> recover(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(fileCheckService.recover(id, body == null ? null : body.get("fileUrl")));
    }
}
