package com.laminar.ops.security;

import com.laminar.ops.biz.entity.BizEngineer;
import com.laminar.ops.biz.entity.BizHospitalUser;
import com.laminar.ops.biz.mapper.BizEngineerMapper;
import com.laminar.ops.biz.mapper.BizHospitalUserMapper;
import com.laminar.ops.common.BizException;
import com.laminar.ops.system.entity.SysRole;
import com.laminar.ops.system.entity.SysUser;
import com.laminar.ops.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoginUserService {

    private final SysUserMapper userMapper;
    private final BizEngineerMapper engineerMapper;
    private final BizHospitalUserMapper hospitalUserMapper;

    private static final List<String> SCOPE_ORDER = List.of("all", "region", "hospital", "self");

    public LoginUser load(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        if (!"0".equals(user.getStatus())) {
            throw BizException.unauthorized("账号已被停用");
        }
        List<SysRole> roles = userMapper.selectRolesByUserId(userId);
        Set<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
        List<String> perms = userMapper.selectPermsByUserId(userId);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickName(user.getNickName());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setUserType(user.getUserType());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setRoleKeys(roleKeys);
        loginUser.setPermissions(perms == null ? Set.of() : perms.stream()
                .flatMap(p -> List.of(p.split(",")).stream())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet()));
        loginUser.setDataScope(resolveScope(roles));
        loginUser.setCompanyId(user.getCompanyId());
        loginUser.setMustChangePassword(user.getMustChangePwd() != null && user.getMustChangePwd() == 1);

        if (roleKeys.contains("super_admin")) {
            loginUser.getPermissions().add("*:*:*");
        }

        BizEngineer engineer = engineerMapper.selectByUserId(userId);
        if (engineer != null) {
            loginUser.setEngineerId(engineer.getEngineerId());
            loginUser.setRegionId(engineer.getRegionId());
            if (loginUser.getCompanyId() == null) {
                loginUser.setCompanyId(engineer.getCompanyId());
            }
        }
        BizHospitalUser hu = hospitalUserMapper.selectByUserId(userId);
        if (hu != null && "0".equals(hu.getStatus())) {
            loginUser.setHospitalId(hu.getHospitalId());
            loginUser.setDepartmentId(hu.getDepartmentId());
        }
        return loginUser;
    }

    private String resolveScope(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return "self";
        }
        String best = "self";
        for (SysRole role : roles) {
            String s = role.getDataScope() == null ? "self" : role.getDataScope();
            if (SCOPE_ORDER.indexOf(s) < SCOPE_ORDER.indexOf(best)) {
                best = s;
            }
        }
        return best;
    }
}
