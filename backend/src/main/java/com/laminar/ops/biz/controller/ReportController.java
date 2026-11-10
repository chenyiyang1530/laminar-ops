package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.ReportService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final DataScopeService dataScope;

    @GetMapping("/summary")
    @Perm("report:view")
    public Result<Map<String, Object>> summary(@RequestParam(required = false) Integer year) {
        com.laminar.ops.security.LoginUser user = AuthContext.require();
        return Result.ok(reportService.summary(year,
                dataScope.visibleHospitalIds(user), dataScope.companyId(user)));
    }
}
