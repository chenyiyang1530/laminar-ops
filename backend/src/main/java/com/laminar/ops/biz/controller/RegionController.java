package com.laminar.ops.biz.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laminar.ops.biz.entity.BizRegion;
import com.laminar.ops.biz.mapper.BizRegionMapper;
import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/region")
@RequiredArgsConstructor
public class RegionController {

    private final BizRegionMapper regionMapper;
    private final DataScopeService dataScope;
    private final JdbcTemplate jdbc;

    @GetMapping("/options")
    public Result<List<BizRegion>> options() {
        Long companyId = dataScope.companyId(AuthContext.require());
        return Result.ok(regionMapper.selectList(new LambdaQueryWrapper<BizRegion>()
                .eq(BizRegion::getStatus, "0")
                .eq(companyId != null, BizRegion::getCompanyId, companyId)
                .orderByAsc(BizRegion::getRegionId)));
    }

    @GetMapping("/{regionId}/manager-options")
    @Perm("customer:region:list")
    public Result<List<Map<String, Object>>> managerOptions(@PathVariable Long regionId) {
        LoginUser user = AuthContext.require();
        List<Long> companies = jdbc.queryForList(
                "select company_id from biz_region where region_id=?", Long.class, regionId);
        if (companies.isEmpty()) throw BizException.badRequest("区域不存在");
        Long regionCompany = companies.get(0);
        Long companyId = dataScope.companyId(user);
        if (companyId != null && !companyId.equals(regionCompany)) {
            throw BizException.forbidden("无权访问其他公司区域");
        }
        return Result.ok(jdbc.queryForList(
                "select u.user_id as value, u.nick_name as label " +
                        "from biz_engineer e join sys_user u on u.user_id=e.user_id " +
                        "where e.region_id=? and e.company_id=? and u.status='0' " +
                        "order by u.nick_name",
                regionId, regionCompany));
    }
}
