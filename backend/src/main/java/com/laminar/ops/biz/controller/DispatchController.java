package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.SmartDispatchService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final SmartDispatchService smartDispatchService;

    @GetMapping("/recommend/{workorderId}")
    @Perm("staff:dispatch:view")
    public Result<Map<String, Object>> recommend(@PathVariable Long workorderId) {
        return Result.ok(smartDispatchService.recommend(workorderId));
    }
}
