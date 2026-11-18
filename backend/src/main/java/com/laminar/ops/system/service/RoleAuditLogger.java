package com.laminar.ops.system.service;

import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 角色分配审计写入，独立于用户/角色主流程，便于后续扩展 before/after 快照。 */
@Service
@RequiredArgsConstructor
public class RoleAuditLogger {

    private final JdbcTemplate jdbc;

    public void log(Long roleId, Long userId, String action, String detail) {
        LoginUser user = AuthContext.require();
        Long operatorId = user == null ? null : user.getUserId();
        String operatorName = user == null ? "system" : user.getNickName();
        String roleName = jdbc.queryForObject("select role_name from sys_role where role_id=?", String.class, roleId);
        jdbc.update("insert into sys_role_assign_log(role_id,role_name_snapshot,user_id,action,operator_id,operator_name,detail) " +
                        "values(?,?,?,?,?,?,?)",
                roleId, roleName, userId, action, operatorId, operatorName, detail);
    }

    public void logRemoved(Long userId, List<Long> before, Object afterRoleIds) {
        List<Long> after = new ArrayList<>();
        if (afterRoleIds instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) after.add(((Number) item).longValue());
            }
        }
        for (Long roleId : before) {
            if (!after.contains(roleId)) {
                log(roleId, userId, "revoke", "移除角色");
            }
        }
    }
}
