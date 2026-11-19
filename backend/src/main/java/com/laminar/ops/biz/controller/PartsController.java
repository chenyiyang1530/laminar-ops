package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.PartsService;
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
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartsController {

    private final PartsService partsService;

    @GetMapping("/requisition/page")
    @Perm("resource:requisition:list")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String status) {
        return Result.ok(partsService.page(pageNum, pageSize, status));
    }

    @GetMapping("/requisition/{id}")
    @Perm("resource:requisition:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(partsService.detail(id));
    }

    @PostMapping("/requisition")
    @Perm("resource:requisition:add")
    public Result<Long> create(@RequestBody Map<String, Object> body) {
        return Result.ok(partsService.create(body));
    }

    @PostMapping("/requisition/{id}/approve")
    @Perm("resource:requisition:approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        partsService.approve(id, true, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/requisition/{id}/reject")
    @Perm("resource:requisition:approve")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        partsService.approve(id, false, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/requisition/{id}/out")
    @Perm("resource:requisition:out")
    public Result<Void> out(@PathVariable Long id) {
        partsService.outbound(id);
        return Result.ok();
    }

    @PostMapping("/stock")
    @Perm("resource:part:stock")
    public Result<Void> stock(@RequestBody Map<String, Object> body) {
        partsService.stock(((Number) body.get("partId")).longValue(), String.valueOf(body.get("type")),
                ((Number) body.get("quantity")).intValue(), (String) body.get("remark"));
        return Result.ok();
    }

    @GetMapping("/{partId}/transactions")
    @Perm("resource:part:list")
    public Result<java.util.List<Map<String, Object>>> transactions(@PathVariable Long partId,
                                                                   @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(partsService.transactions(partId, limit));
    }
}
