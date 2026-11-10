package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.LeaveService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/apply")
    @Perm("staff:leave:list")
    public Result<Long> apply(@RequestBody Map<String, Object> body) {
        return Result.ok(leaveService.apply(body));
    }

    @GetMapping("/my")
    @Perm("staff:leave:list")
    public Result<List<Map<String, Object>>> my() {
        return Result.ok(leaveService.my());
    }

    @GetMapping("/pending")
    @Perm("staff:leave:list")
    public Result<List<Map<String, Object>>> pending() {
        return Result.ok(leaveService.pending());
    }

    @GetMapping("/page")
    @Perm("staff:leave:list")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam Map<String, String> params) {
        return Result.ok(leaveService.page(pageNum, pageSize, params));
    }

    @PostMapping("/{id}/approve")
    @Perm("staff:leave:list")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        leaveService.approve(id, body == null ? "" : String.valueOf(body.getOrDefault("comment", "")), true);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    @Perm("staff:leave:list")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        leaveService.approve(id, body == null ? "" : String.valueOf(body.getOrDefault("comment", "")), false);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    @Perm("staff:leave:list")
    public Result<Void> cancel(@PathVariable Long id) {
        leaveService.cancel(id);
        return Result.ok();
    }
}
