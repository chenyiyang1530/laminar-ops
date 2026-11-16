package com.laminar.ops.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laminar.ops.biz.entity.BizHospitalUser;
import com.laminar.ops.biz.mapper.BizHospitalUserMapper;
import com.laminar.ops.biz.service.CompanyCodeService;
import com.laminar.ops.common.BizException;
import com.laminar.ops.common.ClientInfoUtil;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final SysConfigService sysConfig;

    public Map<String, Object> login(String companyCode, String username, String password,
                                     String captchaUuid, String captchaCode, HttpServletRequest request) {
        validateCaptcha(captchaUuid, captchaCode);
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
            throw BizException.badRequest("连续登录失败，账号已锁定" + lockMinutes() + "分钟");
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
        int expireDays = sysConfig.intValue("sys.user.password.expireDays", 0);
        if (expireDays > 0 && (user.getPwdUpdateTime() == null
                || user.getPwdUpdateTime().isBefore(LocalDateTime.now().minusDays(expireDays)))) {
            jdbc.update("update sys_user set must_change_pwd=1 where user_id=?", user.getUserId());
            user.setMustChangePwd(1);
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
        if (sysConfig.bool("platform.maintenance.enabled", false)
                && userMapper.selectRolesByUserId(user.getUserId()).stream()
                .noneMatch(role -> "super_admin".equals(role.getRoleKey()))) {
            String message = sysConfig.value("platform.maintenance.message", "平台正在维护，请稍后再试");
            writeLoginLog(account, ip, ua, "1", "维护模式");
            throw new BizException(503, message);
        }
        String token = jwtUtil.createToken(user.getUserId(), user.getUsername());

        jdbc.update("update sys_user set login_ip=?, login_date=now(), login_fail_count=0, lock_until=null where user_id=?",
                ip, user.getUserId());
        writeLoginLog(account, ip, ua, "0", "登录成功");
        redis.delete("kicked:" + user.getUserId());
        redis.opsForValue().set("online:" + user.getUserId(),
                user.getUsername() + "|" + user.getNickName() + "|" + ip + "|" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "|" +
                        ClientInfoUtil.location(ip) + "|" + ClientInfoUtil.browser(ua) + "|" + ClientInfoUtil.os(ua),
                onlineTtl());

        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("expiresIn", jwtUtil.getExpireSeconds());
        res.put("username", user.getUsername());
        res.put("nickName", user.getNickName());
        res.put("userType", user.getUserType());
        res.put("mustChangePassword", user.getMustChangePwd() != null && user.getMustChangePwd() == 1);
        return res;
    }

    public Map<String, Object> captcha() {
        Map<String, Object> result = new HashMap<>();
        boolean enabled = sysConfig.bool("sys.account.captchaEnabled", false);
        result.put("enabled", enabled);
        if (!enabled) return result;
        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) code.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("captcha:" + uuid, code.toString(), Duration.ofMinutes(2));
        result.put("uuid", uuid);
        result.put("img", renderCaptcha(code.toString()));
        return result;
    }

    private void validateCaptcha(String uuid, String code) {
        if (!sysConfig.bool("sys.account.captchaEnabled", false)) return;
        if (uuid == null || uuid.isBlank()) throw BizException.badRequest("验证码已过期，请刷新后重试");
        String key = "captcha:" + uuid;
        String expected = redis.opsForValue().get(key);
        redis.delete(key);
        if (expected == null) throw BizException.badRequest("验证码已过期，请刷新后重试");
        if (code == null || !expected.equalsIgnoreCase(code.trim())) {
            throw BizException.badRequest("验证码错误");
        }
    }

    private String renderCaptcha(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(242, 247, 252));
            g.fillRect(0, 0, width, height);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            for (int i = 0; i < 4; i++) {
                g.setColor(new Color(20 + SECURE_RANDOM.nextInt(80), 60 + SECURE_RANDOM.nextInt(100),
                        100 + SECURE_RANDOM.nextInt(100)));
                g.drawString(String.valueOf(code.charAt(i)), 14 + i * 26, 30);
            }
            g.setColor(new Color(120, 160, 200, 120));
            for (int i = 0; i < 5; i++) {
                g.drawLine(SECURE_RANDOM.nextInt(width), SECURE_RANDOM.nextInt(height),
                        SECURE_RANDOM.nextInt(width), SECURE_RANDOM.nextInt(height));
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码生成失败", e);
        }
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        res.put("pageSize", db.getPrefPageSize() == null
                ? Math.max(1, sysConfig.intValue("platform.defaultPageSize", 10))
                : db.getPrefPageSize());
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
        int minLength = Math.max(6, Math.min(32, sysConfig.intValue("sys.user.password.minLength", 8)));
        if (password == null || password.length() < minLength || password.length() > 32) {
            throw BizException.badRequest("密码长度需为 " + minLength + "-32 位");
        }
        boolean letter = sysConfig.bool("sys.user.password.requireLetter", true);
        boolean number = sysConfig.bool("sys.user.password.requireNumber", true);
        boolean special = sysConfig.bool("sys.user.password.requireSpecial", false);
        if (letter && !password.matches(".*[A-Za-z].*")) throw BizException.badRequest("密码必须包含字母");
        if (number && !password.matches(".*\\d.*")) throw BizException.badRequest("密码必须包含数字");
        if (special && !password.matches(".*[^A-Za-z0-9].*")) throw BizException.badRequest("密码必须包含特殊字符");
    }

    private int lockMinutes() {
        return Math.max(1, sysConfig.intValue("sys.user.login.lockMinutes", 15));
    }

    private Duration onlineTtl() {
        return Duration.ofMinutes(Math.max(1, sysConfig.intValue("sys.session.idleMinutes", 30)));
    }

    private void recordLoginFailure(SysUser user, String username, String ip, String ua) {
        int maxFail = sysConfig.intValue("sys.user.login.maxFail", 5);
        int fails = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
        LocalDateTime lockUntil = maxFail > 0 && fails >= maxFail
                ? LocalDateTime.now().plusMinutes(lockMinutes()) : null;
        jdbc.update("update sys_user set login_fail_count=?, lock_until=? where user_id=?",
                fails, lockUntil, user.getUserId());
        if (lockUntil != null) {
            writeLoginLog(username, ip, ua, "1", "连续失败" + maxFail + "次，账号锁定" + lockMinutes() + "分钟");
            throw BizException.badRequest("连续登录失败，账号已锁定" + lockMinutes() + "分钟");
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
