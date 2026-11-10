package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/release")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(releaseService.list());
    }

    @GetMapping("/latest")
    public Result<Map<String, Object>> latest() {
        return Result.ok(releaseService.latest());
    }

    @PostMapping("/seen")
    public Result<Void> seen(@RequestBody(required = false) Map<String, Object> body) {
        Long id = body == null || body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        releaseService.markSeen(id);
        return Result.ok();
    }

    @PostMapping
    @Perm("system:release:edit")
    public Result<Void> add(@RequestBody Map<String, Object> body) {
        releaseService.save(body);
        return Result.ok();
    }

    @PutMapping
    @Perm("system:release:edit")
    public Result<Void> edit(@RequestBody Map<String, Object> body) {
        releaseService.update(body);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("system:release:edit")
    public Result<Void> remove(@PathVariable Long id) {
        releaseService.delete(id);
        return Result.ok();
    }
}
