package com.laminar.ops.system.service;

import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NavBadgeService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;

    public Map<String, Object> badges() {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        String company = companyId == null ? "" : " and company_id=" + companyId + " ";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("alarmUnhandled", count(
                "select count(*) from biz_alarm where status in ('0','1','2')" + company));
        result.put("workorderPending", count(
                "select count(*) from biz_workorder where status in ('0','1')" + company));
        result.put("inspectionOverdue", count(
                "select count(*) from biz_inspection_task where status='3'" + company));
        result.put("maintenanceOverdue", count(
                "select count(*) from biz_maintenance_task where status='3'" + company));
        result.put("controlPending", count(
                "select count(*) from biz_control_command where status='PENDING_REVIEW'" + company));
        return result;
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
