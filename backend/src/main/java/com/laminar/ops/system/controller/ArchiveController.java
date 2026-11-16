package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    @GetMapping("/alarms")
    @Perm("system:archive:edit")
    public Result<Map<String, Object>> alarms(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(archiveService.archivedAlarms(pageNum, pageSize, keyword));
    }

    @GetMapping("/workorders")
    @Perm("system:archive:edit")
    public Result<Map<String, Object>> workorders(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String keyword) {
        return Result.ok(archiveService.archivedWorkorders(pageNum, pageSize, keyword));
    }

    @PostMapping("/alarms/{id}/restore")
    @Perm("system:archive:edit")
    public Result<Void> restoreAlarm(@PathVariable Long id) {
        archiveService.restoreAlarm(id);
        return Result.ok();
    }

    @PostMapping("/workorders/{id}/restore")
    @Perm("system:archive:edit")
    public Result<Void> restoreWorkorder(@PathVariable Long id) {
        archiveService.restoreWorkorder(id);
        return Result.ok();
    }

    @PostMapping("/run-now")
    @Perm("system:archive:edit")
    public Result<Map<String, Object>> runNow(@RequestBody Map<String, Object> body) {
        boolean alarms = body.get("alarms") == null || Boolean.parseBoolean(String.valueOf(body.get("alarms")));
        boolean workorders = body.get("workorders") == null || Boolean.parseBoolean(String.valueOf(body.get("workorders")));
        return Result.ok(archiveService.runNow(alarms, workorders));
    }

    @PostMapping("/alarms/restore-batch")
    @Perm("system:archive:edit")
    public Result<Map<String, Object>> restoreAlarms(@RequestBody Map<String, Object> body) {
        return Result.ok(restoreBatch(body, true));
    }

    @PostMapping("/workorders/restore-batch")
    @Perm("system:archive:edit")
    public Result<Map<String, Object>> restoreWorkorders(@RequestBody Map<String, Object> body) {
        return Result.ok(restoreBatch(body, false));
    }

    private Map<String, Object> restoreBatch(Map<String, Object> body, boolean alarm) {
        List<Long> ids = new ArrayList<>();
        Object raw = body.get("ids");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                try {
                    ids.add(Long.valueOf(String.valueOf(item)));
                } catch (NumberFormatException ignored) {
                    // 忽略非法 ID
                }
            }
        }
        if (ids.isEmpty()) throw com.laminar.ops.common.BizException.badRequest("请选择要恢复的数据");
        if (ids.size() > 200) throw com.laminar.ops.common.BizException.badRequest("一次最多恢复 200 条");

        List<Long> failed = new ArrayList<>();
        for (Long id : ids) {
            try {
                if (alarm) archiveService.restoreAlarm(id);
                else archiveService.restoreWorkorder(id);
            } catch (Exception e) {
                failed.add(id);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", ids.size() - failed.size());
        result.put("failed", failed.size());
        result.put("failedIds", failed);
        return result;
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
