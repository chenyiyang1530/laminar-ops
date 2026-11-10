package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.DashboardService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DataScopeService dataScope;

    @GetMapping("/overview")
    @Perm("dashboard:view")
    public Result<Map<String, Object>> overview() {
        LoginUser user = AuthContext.require();
        return Result.ok(dashboardService.overview(
                dataScope.visibleHospitalIds(user), dataScope.companyId(user)));
    }
}
