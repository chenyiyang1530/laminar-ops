package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.MaintenanceService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping("/task/page")
    @Perm("maintenance:task:list")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam Map<String, String> params) {
        return Result.ok(maintenanceService.page(pageNum, pageSize, params));
    }

    @GetMapping("/task/{id}")
    @Perm("maintenance:task:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(maintenanceService.detail(id));
    }

    @PostMapping("/task/{id}/start")
    @Perm("maintenance:task:execute")
    public Result<Void> start(@PathVariable Long id) {
        maintenanceService.start(id);
        return Result.ok();
    }

    @PostMapping("/task/{id}/complete")
    @Perm("maintenance:task:execute")
    public Result<Long> complete(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(maintenanceService.complete(id, body));
    }

    @PostMapping("/generate")
    @Perm("maintenance:plan:edit")
    public Result<Integer> generate() {
        return Result.ok(maintenanceService.generate());
    }
}
