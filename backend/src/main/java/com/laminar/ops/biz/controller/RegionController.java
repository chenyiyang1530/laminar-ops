package com.laminar.ops.biz.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laminar.ops.biz.entity.BizRegion;
import com.laminar.ops.biz.mapper.BizRegionMapper;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/region")
@RequiredArgsConstructor
public class RegionController {

    private final BizRegionMapper regionMapper;
    private final DataScopeService dataScope;

    @GetMapping("/options")
    public Result<List<BizRegion>> options() {
        Long companyId = dataScope.companyId(AuthContext.require());
        return Result.ok(regionMapper.selectList(new LambdaQueryWrapper<BizRegion>()
                .eq(BizRegion::getStatus, "0")
                .eq(companyId != null, BizRegion::getCompanyId, companyId)
                .orderByAsc(BizRegion::getRegionId)));
    }
}
