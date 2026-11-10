package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final String SCENE = "bind";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    @Value("${app.verification.dev-mode:true}")
    private boolean devMode;

    @Value("${app.verification.code-ttl-seconds:300}")
    private int ttlSeconds;

    @Value("${app.verification.cooldown-seconds:60}")
    private int cooldownSeconds;

    public Map<String, Object> send(Long userId, String type, String requestedTarget) {
        String targetType = normalizeType(type);
        Map<String, Object> user = currentContact(userId, targetType);
        if ("1".equals(String.valueOf(user.get("verified")))) {
            throw BizException.badRequest("该联系方式已经验证，如需更换请联系管理员");
        }
        String current = user.get("target") == null ? null : String.valueOf(user.get("target"));
        String target = normalizeTarget(targetType, requestedTarget == null || requestedTarget.isBlank() ? current : requestedTarget);
        if (target == null) throw BizException.badRequest("请输入" + label(targetType));
        if (!target.equals(current)) validateTargetAvailable(userId, targetType, target);
        String cooldownKey = "verify:cooldown:" + targetType + ":" + target;
        Boolean acquired = redis.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds));
        if (!Boolean.TRUE.equals(acquired)) throw BizException.badRequest("验证码发送过于频繁，请稍后再试");

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        jdbc.update("insert into sys_verification_code(target_type,target,scene,code_hash,expires_at,attempts,used,create_time) " +
                        "values(?,?,?,?,?,0,0,now())",
                targetType, target, SCENE, PasswordUtil.encode(code),
                Timestamp.valueOf(LocalDateTime.now().plusSeconds(ttlSeconds)));
        log.info("verification code targetType={} target={} code={} (dev-mode={})", targetType, mask(target), code, devMode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expiresIn", ttlSeconds);
        result.put("devMode", devMode);
        if (devMode) result.put("debugCode", code);
        return result;
    }

    @Transactional
    public void confirm(Long userId, String type, String code, String requestedTarget) {
        String targetType = normalizeType(type);
        if (code == null || !code.matches("\\d{6}")) throw BizException.badRequest("请输入6位验证码");
        Map<String, Object> user = currentContact(userId, targetType);
        if ("1".equals(String.valueOf(user.get("verified")))) {
            throw BizException.badRequest("该联系方式已经验证，如需更换请联系管理员");
        }
        String current = user.get("target") == null ? null : String.valueOf(user.get("target"));
        String target = normalizeTarget(targetType, requestedTarget == null || requestedTarget.isBlank() ? current : requestedTarget);
        if (target == null) throw BizException.badRequest("请输入" + label(targetType));
        if (!target.equals(current)) validateTargetAvailable(userId, targetType, target);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select verification_id as `id`, code_hash as `codeHash`, expires_at as `expiresAt`, attempts, used " +
                        "from sys_verification_code where target_type=? and target=? and scene=? and used=0 " +
                        "order by verification_id desc limit 1", targetType, target, SCENE);
        if (rows.isEmpty()) throw BizException.badRequest("验证码不存在，请重新发送");
        Map<String, Object> row = rows.get(0);
        long id = ((Number) row.get("id")).longValue();
        int attempts = row.get("attempts") == null ? 0 : ((Number) row.get("attempts")).intValue();
        if (attempts >= 5) throw BizException.badRequest("验证码错误次数过多，请重新发送");
        Object expiresValue = row.get("expiresAt");
        LocalDateTime expiresAt = expiresValue instanceof Timestamp ts
                ? ts.toLocalDateTime()
                : (LocalDateTime) expiresValue;
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            jdbc.update("update sys_verification_code set used=1 where verification_id=?", id);
            throw BizException.badRequest("验证码已过期，请重新发送");
        }
        if (!PasswordUtil.matches(code, String.valueOf(row.get("codeHash")))) {
            int next = attempts + 1;
            jdbc.update("update sys_verification_code set attempts=?, used=? where verification_id=?",
                    next, next >= 5 ? 1 : 0, id);
            throw BizException.badRequest(next >= 5 ? "验证码错误次数过多，请重新发送" : "验证码错误");
        }
        jdbc.update("update sys_verification_code set used=1 where verification_id=?", id);
        if ("phone".equals(targetType)) {
            jdbc.update("update sys_user set phone=?, phone_verified=1, phone_verified_at=now() where user_id=?",
                    target, userId);
        } else {
            jdbc.update("update sys_user set email=?, email_verified=1, email_verified_at=now() where user_id=?",
                    target, userId);
        }
    }

    private Map<String, Object> currentContact(Long userId, String type) {
        String column = "phone".equals(type) ? "phone" : "email";
        String verifiedColumn = "phone".equals(type) ? "phone_verified" : "email_verified";
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select " + column + " as `target`, " + verifiedColumn + " as `verified` from sys_user where user_id=?",
                userId);
        if (rows.isEmpty()) throw BizException.badRequest("用户不存在");
        return rows.get(0);
    }

    private String normalizeTarget(String type, String target) {
        if (target == null || target.isBlank()) return null;
        String value = target.trim();
        if ("phone".equals(type)) {
            if (!value.matches("^\\+?\\d{6,20}$")) throw BizException.badRequest("手机号格式不正确");
        } else {
            if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw BizException.badRequest("邮箱格式不正确");
            value = value.toLowerCase();
        }
        return value;
    }

    private void validateTargetAvailable(Long userId, String type, String target) {
        Integer count = "phone".equals(type)
                ? jdbc.queryForObject("select count(*) from sys_user where phone=? and user_id<>?",
                Integer.class, target, userId)
                : jdbc.queryForObject("select count(*) from sys_user where lower(email)=? and user_id<>?",
                Integer.class, target, userId);
        if (count != null && count > 0) {
            throw BizException.badRequest(("phone".equals(type) ? "手机号" : "邮箱") + "已被其他账号使用");
        }
    }

    private String label(String type) {
        return "phone".equals(type) ? "手机号" : "邮箱";
    }

    private String normalizeType(String type) {
        if (type == null) throw BizException.badRequest("缺少验证类型");
        String value = type.trim().toLowerCase();
        if (!"phone".equals(value) && !"email".equals(value)) {
            throw BizException.badRequest("不支持的验证类型");
        }
        return value;
    }

    private String mask(String target) {
        if (target == null || target.length() < 4) return target;
        return target.substring(0, 2) + "****" + target.substring(target.length() - 2);
    }
}
