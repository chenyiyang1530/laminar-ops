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

    private static final List<String> RESERVED_ROLE_KEYS = List.of(
            "super_admin", "hospital_admin", "hospital_dept",
            "maintenance_manager", "regional_manager", "engineer",
            "company_executive", "department_staff");

    /** 区域主管等业务下拉：仅内部账号（不含院方），按当前公司隔离 */
    public List<Map<String, Object>> userOptions() {
        Long companyId = dataScope.companyId(AuthContext.require());
        if (companyId == null) {
            return jdbc.queryForList(
                    "select u.user_id as `value`, concat(u.nick_name, '（', u.username, '）') as `label` " +
                            "from sys_user u where u.status='0' and u.user_type <> '2' order by u.user_id");
        }
        return jdbc.queryForList(
                "select u.user_id as `value`, concat(u.nick_name, '（', u.username, '）') as `label` " +
                        "from sys_user u where u.status='0' and u.user_type <> '2' and u.company_id=? order by u.user_id",
                companyId);
    }

    /** 公司内部可分配角色：平台核心角色 + 本公司自定义角色 */
    public List<Map<String, Object>> assignableRoles() {
        Long companyId = dataScope.companyId(AuthContext.require());
        if (companyId == null) {
            return jdbc.queryForList(
                    "select role_id as `roleId`, role_name as `roleName` from sys_role " +
                            "where company_id is null and role_id in (100,101,102,104) order by role_id");
        }
        return jdbc.queryForList(
                "select role_id as `roleId`, role_name as `roleName` from sys_role " +
                        "where (company_id = ?) or (company_id is null and role_id in (100,101,102,104)) " +
                        "order by role_id", companyId);
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
                "select engineer_id as `value`, engineer_name as `label`, user_id as `boundUserId`, " +
                        "phone, company_id as `companyId` from biz_engineer where status='0' ");
        List<Object> args = new ArrayList<>();
        if (companyId != null) {
            sql.append(" and company_id=? ");
            args.add(companyId);
        }
        if (userId != null) {
            sql.append(" and (user_id is null or user_id=?) ");
            args.add(userId);
        } else {
            sql.append(" and user_id is null ");
        }
        sql.append(" order by engineer_name");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> userPage(int pageNum, int pageSize, Map<String, String> params) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        String username = params.get("username");
        String nickName = params.get("nickName");
        String status = params.get("status");
        String deptId = params.get("deptId");
        if (notBlank(username)) { where.append(" and u.username like ? "); args.add("%" + username.trim() + "%"); }
        if (notBlank(nickName)) { where.append(" and u.nick_name like ? "); args.add("%" + nickName.trim() + "%"); }
        if (notBlank(status)) { where.append(" and u.status = ? "); args.add(status); }
        if (notBlank(deptId)) { where.append(" and u.dept_id = ? "); args.add(deptId); }
        where.append(" and u.user_type <> '2' ");
        Long companyId = dataScope.companyId(AuthContext.require());
        if (companyId != null) {
            where.append(" and u.company_id = ? ");
            args.add(companyId);
        }
        Long total = jdbc.queryForObject("select count(*) from sys_user u" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select u.user_id as `userId`, u.username, u.nick_name as `nickName`, u.phone, u.email, u.gender, " +
                        "u.dept_id as `deptId`, u.post_id as `postId`, u.company_id as `companyId`, u.user_type as `userType`, u.status, " +
                        "date_format(u.login_date,'%Y-%m-%d %H:%i') as `loginDate`, u.login_ip as `loginIp`, " +
                        "d.dept_name as `deptName`, p.post_name as `postName`, c.company_name as `companyName`, " +
                        "eng.engineer_id as `engineerId`, eng.engineer_name as `engineerName`, " +
                        "(select group_concat(r.role_name separator '、') from sys_user_role ur join sys_role r on r.role_id=ur.role_id " +
                        "where ur.user_id=u.user_id) as `roleNames` " +
                        "from sys_user u left join sys_dept d on d.dept_id=u.dept_id left join sys_post p on p.post_id=u.post_id " +
                        "left join biz_company c on c.company_id=u.company_id " +
                        "left join biz_engineer eng on eng.user_id=u.user_id " +
                        where + " order by u.user_id limit ? offset ?", pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> userDetail(Long id) {
        Map<String, Object> user = jdbc.queryForMap(
                "select u.user_id as `userId`, u.username, u.nick_name as `nickName`, u.phone, u.email, u.gender, " +
                        "u.dept_id as `deptId`, u.post_id as `postId`, u.company_id as `companyId`, u.status, u.remark, " +
                        "eng.engineer_id as `engineerId`, eng.engineer_name as `engineerName` " +
                        "from sys_user u left join biz_engineer eng on eng.user_id=u.user_id where u.user_id=?", id);
        Long companyId = dataScope.companyId(AuthContext.require());
        Object targetCompany = user.get("companyId");
        if (companyId != null && (targetCompany == null
                || !companyId.equals(((Number) targetCompany).longValue()))) {
            throw BizException.forbidden("无权访问其他公司用户");
        }
        user.put("roleIds", jdbc.queryForList("select role_id from sys_user_role where user_id=?", Long.class, id));
        return user;
    }

    @Transactional
    public void saveUser(Map<String, Object> body) {
        String username = str(body.get("username"));
        if (username.isBlank()) throw BizException.badRequest("请填写登录账号");
        Long currentCompany = dataScope.companyId(AuthContext.require());
        Long companyId = currentCompany != null ? currentCompany : longOf(body.get("companyId"));
        if (companyId == null) throw BizException.badRequest("请选择所属公司");
        Integer exists = jdbc.queryForObject(
                "select count(*) from sys_user where username=? and company_id=?",
                Integer.class, username, companyId);
        if (exists != null && exists > 0) throw BizException.badRequest("本公司已存在该登录账号");
        String phone = normalizeContact(body.get("phone"));
        String email = normalizeEmail(body.get("email"));
        validateContactUnique(phone, email, null);
        validateUserOrg(companyId, body.get("deptId"), body.get("postId"));
        jdbc.update("insert into sys_user(username,password,must_change_pwd,nick_name,phone,email,gender,dept_id,post_id,company_id,user_type,status,create_time) " +
                        "values(?,?,1,?,?,?,?,?,?,?,'1','0',now())", username, PasswordUtil.encode("123456"),
                str(body.get("nickName")), phone, email,
                str(body.getOrDefault("gender", "0")), body.get("deptId"), body.get("postId"), companyId);
        Long id = jdbc.queryForObject(
                "select user_id from sys_user where username=? and company_id=?", Long.class, username, companyId);
        validateRoleAssignments(companyId, body.get("roleIds"));
        saveRoles(id, body.get("roleIds"));
        bindEngineer(id, companyId, body.get("engineerId"));
    }

    @Transactional
    public void updateUser(Map<String, Object> body) {
        Long id = longOf(body.get("userId"));
        Long currentCompany = dataScope.companyId(AuthContext.require());
        Object targetCompany = jdbc.queryForObject("select company_id from sys_user where user_id=?", Object.class, id);
        if (currentCompany != null && (targetCompany == null
                || !currentCompany.equals(((Number) targetCompany).longValue()))) {
            throw BizException.forbidden("无权修改其他公司用户");
        }
        Long companyId = currentCompany != null ? currentCompany : longOf(body.get("companyId"));
        if (companyId == null && targetCompany != null) companyId = ((Number) targetCompany).longValue();
        if (companyId == null) throw BizException.badRequest("请选择所属公司");
        String phone = normalizeContact(body.get("phone"));
        String email = normalizeEmail(body.get("email"));
        validateContactUnique(phone, email, id);
        validateUserOrg(companyId, body.get("deptId"), body.get("postId"));
        Map<String, Object> old = jdbc.queryForMap(
                "select phone,email,phone_verified,email_verified from sys_user where user_id=?", id);
        int phoneVerified = Objects.equals(normalizeContact(old.get("phone")), phone) && intVal(old.get("phone_verified")) == 1 ? 1 : 0;
        int emailVerified = Objects.equals(normalizeContact(old.get("email")), email) && intVal(old.get("email_verified")) == 1 ? 1 : 0;
        jdbc.update("update sys_user set nick_name=?,phone=?,email=?,gender=?,dept_id=?,post_id=?,company_id=?,status=?," +
                        "phone_verified=?,phone_verified_at=case when ?=1 then coalesce(phone_verified_at,now()) else null end," +
                        "email_verified=?,email_verified_at=case when ?=1 then coalesce(email_verified_at,now()) else null end " +
                        "where user_id=?",
                str(body.get("nickName")), phone, email,
                str(body.getOrDefault("gender", "0")), body.get("deptId"), body.get("postId"),
                companyId, str(body.getOrDefault("status", "0")),
                phoneVerified, phoneVerified, emailVerified, emailVerified, id);
        if (body.containsKey("roleIds")) {
            validateRoleAssignments(companyId, body.get("roleIds"));
            jdbc.update("delete from sys_user_role where user_id=?", id);
            saveRoles(id, body.get("roleIds"));
        }
        if (body.containsKey("engineerId")) {
            bindEngineer(id, companyId, body.get("engineerId"));
        }
    }

    @Transactional
    public void deleteUser(Long[] ids) {
        for (Long id : ids) {
            if (id == 1L) throw BizException.badRequest("超级管理员不可删除");
            jdbc.update("update biz_engineer set user_id=null where user_id=?", id);
            jdbc.update("delete from sys_user_role where user_id=?", id);
            jdbc.update("delete from sys_user where user_id=?", id);
        }
    }

    public void resetPwd(Long id) {
        jdbc.update("update sys_user set password=?, must_change_pwd=1, login_fail_count=0, lock_until=null where user_id=?",
                PasswordUtil.encode("123456"), id);
    }

    public void changeUserStatus(Long id, String status) {
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
    }

    private void validateRoleAssignments(Long companyId, Object roleIds) {
        if (!(roleIds instanceof List<?> list)) return;
        for (Object r : list) {
            Long roleId = ((Number) r).longValue();
            Integer allowed = jdbc.queryForObject(
                    "select count(*) from sys_role where role_id=? and " +
                            "(company_id=? or (company_id is null and role_id in (100,101,102,104)))",
                    Integer.class, roleId, companyId);
            if (allowed == null || allowed == 0) {
                throw BizException.badRequest("无权分配该角色");
            }
        }
    }

    private void saveRoles(Long userId, Object roleIds) {
        if (roleIds instanceof List<?> list) {
            for (Object r : list) {
                jdbc.update("insert ignore into sys_user_role(user_id,role_id) values(?,?)", userId, ((Number) r).longValue());
            }
        }
    }

    public List<Map<String, Object>> roleList() {
        Long companyId = dataScope.companyId(AuthContext.require());
        String sql = "select r.role_id as `roleId`, r.role_name as `roleName`, r.role_key as `roleKey`, r.sort, " +
                "r.data_scope as `dataScope`, r.status, r.remark, r.company_id as `companyId`, " +
                "r.role_type as `roleType`, c.company_name as `companyName` from sys_role r " +
                "left join biz_company c on c.company_id=r.company_id ";
        if (companyId == null) {
            return jdbc.queryForList(sql + " order by r.sort");
        }
        return jdbc.queryForList(sql +
                " where (r.company_id = ?) or (r.company_id is null and r.role_type = 'standard') " +
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
        jdbc.update("insert into sys_role(company_id,role_type,role_name,role_key,sort,data_scope,status,remark,create_time) " +
                        "values(?,'custom',?,?,?,?,'0',?,now())",
                companyId, str(body.get("roleName")), roleKey, body.getOrDefault("sort", 0),
                str(body.getOrDefault("dataScope", "self")), str(body.get("remark")));
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
        jdbc.update("update sys_role set role_name=?,role_key=?,sort=?,data_scope=?,status=?,remark=? where role_id=?",
                str(body.get("roleName")), roleKey, body.getOrDefault("sort", 0),
                str(body.getOrDefault("dataScope", "self")), str(body.getOrDefault("status", "0")),
                str(body.get("remark")), roleId);
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

    private Long roleCompany(Long roleId) {
        List<Object> rows = jdbc.queryForList("select company_id from sys_role where role_id=?", Object.class, roleId);
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
        jdbc.update("insert into sys_dept(company_id,parent_id,region_id,dept_name,leader,phone,email,sort,status,create_time) " +
                        "values(?,?,?,?,?,?,?,?,'0',now())",
                companyId, parentId, regionId, name, str(body.get("leader")),
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
        jdbc.update("update sys_dept set parent_id=?,region_id=?,dept_name=?,leader=?,phone=?,email=?,sort=? where dept_id=?",
                parentId, regionId, name, str(body.get("leader")), str(body.get("phone")),
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

    private String normalizeContact(Object o) {
        String value = str(o).trim();
        return value.isBlank() ? null : value;
    }

    private String normalizeEmail(Object o) {
        String value = str(o).trim().toLowerCase();
        return value.isBlank() ? null : value;
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
