package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.ApprovalService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    @Perm("staff:leave:list")
    public Result<List<Map<String, Object>>> pending() {
        return Result.ok(approvalService.pending());
    }

    @GetMapping("/leave/{leaveId}/flow")
    @Perm("staff:leave:list")
    public Result<Map<String, Object>> flow(@PathVariable Long leaveId) {
        return Result.ok(approvalService.flow(leaveId));
    }

    @GetMapping("/history")
    @Perm("staff:leave:list")
    public Result<List<Map<String, Object>>> history() {
        return Result.ok(approvalService.history());
    }

    @GetMapping("/company-records")
    @Perm("staff:leave:list")
    public Result<List<Map<String, Object>>> companyRecords() {
        return Result.ok(approvalService.companyRecords());
    }

    @GetMapping("/rules")
    @Perm("staff:approval:config")
    public Result<List<Map<String, Object>>> rules(@RequestParam(required = false) Long regionId) {
        return Result.ok(approvalService.rules(regionId));
    }

    @GetMapping("/delegates")
    @Perm("staff:leave:list")
    public Result<List<Map<String, Object>>> delegates() {
        return Result.ok(approvalService.delegates());
    }

    @PostMapping("/delegates")
    @Perm("staff:leave:list")
    public Result<Void> saveDelegate(@RequestBody Map<String, Object> body) {
        approvalService.saveDelegate(body);
        return Result.ok();
    }

    @DeleteMapping("/delegates/{id}")
    @Perm("staff:leave:list")
    public Result<Void> deleteDelegate(@PathVariable Long id) {
        approvalService.deleteDelegate(id);
        return Result.ok();
    }

    @PutMapping("/rules")
    @Perm("staff:approval:config")
    public Result<Map<String, Object>> saveRules(@RequestParam(required = false) Long regionId,
                                                @RequestBody List<Map<String, Object>> rules) {
        return Result.ok(approvalService.saveRules(regionId, rules));
    }

    @GetMapping("/rules/history")
    @Perm("staff:approval:config")
    public Result<List<Map<String, Object>>> rulesHistory(@RequestParam(required = false) Long regionId) {
        return Result.ok(approvalService.rulesHistory(regionId));
    }

    @GetMapping("/rules/versions")
    @Perm("staff:approval:config")
    public Result<List<Map<String, Object>>> versions(@RequestParam(required = false) Long regionId) {
        return Result.ok(approvalService.versions(regionId));
    }

    @PostMapping("/rules/versions/{versionId}/publish")
    @Perm("staff:approval:config")
    public Result<Void> publishVersion(@PathVariable Long versionId) {
        approvalService.publishVersion(versionId);
        return Result.ok();
    }

    @PostMapping("/rules/versions/{versionId}/rollback")
    @Perm("staff:approval:config")
    public Result<Map<String, Object>> rollbackVersion(@PathVariable Long versionId) {
        return Result.ok(approvalService.rollbackVersion(versionId));
    }

    @PostMapping("/rules/rollback/{logId}")
    @Perm("staff:approval:config")
    public Result<Void> rollbackRule(@PathVariable Long logId) {
        approvalService.rollbackRule(logId);
        return Result.ok();
    }

    @GetMapping("/transferOptions")
    @Perm("staff:leave:list")
    public Result<List<Map<String, Object>>> transferOptions() {
        return Result.ok(approvalService.transferOptions());
    }

    @PostMapping("/{taskId}/transfer")
    @Perm("staff:leave:list")
    public Result<Void> transfer(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        Object target = body.get("targetUserId");
        approvalService.transfer(taskId, target == null ? null : Long.valueOf(String.valueOf(target)));
        return Result.ok();
    }

    @PostMapping("/{taskId}/urge")
    @Perm("staff:leave:list")
    public Result<Void> urge(@PathVariable Long taskId) {
        approvalService.urge(taskId);
        return Result.ok();
    }

    @PostMapping("/{taskId}/act")
    @Perm("staff:leave:list")
    public Result<Void> act(@PathVariable Long taskId, @RequestBody(required = false) Map<String, Object> body) {
        boolean approved = body != null && Boolean.parseBoolean(String.valueOf(body.get("approved")));
        String comment = body == null || body.get("comment") == null ? "" : String.valueOf(body.get("comment"));
        approvalService.act(taskId, approved, comment);
        return Result.ok();
    }
}
