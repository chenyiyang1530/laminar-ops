package com.laminar.ops.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laminar.ops.biz.entity.BizHospitalUser;
import com.laminar.ops.biz.mapper.BizHospitalUserMapper;
import com.laminar.ops.biz.service.CompanyCodeService;
import com.laminar.ops.common.BizException;
import com.laminar.ops.security.JwtUtil;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.security.LoginUserService;
import com.laminar.ops.security.PasswordUtil;
import com.laminar.ops.system.entity.SysLoginLog;
import com.laminar.ops.system.entity.SysRole;
import com.laminar.ops.system.entity.SysUser;
import com.laminar.ops.system.mapper.SysLoginLogMapper;
import com.laminar.ops.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final LoginUserService loginUserService;
    private final BizHospitalUserMapper hospitalUserMapper;
    private final JwtUtil jwtUtil;
    private final MenuService menuService;
    private final StringRedisTemplate redis;
    private final CompanyCodeService companyCodeService;
    private final JdbcTemplate jdbc;

    public Map<String, Object> login(String companyCode, String username, String password, HttpServletRequest request) {
        String account = username == null ? "" : username.trim();
        if (account.isBlank()) throw BizException.badRequest("请输入手机号、邮箱或账号");
        String ip = clientIp(request);
        String ua = request.getHeader("User-Agent");
        List<SysUser> candidates;
        if (account.contains("@")) {
            // 邮箱全局唯一，直接定位账号，不要求公司编码
            candidates = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, account.toLowerCase()));
        } else if (account.matches("^\\+?\\d{6,20}$")) {
            // 手机号全局唯一，直接定位账号，不要求公司编码
            candidates = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, account));
        } else if (companyCode != null && !companyCode.isBlank()) {
            Long companyId = companyCodeService.resolveCompanyId(companyCode);
            if (companyId == null) {
                writeLoginLog(account, ip, ua, "1", "账号或密码错误");
                throw BizException.badRequest("账号或密码错误");
            }
            candidates = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, account)
                    .eq(SysUser::getCompanyId, companyId));
        } else {
            candidates = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, account));
            if (candidates.size() > 1) {
                throw BizException.badRequest("存在多个同名账号，请输入公司编码");
            }
        }
        if (candidates.size() > 1) {
            throw BizException.badRequest("账号信息异常，请联系管理员");
        }
        SysUser user = candidates.isEmpty() ? null : candidates.get(0);
        if (user != null && user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            writeLoginLog(account, ip, ua, "1", "账号锁定中");
            throw BizException.badRequest("连续登录失败，账号已锁定，请15分钟后再试");
        }
        if (user != null && user.getLockUntil() != null) {
            jdbc.update("update sys_user set login_fail_count=0, lock_until=null where user_id=?",
                    user.getUserId());
            user.setLoginFailCount(0);
            user.setLockUntil(null);
        }
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            if (user != null) recordLoginFailure(user, account, ip, ua);
            writeLoginLog(account, ip, ua, "1", "账号或密码错误");
            throw BizException.badRequest("账号或密码错误");
        }
        if (!"0".equals(user.getStatus())) {
            writeLoginLog(account, ip, ua, "1", "账号已停用");
            throw BizException.badRequest("账号已停用，请联系管理员");
        }
        if (account.contains("@") && !Integer.valueOf(1).equals(user.getEmailVerified())) {
            writeLoginLog(account, ip, ua, "1", "邮箱未验证");
            throw BizException.badRequest("邮箱未验证，请先用账号登录并完成邮箱验证");
        }
        if (account.matches("^\\+?\\d{6,20}$") && !Integer.valueOf(1).equals(user.getPhoneVerified())) {
            writeLoginLog(account, ip, ua, "1", "手机号未验证");
            throw BizException.badRequest("手机号未验证，请先用账号登录并完成手机号验证");
        }
        if ("2".equals(user.getUserType())) {
            BizHospitalUser hu = hospitalUserMapper.selectByUserId(user.getUserId());
            if (hu == null || !"0".equals(hu.getStatus())) {
                writeLoginLog(account, ip, ua, "1", "院方账号未绑定医院");
                throw BizException.badRequest("院方账号未绑定医院，请联系平台管理员");
            }
        }
        String token = jwtUtil.createToken(user.getUserId(), user.getUsername());

        jdbc.update("update sys_user set login_ip=?, login_date=now(), login_fail_count=0, lock_until=null where user_id=?",
                ip, user.getUserId());
        writeLoginLog(account, ip, ua, "0", "登录成功");
        redis.opsForValue().set("online:" + user.getUserId(),
                user.getUsername() + "|" + user.getNickName() + "|" + ip + "|" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                Duration.ofMinutes(30));

        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("expiresIn", jwtUtil.getExpireSeconds());
        res.put("username", user.getUsername());
        res.put("nickName", user.getNickName());
        res.put("userType", user.getUserType());
        res.put("mustChangePassword", user.getMustChangePwd() != null && user.getMustChangePwd() == 1);
        return res;
    }

    public Map<String, Object> info() {
        LoginUser user = loginUserService.load(com.laminar.ops.security.AuthContext.userId());
        SysUser db = userMapper.selectById(user.getUserId());
        List<SysRole> roles = userMapper.selectRolesByUserId(user.getUserId());

        Map<String, Object> res = new HashMap<>();
        res.put("user", db);
        res.put("roles", roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList()));
        res.put("roleNames", roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        res.put("permissions", user.getPermissions());
        res.put("menus", menuService.getUserMenuTree(user));
        res.put("dataScope", user.getDataScope());
        res.put("hospitalId", user.getHospitalId());
        res.put("departmentId", user.getDepartmentId());
        res.put("engineerId", user.getEngineerId());
        res.put("companyId", user.getCompanyId());
        res.put("pageSize", db.getPrefPageSize() == null ? 10 : db.getPrefPageSize());
        if (db.getCompanyId() != null) {
            List<String> names = jdbc.queryForList(
                    "select company_name from biz_company where company_id=?", String.class, db.getCompanyId());
            res.put("companyName", names.isEmpty() ? null : names.get(0));
        }
        res.put("mustChangePassword", db.getMustChangePwd() != null && db.getMustChangePwd() == 1);
        return res;
    }

    public void updatePreferences(Long userId, Map<String, Object> body) {
        Object value = body.get("pageSize");
        if (value == null) throw BizException.badRequest("缺少每页条数");
        Integer pageSize;
        try {
            pageSize = Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw BizException.badRequest("每页条数格式不正确");
        }
        if (!List.of(10, 20, 50, 100).contains(pageSize)) {
            throw BizException.badRequest("每页条数只能是 10、20、50、100");
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPrefPageSize(pageSize);
        userMapper.updateById(update);
    }

    public void logout(Long userId) {
        redis.delete("online:" + userId);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        validatePassword(newPassword);
        SysUser user = userMapper.selectById(userId);
        if (user == null || !PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw BizException.badRequest("原密码不正确");
        }
        if (PasswordUtil.matches(newPassword, user.getPassword())) {
            throw BizException.badRequest("新密码不能与原密码相同");
        }
        jdbc.update("update sys_user set password=?, must_change_pwd=0, pwd_update_time=now(), " +
                        "login_fail_count=0, lock_until=null where user_id=?",
                PasswordUtil.encode(newPassword), userId);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 32) {
            throw BizException.badRequest("密码长度需为 8-32 位");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw BizException.badRequest("密码必须同时包含字母和数字");
        }
    }

    private void recordLoginFailure(SysUser user, String username, String ip, String ua) {
        int fails = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
        LocalDateTime lockUntil = fails >= 5 ? LocalDateTime.now().plusMinutes(15) : null;
        jdbc.update("update sys_user set login_fail_count=?, lock_until=? where user_id=?",
                fails, lockUntil, user.getUserId());
        if (lockUntil != null) {
            writeLoginLog(username, ip, ua, "1", "连续失败5次，账号锁定15分钟");
            throw BizException.badRequest("连续登录失败，账号已锁定15分钟");
        }
    }

    private void writeLoginLog(String username, String ip, String ua, String status, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setIp(ip);
        log.setOs(parseOs(ua));
        log.setBrowser(parseBrowser(ua));
        log.setStatus(status);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String parseOs(String ua) {
        if (ua == null) return "未知";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Linux")) return "Linux";
        return "其他";
    }

    private String parseBrowser(String ua) {
        if (ua == null) return "未知";
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/")) return "Safari";
        return "其他";
    }
}
