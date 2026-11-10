package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.AlarmService;
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
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/page")
    @Perm("alarm:list")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam Map<String, String> params) {
        params.remove("pageNum");
        params.remove("pageSize");
        return Result.ok(alarmService.page(pageNum, pageSize, params));
    }

    @GetMapping("/{id}")
    @Perm("alarm:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(alarmService.detail(id));
    }

    @PostMapping("/{id}/confirm")
    @Perm("alarm:confirm")
    public Result<Void> confirm(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        alarmService.confirm(id, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/handle")
    @Perm("alarm:handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        alarmService.handle(id, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    @Perm("alarm:close")
    public Result<Void> close(@PathVariable Long id, @RequestBody Map<String, String> body) {
        alarmService.close(id, body.get("rootCause"), body.get("solution"));
        return Result.ok();
    }

    @PostMapping("/{id}/ignore")
    @Perm("alarm:ignore")
    public Result<Void> ignore(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        alarmService.ignore(id, body == null ? null : body.get("reason"));
        return Result.ok();
    }
}
