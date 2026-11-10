package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.ContractScopeService;
import com.laminar.ops.biz.service.MonitorService;
import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;
    private final DataScopeService dataScope;
    private final ContractScopeService contractScope;

    @GetMapping("/overview")
    @Perm("device:realtime:view")
    public Result<Map<String, Object>> overview() {
        return Result.ok(monitorService.overview(dataScope.visibleHospitalIds(AuthContext.require())));
    }

    @GetMapping("/equipment/{id}")
    @Perm("device:realtime:view")
    public Result<Map<String, Object>> equipment(@PathVariable Long id) {
        com.laminar.ops.security.LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (!dataScope.canAccessEquipment(user, id)
                || (companyId != null && !contractScope.coversEquipment(companyId, id))) {
            throw BizException.forbidden("无权查看该医院设备");
        }
        return Result.ok(monitorService.equipmentDetail(id));
    }

    @GetMapping("/history")
    @Perm("device:realtime:view")
    public Result<List<Map<String, Object>>> history(@RequestParam Long pointId,
                                                     @RequestParam(defaultValue = "24") int hours) {
        com.laminar.ops.security.LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (!dataScope.canAccessPoint(user, pointId)
                || (companyId != null && !contractScope.coversPoint(companyId, pointId))) {
            throw BizException.forbidden("无权查看该设备历史数据");
        }
        return Result.ok(monitorService.history(pointId, hours));
    }

    @PostMapping("/collect")
    @Perm("device:realtime:view")
    public Result<Integer> collect() {
        com.laminar.ops.security.LoginUser user = AuthContext.require();
        if (!"all".equals(user.getDataScope()) || dataScope.companyId(user) != null) {
            throw BizException.forbidden("仅系统管理员可手动触发全局采集");
        }
        return Result.ok(monitorService.collect());
    }
}
