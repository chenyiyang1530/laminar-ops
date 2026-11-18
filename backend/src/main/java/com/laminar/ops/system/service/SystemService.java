package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.security.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SystemService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;
    private final SysConfigService sysConfig;
    private final RoleAuditLogger roleAuditLogger;

    private static final List<String> RESERVED_ROLE_KEYS = List.of(
            "super_admin", "hospital_admin", "hospital_dept", "hospital_sub_admin",
            "maintenance_manager", "regional_manager", "engineer",
            "company_executive", "department_staff", "company_admin", "company_sub_admin");


    /** 区域主管等业务下拉：仅内部账号（不含院方），按当前公司隔离 */
    public List<Map<String, Object>> userOptions() {
        Long companyId = dataScope.companyId(AuthContext.require());
        if (companyId == null) {
            return jdbc.queryForList(
                    "select u.user_id as `value`, concat(u.nick_name, '（', coalesce(d.dept_name, '未分配部门'), '）') as `label` " +
                            "from sys_user u left join sys_dept d on d.dept_id=u.dept_id " +
                            "where u.status='0' and u.user_type <> '2' order by u.user_id");
        }
        return jdbc.queryForList(
                "select u.user_id as `value`, concat(u.nick_name, '（', coalesce(d.dept_name, '未分配部门'), '）') as `label` " +
                        "from sys_user u left join sys_dept d on d.dept_id=u.dept_id " +
                        "where u.status='0' and u.user_type <> '2' and u.company_id=? order by u.user_id",
                companyId);
    }

    /** 公司内部可分配角色：平台核心角色 + 本公司自定义角色 */
    public List<Map<String, Object>> assignableRoles() {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        List<Map<String, Object>> roles = jdbc.queryForList(
                "select role_id as `roleId`, role_name as `roleName`, role_key as `roleKey`, " +
                        "company_id as `companyId`, assignable from sys_role where status='0' order by sort, role_id");
        int actorRank = userRank(user);
        return roles.stream().filter(role -> {
            Long roleCompany = role.get("companyId") == null ? null : ((Number) role.get("companyId")).longValue();
            if (user.isSuperAdmin()) return true;
            if (roleCompany != null && (companyId == null || !companyId.equals(roleCompany))) return false;
            if (roleCompany == null) {
                Long roleId = ((Number) role.get("roleId")).longValue();
                if (!"1".equals(String.valueOf(role.get("assignable")))) return false;
            }
            return roleRank(String.valueOf(role.get("roleKey"))) < actorRank;
        }).map(role -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("roleId", role.get("roleId"));
            item.put("roleName", role.get("roleName"));
            return item;
        }).toList();
    }

    public List<Map<String, Object>> postOptions() {
        Long companyId = dataScope.companyId(AuthContext.require());
        if (companyId == null) {
            return jdbc.queryForList(
                    "select post_id as `value`, post_name as `label` from sys_post where status='0' order by post_id");
        }
        return jdbc.queryForList(
                "select post_id as `value`, post_name as `label` from sys_post " +
                        "where status='0' and company_id=? order by post_id", companyId);
    }

    public List<Map<String, Object>> engineerOptions(Long requestedCompanyId, Long userId) {
        Long companyId = dataScope.companyId(AuthContext.require());
        if (companyId == null) companyId = requestedCompanyId;
        StringBuilder sql = new StringBuilder(
                "select e.engineer_id as `value`, e.engineer_name as `label`, e.user_id as `boundUserId`, " +
                        "coalesce(au.phone,e.phone) as `phone`, e.company_id as `companyId`, " +
                        "e.region_id as `regionId`, d.dept_id as `deptId` " +
                        "from biz_engineer e left join sys_user au on au.user_id=e.user_id " +
                        "left join sys_dept d on d.company_id=e.company_id and d.region_id=e.region_id " +
                        "where e.status='0' ");
        List<Object> args = new ArrayList<>();
        if (companyId != null) {
            sql.append(" and e.company_id=? ");
            args.add(companyId);
        }
        if (userId != null) {
            sql.append(" and (e.user_id is null or e.user_id=?) ");
            args.add(userId);
        } else {
            sql.append(" and e.user_id is null ");
        }
        sql.append(" order by e.engineer_name");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> userPage(int pageNum, int pageSize, Map<String, String> params) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        String username = params.get("username");
        String nickName = params.get("nickName");
        String phone = params.get("phone");
        String email = params.get("email");
        String status = params.get("status");
        String deptId = params.get("deptId");
        String roleId = params.get("roleId");
        String postId = params.get("postId");
        String companyIdParam = params.get("companyId");
        String userType = params.get("userType");
        if (notBlank(username)) { where.append(" and u.username like ? "); args.add("%" + username.trim() + "%"); }
        if (notBlank(nickName)) { where.append(" and u.nick_name like ? "); args.add("%" + nickName.trim() + "%"); }
        if (notBlank(phone)) { where.append(" and u.phone like ? "); args.add("%" + phone.trim() + "%"); }
        if (notBlank(email)) { where.append(" and u.email like ? "); args.add("%" + email.trim() + "%"); }
        if (notBlank(status)) { where.append(" and u.status = ? "); args.add(status); }
        if (notBlank(deptId)) { where.append(" and u.dept_id = ? "); args.add(deptId); }
        if (notBlank(postId)) { where.append(" and u.post_id = ? "); args.add(postId); }
        if (notBlank(roleId)) {
            where.append(" and exists (select 1 from sys_user_role ur where ur.user_id=u.user_id and ur.role_id=?) ");
            args.add(roleId);
        }
        if (notBlank(userType)) {
            where.append(" and u.user_type = ? ");
            args.add(userType);
        }
        LoginUser actor = AuthContext.require();
        Long companyId = dataScope.companyId(actor);
        if (companyId != null) {
            where.append(" and u.company_id = ? ");
            args.add(companyId);
        } else if (actor.isHospitalUser()) {
            Long hospitalId = actor.getHospitalId();
            if (hospitalId == null) {
                where.append(" and 1=0 ");
            } else {
                where.append(" and u.user_type = '2' and exists (select 1 from biz_hospital_user hu2 " +
                        "where hu2.user_id=u.user_id and hu2.hospital_id=?) ");
                args.add(hospitalId);
            }
        } else if (notBlank(companyIdParam)) {
            where.append(" and u.company_id = ? ");
            args.add(companyIdParam);
        }
        Long total = jdbc.queryForObject("select count(*) from sys_user u" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select u.user_id as `userId`, u.username, u.nick_name as `nickName`, u.phone, u.email, u.gender, " +
                        "u.dept_id as `deptId`, u.post_id as `postId`, u.company_id as `companyId`, u.user_type as `userType`, u.account_scope as `accountScope`, u.status, " +
                        "date_format(u.login_date,'%Y-%m-%d %H:%i') as `loginDate`, u.login_ip as `loginIp`, " +
                        "d.dept_name as `deptName`, p.post_name as `postName`, c.company_name as `companyName`, " +
                        "hh.hospital_name as `hospitalName`, " +
                        "eng.engineer_id as `engineerId`, eng.engineer_name as `engineerName`, rn.roleNames as `roleNames` " +
                        "from sys_user u left join sys_dept d on d.dept_id=u.dept_id left join sys_post p on p.post_id=u.post_id " +
                        "left join biz_company c on c.company_id=u.company_id " +
                        "left join biz_engineer eng on eng.user_id=u.user_id " +
                        "left join biz_hospital_user hu on hu.user_id=u.user_id " +
                        "left join biz_hospital hh on hh.hospital_id=hu.hospital_id " +
                        "left join (select ur.user_id, group_concat(r.role_name separator '、') as roleNames " +
                        "from sys_user_role ur join sys_role r on r.role_id=ur.role_id group by ur.user_id) rn on rn.user_id=u.user_id " +
                        where + " order by u.user_id limit ? offset ?", pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> userDetail(Long id) {
        Map<String, Object> user = jdbc.queryForMap(
                "select u.user_id as `userId`, u.username, u.nick_name as `nickName`, u.phone, u.email, u.gender, " +
                        "u.dept_id as `deptId`, u.post_id as `postId`, u.company_id as `companyId`, u.account_scope as `accountScope`, u.status, u.remark, " +
                        "eng.engineer_id as `engineerId`, eng.engineer_name as `engineerName` " +
                        "from sys_user u left join biz_engineer eng on eng.user_id=u.user_id where u.user_id=?", id);
        Long companyId = dataScope.companyId(AuthContext.require());
        Object targetCompany = user.get("companyId");
        if (companyId != null && (targetCompany == null
                || !companyId.equals(((Number) targetCompany).longValue()))) {
            throw BizException.forbidden("无权访问其他公司用户");
        }
        user.put("roleIds", jdbc.queryForList("select role_id from sys_user_role where user_id=?", Long.class, id));
        List<Long> hospitalIds = jdbc.queryForList(
                "select hospital_id from biz_hospital_user where user_id=?", Long.class, id);
        user.put("hospitalId", hospitalIds.isEmpty() ? null : hospitalIds.get(0));
        List<Long> hospitalDeptIds = jdbc.queryForList(
                "select department_id from biz_hospital_user where user_id=?", Long.class, id);
        user.put("hospitalDepartmentId", hospitalDeptIds.isEmpty() ? null : hospitalDeptIds.get(0));
        return user;
    }

    @Transactional
    public void saveUser(Map<String, Object> body) {
        String username = str(body.get("username")).trim();
        if (username.isBlank()) throw BizException.badRequest("请填写登录账号");
        if (!username.matches("^[A-Za-z][A-Za-z0-9_]{2,31}$")) {
            throw BizException.badRequest("账号需以字母开头，3-32 位字母、数字或下划线");
        }
        String nickName = validateNickName(body.get("nickName"));
        Object roleIds = body.get("roleIds");
        if (roleIds == null || (roleIds instanceof List<?> list && list.isEmpty())) {
            roleIds = roleIdsForAccountType(body.get("accountType"));
        }
        boolean hospitalRole = hasHospitalRole(roleIds);
        LoginUser actor = AuthContext.require();
        Long currentCompany = dataScope.companyId(actor);
        if (hospitalRole && currentCompany != null) {
            throw BizException.forbidden("院方账号由平台管理员或院方管理员创建");
        }
        Long hospitalId = longOf(body.get("hospitalId"));
        Long hospitalDepartmentId = longOf(body.get("hospitalDepartmentId"));
        if (hospitalRole && hospitalId == null) {
            throw BizException.badRequest("院方角色必须选择所属医院");
        }
        if (hospitalRole && hasRoleKey(roleIds, "hospital_sub_admin") && hospitalDepartmentId == null) {
            throw BizException.badRequest("院方子管理员必须选择所属科室");
        }
        if (hospitalRole) validateHospitalAccountActor(actor, roleIds, hospitalId, hospitalDepartmentId);
        if (hospitalDepartmentId != null) validateHospitalDepartment(hospitalId, hospitalDepartmentId);
        Long companyId = hospitalRole ? null : (currentCompany != null ? currentCompany : longOf(body.get("companyId")));
        if (companyId == null && !hospitalRole) throw BizException.badRequest("请选择所属公司");
        if (hospitalId != null) validateHospitalCompany(hospitalId, companyId);
        Integer exists = hospitalRole
                ? jdbc.queryForObject("select count(*) from sys_user where username=?", Integer.class, username)
                : jdbc.queryForObject("select count(*) from sys_user where username=? and company_id=?",
                Integer.class, username, companyId);
        if (exists != null && exists > 0) throw BizException.badRequest("已存在该登录账号");
        String phone = normalizeContact(body.get("phone"));
        String email = normalizeEmail(body.get("email"));
        validateContactUnique(phone, email, null);
        if (!hospitalRole) {
            validateUserOrg(companyId, body.get("deptId"), body.get("postId"));
            validateUserEngineerBinding(companyId, null, body);
        }
        String userType = hospitalRole ? "2" : "1";
        String accountScope = hospitalRole ? "hospital" : (companyId == null ? "platform" : "company");
        jdbc.update("insert into sys_user(username,password,must_change_pwd,nick_name,phone,email,gender,dept_id,post_id,company_id,user_type,status,create_time,account_scope,owner_company_id,owner_hospital_id,owner_department_id) " +
                        "values(?,?,1,?,?,?,?,?,?,?,?,'0',now(),?,?,?,?)", username, PasswordUtil.encode(initialPassword()),
                nickName, phone, email,
                str(body.getOrDefault("gender", "0")), hospitalRole ? null : body.get("deptId"),
                hospitalRole ? null : body.get("postId"), companyId, userType,
                accountScope, "company".equals(accountScope) ? companyId : null, hospitalRole ? hospitalId : null,
                hospitalRole ? hospitalDepartmentId : null);
        Long id = hospitalRole
                ? jdbc.queryForObject("select user_id from sys_user where username=? order by user_id desc limit 1", Long.class, username)
                : jdbc.queryForObject("select user_id from sys_user where username=? and company_id=?", Long.class, username, companyId);
        validateRoleAssignments(companyId, roleIds);
        saveRoles(id, roleIds);
        if (!hospitalRole) {
            applyRoleDefaultDept(id, companyId);
            bindEngineer(id, companyId, body.get("engineerId"));
        }
        syncHospitalUser(id, hospitalId, hospitalDepartmentId, roleIds, companyId);
        if (!hospitalRole) syncEngineerIdentity(id);
    }

    @Transactional
    public void updateUser(Map<String, Object> body) {
        Long id = longOf(body.get("userId"));
        assertCanManageUser(id, true);
        String nickName = validateNickName(body.get("nickName"));
        Long currentCompany = dataScope.companyId(AuthContext.require());
        Object targetCompany = jdbc.queryForObject("select company_id from sys_user where user_id=?", Object.class, id);
        if (currentCompany != null && (targetCompany == null
                || !currentCompany.equals(((Number) targetCompany).longValue()))) {
            throw BizException.forbidden("无权修改其他公司用户");
        }
        Long companyId = currentCompany != null ? currentCompany : longOf(body.get("companyId"));
        if (companyId == null && targetCompany != null) companyId = ((Number) targetCompany).longValue();
        Object roleIds = body.get("roleIds");
        boolean hospitalRole = hasHospitalRole(roleIds);
        if (hospitalRole && currentCompany != null) {
            throw BizException.forbidden("院方账号由平台管理员创建");
        }
        Long hospitalId = body.containsKey("hospitalId") ? longOf(body.get("hospitalId"))
                : jdbc.queryForList("select hospital_id from biz_hospital_user where user_id=?", Long.class, id)
                .stream().findFirst().orElse(null);
        Long hospitalDepartmentId = body.containsKey("hospitalDepartmentId") ? longOf(body.get("hospitalDepartmentId"))
                : jdbc.queryForList("select department_id from biz_hospital_user where user_id=?", Long.class, id)
                .stream().findFirst().orElse(null);
        if (hospitalRole && hasRoleKey(roleIds, "hospital_sub_admin") && hospitalDepartmentId == null) {
            throw BizException.badRequest("院方子管理员必须选择所属科室");
        }
        if (hospitalRole) validateHospitalAccountActor(AuthContext.require(), roleIds, hospitalId, hospitalDepartmentId);
        if (hospitalDepartmentId != null) validateHospitalDepartment(hospitalId, hospitalDepartmentId);
        if (hospitalRole && hospitalId == null) {
            throw BizException.badRequest("院方角色必须选择所属医院");
        }
        if (companyId == null && !hospitalRole) throw BizException.badRequest("请选择所属公司");
        if (hospitalRole) companyId = null;
        String phone = normalizeContact(body.get("phone"));
        String email = normalizeEmail(body.get("email"));
        validateContactUnique(phone, email, id);
        if (!hospitalRole) {
            validateUserOrg(companyId, body.get("deptId"), body.get("postId"));
            validateUserEngineerBinding(companyId, id, body);
        }
        Map<String, Object> old = jdbc.queryForMap(
                "select phone,email,phone_verified,email_verified from sys_user where user_id=?", id);
        int phoneVerified = Objects.equals(normalizeContact(old.get("phone")), phone) && intVal(old.get("phone_verified")) == 1 ? 1 : 0;
        int emailVerified = Objects.equals(normalizeContact(old.get("email")), email) && intVal(old.get("email_verified")) == 1 ? 1 : 0;
        String accountScope = hospitalRole ? "hospital" : (companyId == null ? "platform" : "company");
        jdbc.update("update sys_user set nick_name=?,phone=?,email=?,gender=?,dept_id=?,post_id=?,company_id=?,status=?," +
                        "phone_verified=?,phone_verified_at=case when ?=1 then coalesce(phone_verified_at,now()) else null end," +
                        "email_verified=?,email_verified_at=case when ?=1 then coalesce(email_verified_at,now()) else null end," +
                        "account_scope=?,owner_company_id=?,owner_hospital_id=?,owner_department_id=? " +
                        "where user_id=?",
                nickName, phone, email,
                str(body.getOrDefault("gender", "0")), hospitalRole ? null : body.get("deptId"),
                hospitalRole ? null : body.get("postId"),
                companyId, str(body.getOrDefault("status", "0")),
                phoneVerified, phoneVerified, emailVerified, emailVerified,
                accountScope, "company".equals(accountScope) ? companyId : null,
                hospitalRole ? hospitalId : null, hospitalRole ? hospitalDepartmentId : null, id);
        if (body.containsKey("roleIds")) {
            List<Long> beforeRoles = jdbc.queryForList(
                    "select role_id from sys_user_role where user_id=?", Long.class, id);
            validateRoleAssignments(companyId, roleIds);
            jdbc.update("delete from sys_user_role where user_id=?", id);
            saveRoles(id, roleIds);
            roleAuditLogger.logRemoved(id, beforeRoles, roleIds);
        }
        if (hospitalId != null) validateHospitalCompany(hospitalId, companyId);
        if (body.containsKey("roleIds") || body.containsKey("hospitalId")) {
            jdbc.update("update sys_user set user_type=? where user_id=?", hospitalRole ? "2" : "1", id);
            syncHospitalUser(id, hospitalId, hospitalDepartmentId, roleIds, companyId);
        }
        if (!hospitalRole) {
            applyRoleDefaultDept(id, companyId);
            if (body.containsKey("engineerId")) {
                bindEngineer(id, companyId, body.get("engineerId"));
            }
            syncEngineerIdentity(id);
        }
    }

    @Transactional
    public void deleteUser(Long[] ids) {
        for (Long id : ids) {
            if (id == 1L) throw BizException.badRequest("超级管理员不可删除");
            assertCanManageUser(id, false);
            for (Long roleId : jdbc.queryForList("select role_id from sys_user_role where user_id=?", Long.class, id)) {
                roleAuditLogger.log(roleId, id, "revoke", "删除用户时移除角色");
            }
            jdbc.update("update biz_engineer set user_id=null where user_id=?", id);
            jdbc.update("delete from sys_user_role where user_id=?", id);
            jdbc.update("delete from sys_user where user_id=?", id);
        }
    }

    public void resetPwd(Long id) {
        assertCanManageUser(id, false);
        jdbc.update("update sys_user set password=?, must_change_pwd=1, login_fail_count=0, lock_until=null where user_id=?",
                PasswordUtil.encode(initialPassword()), id);
    }

    private String initialPassword() {
        String value = sysConfig.value("sys.user.initPassword", "Laminar@2026");
        return value == null || value.isBlank() ? "Laminar@2026" : value;
    }

    public void changeUserStatus(Long id, String status) {
        assertCanManageUser(id, false);
        jdbc.update("update sys_user set status=? where user_id=?", status, id);
    }

    private void bindEngineer(Long userId, Long companyId, Object engineerIdValue) {
        jdbc.update("update biz_engineer set user_id=null where user_id=?", userId);
        Long engineerId = longOf(engineerIdValue);
        if (engineerId == null) return;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select company_id as `companyId`, user_id as `userId` from biz_engineer where engineer_id=?",
                engineerId);
        if (rows.isEmpty()) throw BizException.badRequest("工程师不存在");
        Object engineerCompany = rows.get(0).get("companyId");
        if (engineerCompany == null || !companyId.equals(((Number) engineerCompany).longValue())) {
            throw BizException.forbidden("只能绑定本公司工程师");
        }
        Object boundUser = rows.get(0).get("userId");
        if (boundUser != null && !userId.equals(((Number) boundUser).longValue())) {
            throw BizException.badRequest("该工程师已绑定其他用户");
        }
        try {
            jdbc.update("update biz_engineer set user_id=? where engineer_id=?", userId, engineerId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw BizException.badRequest("该工程师已绑定其他用户");
        }
        jdbc.update("update sys_user u join biz_engineer e on e.user_id=u.user_id " +
                "left join sys_dept d on d.company_id=e.company_id and d.region_id=e.region_id " +
                "set u.dept_id=d.dept_id where u.user_id=?", userId);
    }

    private void syncEngineerIdentity(Long userId) {
        jdbc.update("update biz_engineer e join sys_user u on u.user_id=e.user_id " +
                "set e.engineer_name=u.nick_name, e.phone=u.phone where u.user_id=?", userId);
    }

    private void validateRoleAssignments(Long companyId, Object roleIds) {
        if (!(roleIds instanceof List<?> list)) return;
        LoginUser user = AuthContext.require();
        int actorRank = userRank(user);
        for (Object r : list) {
            Long roleId = ((Number) r).longValue();
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select role_key, company_id, assignable from sys_role where role_id=?", roleId);
            if (rows.isEmpty()) throw BizException.badRequest("无权分配该角色");
            if (user.isSuperAdmin()) continue;
            Map<String, Object> role = rows.get(0);
            Long roleCompany = role.get("company_id") == null ? null : ((Number) role.get("company_id")).longValue();
            if (roleCompany != null && (companyId == null || !companyId.equals(roleCompany))) {
                throw BizException.badRequest("无权分配该角色");
            }
            if (roleCompany == null && !"1".equals(String.valueOf(role.get("assignable")))) {
                throw BizException.badRequest("无权分配该角色");
            }
            if (roleRank(String.valueOf(role.get("role_key"))) >= actorRank) {
                throw BizException.badRequest("无权分配同级或更高权限角色");
            }
        }
    }

    private int userRank(LoginUser user) {
        if (user == null) return 0;
        if (user.isSuperAdmin()) return 1000;
        int rank = 0;
        if (user.getRoleKeys() != null) {
            for (String key : user.getRoleKeys()) rank = Math.max(rank, roleRank(key));
        }
        return rank;
    }

    private Object roleIdsForAccountType(Object accountType) {
        String key = switch (String.valueOf(accountType)) {
            case "hospital_admin" -> "hospital_admin";
            case "hospital_sub_admin" -> "hospital_sub_admin";
            case "hospital_dept" -> "hospital_dept";
            case "engineer" -> "engineer";
            default -> null;
        };
        if (key == null) return null;
        List<Long> ids = jdbc.queryForList(
                "select role_id from sys_role where role_key=? and status='0' limit 1", Long.class, key);
        return ids.isEmpty() ? null : ids;
    }

    private boolean hasHospitalRole(Object roleIds) {
        if (!(roleIds instanceof List<?> list)) return false;
        for (Object item : list) {
            if (item == null) continue;
            List<String> keys = jdbc.queryForList(
                    "select role_key from sys_role where role_id=?", String.class,
                    Long.valueOf(String.valueOf(item)));
            for (String key : keys) {
                if (key != null && key.startsWith("hospital_")) return true;
            }
        }
        return false;
    }

    private void validateHospitalAccountActor(LoginUser actor, Object roleIds, Long hospitalId, Long departmentId) {
        if (actor.isSuperAdmin()) return;
        if (!actor.getRoleKeys().contains("hospital_admin")) {
            throw BizException.forbidden("院方账号由平台管理员或院方管理员创建");
        }
        List<String> requested = roleKeys(roleIds);
        boolean allowed = !requested.isEmpty() && requested.stream()
                .allMatch(key -> "hospital_sub_admin".equals(key) || "hospital_dept".equals(key));
        if (!allowed) {
            throw BizException.forbidden("院方管理员只能创建院方子管理员或院方科室用户");
        }
        Long actorHospital = actor.getHospitalId();
        if (actorHospital == null) {
            List<Long> ids = jdbc.queryForList(
                    "select hospital_id from biz_hospital_user where user_id=?", Long.class, actor.getUserId());
            actorHospital = ids.isEmpty() ? null : ids.get(0);
        }
        if (hospitalId == null || !hospitalId.equals(actorHospital)) {
            throw BizException.forbidden("只能创建本院账号");
        }
        if (departmentId == null) {
            throw BizException.badRequest("院方子管理员或科室用户必须选择所属科室");
        }
    }

    private List<String> roleKeys(Object roleIds) {
        List<String> keys = new ArrayList<>();
        if (!(roleIds instanceof List<?> list)) return keys;
        for (Object item : list) {
            if (item == null) continue;
            keys.addAll(jdbc.queryForList("select role_key from sys_role where role_id=?",
                    String.class, Long.valueOf(String.valueOf(item))));
        }
        return keys;
    }

    private boolean hasRoleKey(Object roleIds, String roleKey) {
        if (!(roleIds instanceof List<?> list)) return false;
        for (Object item : list) {
            if (item == null) continue;
            Integer count = jdbc.queryForObject(
                    "select count(*) from sys_role where role_id=? and role_key=?",
                    Integer.class, Long.valueOf(String.valueOf(item)), roleKey);
            if (count != null && count > 0) return true;
        }
        return false;
    }

    private void validateHospitalDepartment(Long hospitalId, Long departmentId) {
        if (hospitalId == null) throw BizException.badRequest("请先选择院方医院");
        Integer count = jdbc.queryForObject(
                "select count(*) from biz_department where department_id=? and hospital_id=?",
                Integer.class, departmentId, hospitalId);
        if (count == null || count == 0) {
            throw BizException.badRequest("所选科室不属于该医院");
        }
    }

    private void validateHospitalCompany(Long hospitalId, Long companyId) {
        Integer count;
        if (companyId == null) {
            count = jdbc.queryForObject(
                    "select count(*) from biz_hospital where hospital_id=? and status='0'",
                    Integer.class, hospitalId);
        } else {
            count = jdbc.queryForObject(
                    "select count(*) from biz_company_hospital where hospital_id=? and company_id=?",
                    Integer.class, hospitalId, companyId);
        }
        if (count == null || count == 0) {
            throw BizException.badRequest("所选医院不属于当前公司");
        }
    }

    private void syncHospitalUser(Long userId, Long hospitalId, Long hospitalDepartmentId, Object roleIds, Long companyId) {
        jdbc.update("delete from biz_hospital_user where user_id=?", userId);
        if (hospitalId == null) return;
        String roleType = hasRoleKey(roleIds, "hospital_admin") ? "admin"
                : hasRoleKey(roleIds, "hospital_sub_admin") ? "sub_admin" : "dept";
        jdbc.update("insert into biz_hospital_user(user_id,hospital_id,department_id,role_type,status,create_time) " +
                        "values(?,?,?,?,'0',now())",
                userId, hospitalId, hospitalDepartmentId, roleType);
    }

    private int roleRank(String roleKey) {
        return switch (roleKey == null ? "" : roleKey) {
            case "super_admin" -> 1000;
            case "company_admin" -> 900;
            case "company_sub_admin" -> 800;
            case "company_executive" -> 700;
            case "maintenance_manager" -> 600;
            case "regional_manager" -> 500;
            case "engineer" -> 400;
            case "department_staff" -> 300;
            case "hospital_admin" -> 200;
            case "hospital_sub_admin" -> 150;
            case "hospital_dept" -> 100;
            default -> 10;
        };
    }

    private int targetRank(Long userId) {
        List<String> keys = jdbc.queryForList(
                "select r.role_key from sys_user_role ur join sys_role r on r.role_id=ur.role_id " +
                        "where ur.user_id=?", String.class, userId);
        int rank = 0;
        for (String key : keys) rank = Math.max(rank, roleRank(key));
        return rank;
    }

    private void assertCanManageUser(Long targetId, boolean allowSelf) {
        LoginUser actor = AuthContext.require();
        if (actor.isSuperAdmin()) return;
        if (allowSelf && targetId.equals(actor.getUserId())) return;
        Long actorCompany = dataScope.companyId(actor);
        List<Long> targetCompanies = jdbc.queryForList(
                "select company_id from sys_user where user_id=?", Long.class, targetId);
        if (actorCompany == null || targetCompanies.isEmpty() || targetCompanies.get(0) == null
                || !actorCompany.equals(targetCompanies.get(0))) {
            throw BizException.forbidden("无权操作其他公司用户");
        }
        if (userRank(actor) <= targetRank(targetId)) {
            throw BizException.forbidden("无权操作同级或更高权限用户");
        }
    }

    private void saveRoles(Long userId, Object roleIds) {
        if (roleIds instanceof List<?> list) {
            for (Object r : list) {
                Long roleId = ((Number) r).longValue();
                jdbc.update("insert ignore into sys_user_role(user_id,role_id) values(?,?)", userId, roleId);
                roleAuditLogger.log(roleId, userId, "assign", "分配角色");
            }
        }
    }

    public Map<String, Object> roleAssignLogs(Long roleId, String action, Integer pageNum, Integer pageSize) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            Long owner = roleCompany(roleId);
            if (owner == null || !companyId.equals(owner)) {
                throw BizException.forbidden("无权查看该角色分配记录");
            }
        }
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        String actionFilter = action == null ? "" : action.trim();
        List<String> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where l.role_id=? ");
        args.add(String.valueOf(roleId));
        if (!actionFilter.isBlank()) {
            if (!List.of("assign", "revoke", "create", "update", "delete").contains(actionFilter)) {
                throw BizException.badRequest("不支持的动作类型");
            }
            where.append(" and l.action=? ");
            args.add(actionFilter);
        }
        Long total = jdbc.queryForObject(
                "select count(*) from sys_role_assign_log l" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((page - 1) * size);
        List<Map<String, Object>> records = jdbc.queryForList(
                "select l.log_id as `logId`, l.role_id as `roleId`, l.user_id as `userId`, l.action, " +
                        "l.operator_name as `operatorName`, l.detail, date_format(l.create_time,'%Y-%m-%d %H:%i:%s') as `createTime`, " +
                        "coalesce(l.role_name_snapshot, r.role_name) as `roleName`, u.nick_name as `userName` " +
                        "from sys_role_assign_log l left join sys_role r on r.role_id=l.role_id " +
                        "left join sys_user u on u.user_id=l.user_id" + where +
                        " order by l.log_id desc limit ? offset ?", pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    private void applyRoleDefaultDept(Long userId, Long companyId) {
        if (userId == null || companyId == null) return;
        Integer count = jdbc.queryForObject(
                "select count(*) from sys_user_role ur join sys_role r on r.role_id=ur.role_id " +
                        "where ur.user_id=? and r.role_key='maintenance_manager'", Integer.class, userId);
        if (count == null || count == 0) return;
        Long deptId = jdbc.queryForObject(
                "select dept_id from sys_user where user_id=?", Long.class, userId);
        if (deptId != null) return;
        List<Long> candidates = jdbc.queryForList(
                "select dept_id from sys_dept where company_id=? " +
                        "and dept_name in ('维保部','维保中心','维保事业部') " +
                        "order by case dept_name when '维保部' then 0 when '维保中心' then 1 else 2 end, dept_id limit 1",
                Long.class, companyId);
        if (!candidates.isEmpty()) {
            jdbc.update("update sys_user set dept_id=? where user_id=?", candidates.get(0), userId);
        }
    }

    public List<Map<String, Object>> roleList() {
        Long companyId = dataScope.companyId(AuthContext.require());
        String sql = "select r.role_id as `roleId`, r.role_name as `roleName`, r.role_key as `roleKey`, r.sort, " +
                "r.data_scope as `dataScope`, r.status, r.remark, r.company_id as `companyId`, " +
                "r.role_type as `roleType`, r.assignable, c.company_name as `companyName` from sys_role r " +
                "left join biz_company c on c.company_id=r.company_id ";
        if (companyId == null) {
            return jdbc.queryForList(sql + " order by r.sort");
        }
        return jdbc.queryForList(sql +
                " where (r.company_id = ?) or (r.company_id is null and r.assignable = '1') " +
                " order by r.sort", companyId);
    }

    public void saveRole(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        String roleKey = str(body.get("roleKey")).trim().toLowerCase();
        if (roleKey.isBlank()) throw BizException.badRequest("请填写权限字符");
        if (companyId != null && RESERVED_ROLE_KEYS.contains(roleKey)) {
            throw BizException.badRequest("该权限字符为平台保留角色");
        }
        Integer exists = jdbc.queryForObject(
                "select count(*) from sys_role where role_key=? and company_id <=> ?",
                Integer.class, roleKey, companyId);
        if (exists != null && exists > 0) throw BizException.badRequest("本公司已存在该权限字符");
        jdbc.update("insert into sys_role(company_id,role_type,role_name,role_key,sort,data_scope,status,remark,create_time,assignable) " +
                        "values(?,'custom',?,?,?,?,'0',?,now(),?)",
                companyId, str(body.get("roleName")), roleKey, body.getOrDefault("sort", 0),
                str(body.getOrDefault("dataScope", "self")), str(body.get("remark")),
                companyId == null ? str(body.getOrDefault("assignable", "1")) : "1");
        Long createdId = jdbc.queryForObject(
                "select role_id from sys_role where role_key=? and company_id <=> ? order by role_id desc limit 1",
                Long.class, roleKey, companyId);
        if (createdId != null) roleAuditLogger.log(createdId, null, "create", "创建角色");
    }

    public void updateRole(Map<String, Object> body) {
        Long roleId = longOf(body.get("roleId"));
        Long companyId = dataScope.companyId(AuthContext.require());
        Long roleCompany = roleCompany(roleId);
        if (companyId != null && (roleCompany == null || !companyId.equals(roleCompany))) {
            throw BizException.forbidden("平台标准角色不可修改");
        }
        String roleKey = str(body.get("roleKey")).trim().toLowerCase();
        if (roleKey.isBlank()) throw BizException.badRequest("请填写权限字符");
        if (companyId != null && RESERVED_ROLE_KEYS.contains(roleKey)) {
            throw BizException.badRequest("该权限字符为平台保留角色");
        }
        Integer exists = jdbc.queryForObject(
                "select count(*) from sys_role where role_key=? and role_id<>? and company_id <=> ?",
                Integer.class, roleKey, roleId, roleCompany);
        if (exists != null && exists > 0) throw BizException.badRequest("本公司已存在该权限字符");
        jdbc.update("update sys_role set role_name=?,role_key=?,sort=?,data_scope=?,status=?,remark=?,assignable=? where role_id=?",
                str(body.get("roleName")), roleKey, body.getOrDefault("sort", 0),
                str(body.getOrDefault("dataScope", "self")), str(body.getOrDefault("status", "0")),
                str(body.get("remark")), companyId == null ? str(body.getOrDefault("assignable", "1")) : currentAssignable(roleId), roleId);
        roleAuditLogger.log(roleId, null, "update", "修改角色");
    }

    public void deleteRole(Long id) {
        if (id == 1L) throw BizException.badRequest("超级管理员角色不可删除");
        Long companyId = dataScope.companyId(AuthContext.require());
        Long roleCompany = roleCompany(id);
        if (companyId != null && (roleCompany == null || !companyId.equals(roleCompany))) {
            throw BizException.forbidden("平台标准角色不可删除");
        }
        String roleType = jdbc.queryForObject("select role_type from sys_role where role_id=?", String.class, id);
        if ("standard".equals(roleType)) throw BizException.badRequest("标准角色不可删除");
        roleAuditLogger.log(id, null, "delete", "删除角色");
        jdbc.update("delete from sys_role_menu where role_id=?", id);
        jdbc.update("delete from sys_user_role where role_id=?", id);
        jdbc.update("delete from sys_role where role_id=?", id);
    }

    public List<Long> roleMenuIds(Long roleId) {
        checkRoleOwned(roleId);
        return jdbc.queryForList("select menu_id from sys_role_menu where role_id=?", Long.class, roleId);
    }

    @Transactional
    public void assignMenus(Long roleId, List<?> menuIds) {
        LoginUser user = AuthContext.require();
        checkRoleOwned(roleId);
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            Set<Long> grantable = grantableMenuIds(user);
            if (menuIds != null) {
                for (Object m : menuIds) {
                    Long menuId = ((Number) m).longValue();
                    if (!grantable.contains(menuId)) {
                        throw BizException.forbidden("不能授予超出当前账号范围的菜单权限");
                    }
                }
            }
        }
        jdbc.update("delete from sys_role_menu where role_id=?", roleId);
        if (menuIds != null) {
            for (Object m : menuIds) {
                jdbc.update("insert ignore into sys_role_menu(role_id,menu_id) values(?,?)", roleId, ((Number) m).longValue());
            }
        }
    }

    private void checkRoleOwned(Long roleId) {
        Long companyId = dataScope.companyId(AuthContext.require());
        if (companyId == null) return;
        Long roleCompany = roleCompany(roleId);
        if (roleCompany == null || !companyId.equals(roleCompany)) {
            throw BizException.forbidden("平台标准角色不可分配菜单");
        }
    }

    private String currentAssignable(Long roleId) {
        String value = jdbc.queryForObject("select assignable from sys_role where role_id=?", String.class, roleId);
        return value == null ? "1" : value;
    }

    private Long roleCompany(Long roleId) {        List<Object> rows = jdbc.queryForList("select company_id from sys_role where role_id=?", Object.class, roleId);
        if (rows.isEmpty() || rows.get(0) == null) return null;
        return ((Number) rows.get(0)).longValue();
    }

    public List<Map<String, Object>> menuTree() {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select menu_id as `menuId`, menu_name as `menuName`, parent_id as `parentId`, " +
                        "order_num as `orderNum`, path, component, perms, menu_type as `menuType`, icon, status, visible " +
                        "from sys_menu order by parent_id, order_num");
        if (companyId == null) return dedupeHiddenMenus(projectSamePermissionButtons(rows));
        Set<Long> grantable = grantableMenuIds(user, rows);
        return dedupeHiddenMenus(projectSamePermissionButtons(rows.stream()
                .filter(row -> grantable.contains(((Number) row.get("menuId")).longValue()))
                .toList()));
    }

    /**
     * 隐藏页面如果和某个可见页面使用同一 list 权限，则把它的按钮权限挂到可见页面下，
     * 并隐藏这个重复页面节点。例如 6100 工程师管理 -> 6110 人员档案。
     */
    private List<Map<String, Object>> projectSamePermissionButtons(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        Map<String, Long> visibleByParentPerm = new LinkedHashMap<>();
        Map<String, Long> visibleByPerm = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("menuId")).longValue();
            byId.put(id, row);
            if ("C".equals(String.valueOf(row.get("menuType")))
                    && !"1".equals(String.valueOf(row.get("visible")))
                    && row.get("perms") != null) {
                String perms = String.valueOf(row.get("perms"));
                Long parentId = row.get("parentId") == null ? 0L : ((Number) row.get("parentId")).longValue();
                visibleByParentPerm.putIfAbsent(parentId + "\u0000" + perms, id);
                visibleByPerm.putIfAbsent(perms, id);
            }
        }
        Map<Long, Long> redirect = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            if (!"C".equals(String.valueOf(row.get("menuType")))
                    || !"1".equals(String.valueOf(row.get("visible")))
                    || row.get("perms") == null) continue;
            Long id = ((Number) row.get("menuId")).longValue();
            Long parentId = row.get("parentId") == null ? 0L : ((Number) row.get("parentId")).longValue();
            String perms = String.valueOf(row.get("perms"));
            Long target = visibleByParentPerm.getOrDefault(parentId + "\u0000" + perms, visibleByPerm.get(perms));
            if (target != null && !target.equals(id)) redirect.put(id, target);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("menuId")).longValue();
            if (redirect.containsKey(id)) continue;
            Object p = row.get("parentId");
            Long parentId = p == null ? 0L : ((Number) p).longValue();
            if (redirect.containsKey(parentId)) {
                Map<String, Object> copy = new LinkedHashMap<>(row);
                copy.put("parentId", redirect.get(parentId));
                out.add(copy);
            } else {
                out.add(row);
            }
        }
        return out;
    }

    /**
     * 同一父节点下已有同名可见菜单时，隐藏的别名菜单不再出现在权限树里。
     * 例如 2110 公司管理与 2111 公司管理（/organization/company 为隐藏别名）。
     */
    private List<Map<String, Object>> dedupeHiddenMenus(List<Map<String, Object>> rows) {
        Set<String> visibleKeys = new HashSet<>();
        for (Map<String, Object> row : rows) {
            if (!"1".equals(String.valueOf(row.get("visible")))) {
                Object p = row.get("parentId");
                visibleKeys.add(String.valueOf(p) + "\u0000" + row.get("menuName"));
            }
        }
        return rows.stream().filter(row -> {
            boolean hidden = "1".equals(String.valueOf(row.get("visible")));
            String key = String.valueOf(row.get("parentId")) + "\u0000" + row.get("menuName");
            return !hidden || !visibleKeys.contains(key);
        }).toList();
    }

    /**
     * 当前公司账号可授出的菜单：
     * 1. 只能授出自己角色已拥有的菜单；
     * 2. 必须包含祖先目录，便于前端树展示；
     * 3. 平台系统管理整棵子树永久排除。
     */
    private Set<Long> grantableMenuIds(LoginUser user) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select menu_id as `menuId`, parent_id as `parentId` from sys_menu order by parent_id, order_num");
        return grantableMenuIds(user, rows);
    }

    private Set<Long> grantableMenuIds(LoginUser user, List<Map<String, Object>> rows) {
        Map<Long, Long> parent = new LinkedHashMap<>();
        Map<Long, List<Long>> children = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("menuId")).longValue();
            Object p = row.get("parentId");
            Long parentId = p == null ? 0L : ((Number) p).longValue();
            parent.put(id, parentId);
            children.computeIfAbsent(parentId, k -> new ArrayList<>()).add(id);
        }
        Set<Long> allowed = new LinkedHashSet<>(jdbc.queryForList(
                "select distinct rm.menu_id from sys_user_role ur join sys_role_menu rm on rm.role_id=ur.role_id " +
                        "where ur.user_id=?", Long.class, user.getUserId()));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Long id : new ArrayList<>(allowed)) {
                Long pid = parent.get(id);
                if (pid != null && pid > 0 && allowed.add(pid)) changed = true;
            }
        }
        Set<Long> systemMenus = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(9200L);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (!systemMenus.add(id)) continue;
            queue.addAll(children.getOrDefault(id, List.of()));
        }
        allowed.removeAll(systemMenus);
        return allowed;
    }

    public List<Map<String, Object>> deptTree(Long requestedCompanyId) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (companyId == null) companyId = requestedCompanyId;
        StringBuilder sql = new StringBuilder(
                "select d.dept_id as `deptId`, d.parent_id as `parentId`, d.region_id as `regionId`, " +
                        "r.region_name as `regionName`, d.dept_name as `deptName`, " +
                        "d.company_id as `companyId`, c.company_name as `companyName`, " +
                        "d.leader, d.phone, d.email, d.sort, d.status " +
                        "from sys_dept d left join biz_company c on c.company_id=d.company_id " +
                        "left join biz_region r on r.region_id=d.region_id where 1=1 ");
        List<Object> args = new ArrayList<>();
        if (companyId != null) {
            sql.append(" and d.company_id=? ");
            args.add(companyId);
        }
        sql.append(" order by d.parent_id, d.sort");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public void saveDept(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (companyId == null) companyId = longOf(body.get("companyId"));
        if (companyId == null) throw BizException.badRequest("请选择所属公司");
        String name = str(body.get("deptName"));
        if (name.isBlank()) throw BizException.badRequest("请填写部门名称");
        Long parentId = longOf(body.getOrDefault("parentId", 0));
        if (parentId == null) parentId = 0L;
        validateDeptParent(companyId, parentId, null);
        Integer dup = jdbc.queryForObject(
                "select count(*) from sys_dept where company_id=? and parent_id=? and dept_name=?",
                Integer.class, companyId, parentId, name);
        if (dup != null && dup > 0) throw BizException.badRequest("同级部门名称已存在");
        Long regionId = validateDeptRegion(companyId, longOf(body.get("regionId")), null);
        String leader = regionId == null ? str(body.get("leader")) : regionLeaderName(regionId);
        jdbc.update("insert into sys_dept(company_id,parent_id,region_id,dept_name,leader,phone,email,sort,status,create_time) " +
                        "values(?,?,?,?,?,?,?,?,'0',now())",
                companyId, parentId, regionId, name, leader,
                str(body.get("phone")), str(body.get("email")), body.getOrDefault("sort", 0));
    }

    public void updateDept(Map<String, Object> body) {
        Long deptId = longOf(body.get("deptId"));
        Long companyId = deptCompany(deptId);
        checkCompany(companyId);
        String name = str(body.get("deptName"));
        if (name.isBlank()) throw BizException.badRequest("请填写部门名称");
        Long parentId = longOf(body.getOrDefault("parentId", 0));
        if (parentId == null) parentId = 0L;
        if (parentId.equals(deptId)) throw BizException.badRequest("上级部门不能是自己");
        validateDeptParent(companyId, parentId, deptId);
        Integer dup = jdbc.queryForObject(
                "select count(*) from sys_dept where company_id=? and parent_id=? and dept_name=? and dept_id<>?",
                Integer.class, companyId, parentId, name, deptId);
        if (dup != null && dup > 0) throw BizException.badRequest("同级部门名称已存在");
        Long regionId = validateDeptRegion(companyId, longOf(body.get("regionId")), deptId);
        String leader = regionId == null ? str(body.get("leader")) : regionLeaderName(regionId);
        jdbc.update("update sys_dept set parent_id=?,region_id=?,dept_name=?,leader=?,phone=?,email=?,sort=? where dept_id=?",
                parentId, regionId, name, leader, str(body.get("phone")),
                str(body.get("email")), body.getOrDefault("sort", 0), deptId);
    }

    private Long validateDeptRegion(Long companyId, Long regionId, Long deptId) {
        if (regionId == null) return null;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select company_id from biz_region where region_id=?", regionId);
        if (rows.isEmpty() || rows.get(0).get("company_id") == null
                || !companyId.equals(((Number) rows.get(0).get("company_id")).longValue())) {
            throw BizException.badRequest("关联区域不存在或不属于本公司");
        }
        Integer used = jdbc.queryForObject(
                "select count(*) from sys_dept where region_id=? and dept_id<>?",
                Integer.class, regionId, deptId == null ? 0L : deptId);
        if (used != null && used > 0) throw BizException.badRequest("该地理区域已关联其他部门");
        return regionId;
    }

    private String regionLeaderName(Long regionId) {
        if (regionId == null) return null;
        List<String> names = jdbc.queryForList(
                "select u.nick_name from biz_region r join sys_user u on u.user_id=r.manager_id " +
                        "where r.region_id=?", String.class, regionId);
        return names.isEmpty() ? null : names.get(0);
    }

    public void deleteDept(Long id) {
        checkCompany(deptCompany(id));
        Integer children = jdbc.queryForObject("select count(*) from sys_dept where parent_id=?", Integer.class, id);
        if (children != null && children > 0) throw BizException.badRequest("存在下级部门，不能删除");
        Integer users = jdbc.queryForObject("select count(*) from sys_user where dept_id=?", Integer.class, id);
        if (users != null && users > 0) throw BizException.badRequest("部门下存在员工，不能删除");
        jdbc.update("delete from sys_dept where dept_id=?", id);
    }

    private Long deptCompany(Long deptId) {
        List<Long> ids = jdbc.queryForList("select company_id from sys_dept where dept_id=?", Long.class, deptId);
        if (ids.isEmpty()) throw BizException.badRequest("部门不存在");
        return ids.get(0);
    }

    private void validateDeptParent(Long companyId, Long parentId, Long selfId) {
        if (parentId == null || parentId == 0) return;
        Long parentCompany = deptCompany(parentId);
        if (!companyId.equals(parentCompany)) throw BizException.badRequest("上级部门不属于当前公司");
        if (selfId != null) {
            Long cursor = parentId;
            while (cursor != null && cursor != 0) {
                if (cursor.equals(selfId)) throw BizException.badRequest("不能把部门移动到自己的下级");
                List<Long> parents = jdbc.queryForList(
                        "select parent_id from sys_dept where dept_id=?", Long.class, cursor);
                cursor = parents.isEmpty() ? null : parents.get(0);
            }
        }
    }

    private void checkCompany(Long rowCompanyId) {
        Long companyId = dataScope.companyId(AuthContext.require());
        if (companyId != null && !companyId.equals(rowCompanyId)) {
            throw BizException.forbidden("无权操作其他公司的组织架构");
        }
    }

    private void validateUserOrg(Long companyId, Object deptId, Object postId) {
        if (deptId != null) {
            Long deptCompany = deptCompany(longOf(deptId));
            if (!companyId.equals(deptCompany)) throw BizException.badRequest("部门不属于所选公司");
        }
        if (postId != null) {
            List<Long> companies = jdbc.queryForList(
                    "select company_id from sys_post where post_id=?", Long.class, longOf(postId));
            if (companies.isEmpty() || !companyId.equals(companies.get(0))) {
                throw BizException.badRequest("岗位不属于所选公司");
            }
        }
    }

    private void validateUserEngineerBinding(Long companyId, Long userId, Map<String, Object> body) {
        Long engineerId = longOf(body.get("engineerId"));
        if (engineerId == null) return;
        Long deptId = longOf(body.get("deptId"));
        if (deptId == null && userId != null) {
            List<Long> depts = jdbc.queryForList("select dept_id from sys_user where user_id=?", Long.class, userId);
            deptId = depts.isEmpty() ? null : depts.get(0);
        }
        if (deptId == null || !isMaintenanceDept(companyId, deptId)) {
            throw BizException.badRequest("只有维保部及下级部门可以绑定工程师");
        }
    }

    private boolean isMaintenanceDept(Long companyId, Long deptId) {
        Long current = deptId;
        int guard = 0;
        while (current != null && guard++ < 20) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select dept_name, parent_id, company_id from sys_dept where dept_id=?", current);
            if (rows.isEmpty()) return false;
            Map<String, Object> row = rows.get(0);
            if (row.get("company_id") == null || !companyId.equals(((Number) row.get("company_id")).longValue())) {
                return false;
            }
            String name = str(row.get("dept_name"));
            if ("维保部".equals(name) || "维保中心".equals(name) || "维保事业部".equals(name)) {
                return true;
            }
            current = longOf(row.get("parent_id"));
        }
        return false;
    }

    public Map<String, Object> loginLogPage(int pageNum, int pageSize, String username) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (notBlank(username)) { where.append(" and username like ? "); args.add("%" + username.trim() + "%"); }
        Long total = jdbc.queryForObject("select count(*) from sys_login_log" + where, Long.class, args.toArray());
        args.add(pageSize);
        args.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select id, username, ip, browser, os, status, msg, date_format(login_time,'%Y-%m-%d %H:%i:%s') as `loginTime` " +
                        "from sys_login_log" + where + " order by id desc limit ? offset ?", args.toArray());
        return Map.of("records", records, "total", total == null ? 0 : total);
    }

    public Map<String, Object> operLogPage(int pageNum, int pageSize, String title) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (notBlank(title)) { where.append(" and (title like ? or oper_name like ?) "); args.add("%" + title.trim() + "%"); args.add("%" + title.trim() + "%"); }
        Long total = jdbc.queryForObject("select count(*) from sys_oper_log" + where, Long.class, args.toArray());
        args.add(pageSize);
        args.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select id, title, request_method as `requestMethod`, oper_name as `operName`, oper_url as `operUrl`, " +
                        "oper_ip as `operIp`, status, error_msg as `errorMsg`, cost_time as `costTime`, " +
                        "date_format(oper_time,'%Y-%m-%d %H:%i:%s') as `operTime` from sys_oper_log" +
                        where + " order by id desc limit ? offset ?", args.toArray());
        return Map.of("records", records, "total", total == null ? 0 : total);
    }

    public void deleteLogs(String type, Long[] ids) {
        String table = "oper".equals(type) ? "sys_oper_log" : "sys_login_log";
        for (Long id : ids) {
            jdbc.update("delete from " + table + " where id=?", id);
        }
    }

    public List<Map<String, Object>> jobs() {
        return jdbc.queryForList("select job_id as `jobId`, job_name as `jobName`, bean_name as `beanName`, " +
                "method_name as `methodName`, cron, status, last_result as `lastResult`, " +
                "date_format(last_run_time,'%Y-%m-%d %H:%i:%s') as `lastRunTime` from sys_job order by job_id");
    }

    public void toggleJob(Long id, String status) {
        jdbc.update("update sys_job set status=? where job_id=?", status, id);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String validateNickName(Object value) {
        String nickName = str(value).trim();
        if (nickName.isBlank()) throw BizException.badRequest("请填写姓名");
        if (nickName.length() > 30) throw BizException.badRequest("姓名不能超过 30 个字符");
        return nickName;
    }

    private String normalizeContact(Object o) {
        String value = str(o).trim();
        if (value.isBlank()) return null;
        if (!value.matches("^1[3-9]\\d{9}$")) {
            throw BizException.badRequest("手机号格式不正确，请输入 11 位手机号");
        }
        return value;
    }

    private String normalizeEmail(Object o) {
        String value = str(o).trim().toLowerCase();
        if (value.isBlank()) return null;
        if (value.length() > 100 || !value.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw BizException.badRequest("邮箱格式不正确");
        }
        return value;
    }

    private void validateContactUnique(String phone, String email, Long excludeUserId) {
        if (phone != null) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from sys_user where phone=? and (? is null or user_id<>?)",
                    Integer.class, phone, excludeUserId, excludeUserId);
            if (count != null && count > 0) throw BizException.badRequest("手机号已被其他账号使用");
        }
        if (email != null) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from sys_user where lower(email)=? and (? is null or user_id<>?)",
                    Integer.class, email, excludeUserId, excludeUserId);
            if (count != null && count > 0) throw BizException.badRequest("邮箱已被其他账号使用");
        }
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Long longOf(Object o) {
        return o == null ? null : Long.valueOf(String.valueOf(o));
    }

    private int intVal(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }
}







