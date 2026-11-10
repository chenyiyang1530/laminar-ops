package com.laminar.ops.security;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
public class LoginUser implements Serializable {

    private Long userId;
    private String username;
    private String nickName;
    private String avatar;
    /** 0 超管 1 公司 2 院方 */
    private String userType;
    private Long hospitalId;
    private Long departmentId;
    private Long deptId;
    private Long engineerId;
    private Long regionId;
    private Long companyId;
    private boolean mustChangePassword;
    private String dataScope = "self";
    private Set<String> roleKeys = new HashSet<>();
    private Set<String> permissions = new HashSet<>();

    public boolean isSuperAdmin() {
        return roleKeys != null && roleKeys.contains("super_admin");
    }

    public boolean hasPerm(String perm) {
        return isSuperAdmin() || (permissions != null && permissions.contains(perm));
    }

    public boolean isHospitalUser() {
        return "2".equals(userType);
    }
}
