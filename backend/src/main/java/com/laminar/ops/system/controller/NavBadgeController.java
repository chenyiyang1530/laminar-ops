package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.system.service.NavBadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/nav")
@RequiredArgsConstructor
public class NavBadgeController {

    private final NavBadgeService navBadgeService;

    @GetMapping("/badges")
    public Result<Map<String, Object>> badges() {
        return Result.ok(navBadgeService.badges());
    }
}
