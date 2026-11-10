package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EngineerSelfService {

    private final JdbcTemplate jdbc;

    public Map<String, Object> status() {
        LoginUser user = AuthContext.require();
        Long engineerId = user.getEngineerId();
        if (engineerId == null) throw BizException.badRequest("当前账号未绑定工程师");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select e.engineer_id as `engineerId`, e.engineer_name as `engineerName`, e.on_duty as `onDuty`, " +
                        "e.company_id as `companyId`, e.region_id as `regionId`, r.region_name as `regionName`, " +
                        "r.manager_id as `managerId`, u.nick_name as `managerName`, u.status as `managerStatus`, " +
                        "u.company_id as `managerCompany` " +
                        "from biz_engineer e left join biz_region r on r.region_id=e.region_id " +
                        "left join sys_user u on u.user_id=r.manager_id where e.engineer_id=?", engineerId);
        if (rows.isEmpty()) throw BizException.badRequest("工程师档案不存在");
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        Long companyId = result.get("companyId") == null ? null : ((Number) result.get("companyId")).longValue();
        List<Long> designated = companyId == null ? List.of() : jdbc.queryForList(
                "select total_manager_id from biz_company where company_id=? and total_manager_id is not null",
                Long.class, companyId);
        boolean totalManager = !designated.isEmpty() && user.getUserId().equals(designated.get(0));
        boolean regionalManager = user.getRoleKeys().contains("regional_manager");
        boolean engineerRole = user.getRoleKeys().contains("engineer");
        if (!totalManager && !regionalManager && !engineerRole) {
            result.put("canApplyLeave", false);
        } else if (totalManager || regionalManager) {
            result.put("canApplyLeave", true);
        } else {
            result.put("canApplyLeave", result.get("regionId") != null);
        }
        Long active = jdbc.queryForObject(
                "select count(*) from biz_workorder where engineer_id=? and status in ('0','1','2','3','4')",
                Long.class, engineerId);
        result.put("activeOrders", active == null ? 0 : active);
        result.put("canOffDuty", active == null || active == 0);
        return result;
    }

    public void setOnDuty(boolean onDuty) {
        LoginUser user = AuthContext.require();
        Long engineerId = user.getEngineerId();
        if (engineerId == null) throw BizException.badRequest("当前账号未绑定工程师");
        if (!onDuty) {
            Long active = jdbc.queryForObject(
                    "select count(*) from biz_workorder where engineer_id=? and status in ('0','1','2','3','4')",
                    Long.class, engineerId);
            if (active != null && active > 0) {
                throw BizException.badRequest("还有 " + active + " 张在手单，请先改派后再离岗");
            }
        }
        jdbc.update("update biz_engineer set on_duty=? where engineer_id=?", onDuty ? "1" : "0", engineerId);
    }
}
