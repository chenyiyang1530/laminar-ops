package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.WorkorderService;
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
@RequestMapping("/api/workorder")
@RequiredArgsConstructor
public class WorkorderController {

    private final WorkorderService workorderService;

    @GetMapping("/page")
    @Perm("workorder:list")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam Map<String, String> params) {
        return Result.ok(workorderService.page(pageNum, pageSize, params, false));
    }

    @GetMapping("/my")
    @Perm("workorder:my")
    public Result<Map<String, Object>> my(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam Map<String, String> params) {
        return Result.ok(workorderService.page(pageNum, pageSize, params, true));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        com.laminar.ops.security.LoginUser user = com.laminar.ops.security.AuthContext.require();
        if (!user.hasPerm("workorder:list") && !user.hasPerm("workorder:my")) {
            throw com.laminar.ops.common.BizException.forbidden("无权限访问工单");
        }
        return Result.ok(workorderService.detail(id));
    }

    @PostMapping
    @Perm("workorder:add")
    public Result<Long> create(@RequestBody Map<String, Object> body) {
        return Result.ok(workorderService.create(body));
    }

    @PostMapping("/{id}/receive")
    @Perm("workorder:receive")
    public Result<Void> receive(@PathVariable Long id) {
        workorderService.receive(id);
        return Result.ok();
    }

    @PostMapping("/{id}/dispatch")
    @Perm("workorder:dispatch")
    public Result<Void> dispatch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object engineerId = body.get("engineerId");
        workorderService.dispatch(id, engineerId == null ? null : Long.valueOf(String.valueOf(engineerId)),
                (String) body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/reassign")
    @Perm("workorder:dispatch")
    public Result<Void> reassign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object engineerId = body.get("engineerId");
        workorderService.reassign(id, engineerId == null ? null : Long.valueOf(String.valueOf(engineerId)),
                (String) body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/arrive")
    @Perm("workorder:arrive")
    public Result<Void> arrive(@PathVariable Long id) {
        workorderService.arrive(id);
        return Result.ok();
    }

    @PostMapping("/{id}/finish")
    @Perm("workorder:finish")
    public Result<Void> finish(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object downtime = body.get("downtimeMinutes");
        workorderService.finish(id, (String) body.get("report"),
                downtime == null ? null : Integer.valueOf(String.valueOf(downtime)));
        return Result.ok();
    }

    @PostMapping("/{id}/accept")
    @Perm("workorder:edit")
    public Result<Void> accept(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        workorderService.accept(id, String.valueOf(body.getOrDefault("result", "1")), (String) body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/quote")
    @Perm("workorder:quote")
    public Result<Void> quote(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        workorderService.quote(id, body.get("amount") == null ? null : new java.math.BigDecimal(String.valueOf(body.get("amount"))),
                (String) body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/callback")
    @Perm("workorder:callback")
    public Result<Void> callback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object score = body.get("score");
        workorderService.callback(id, score == null ? 5 : Integer.valueOf(String.valueOf(score)),
                (String) body.get("content"));
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    @Perm("workorder:cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        workorderService.cancel(id, (String) body.get("reason"));
        return Result.ok();
    }

    @PostMapping("/from-alarm/{alarmId}")
    @Perm("workorder:add")
    public Result<Long> fromAlarm(@PathVariable Long alarmId) {
        return Result.ok(workorderService.fromAlarm(alarmId));
    }
}
