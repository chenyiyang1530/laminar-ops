package com.laminar.ops.security;

import com.laminar.ops.common.BizException;
import com.laminar.ops.system.service.SysConfigService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final LoginUserService loginUserService;
    private final StringRedisTemplate redis;
    private final SysConfigService sysConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (token == null) {
            throw BizException.unauthorized("未登录或登录已过期");
        }
        Long userId;
        try {
            Claims claims = jwtUtil.parse(token);
            userId = Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            throw BizException.unauthorized("登录凭证无效，请重新登录");
        }
        if (Boolean.TRUE.equals(redis.hasKey("kicked:" + userId))) {
            throw BizException.unauthorized("账号已被管理员强制下线");
        }
        if (!Boolean.TRUE.equals(redis.hasKey("online:" + userId))) {
            throw BizException.unauthorized("登录会话已失效，请重新登录");
        }
        LoginUser user = loginUserService.load(userId);
        if (sysConfig.bool("platform.maintenance.enabled", false) && !user.isSuperAdmin()) {
            throw new BizException(503, sysConfig.value("platform.maintenance.message", "平台正在维护，请稍后再试"));
        }
        AuthContext.set(user);
        redis.expire("online:" + userId, Duration.ofMinutes(
                Math.max(1, sysConfig.intValue("sys.session.idleMinutes", 30))));

        String uri = request.getRequestURI();
        if (user.isMustChangePassword()
                && !"/api/auth/password".equals(uri)
                && !"/api/auth/info".equals(uri)
                && !"/api/auth/logout".equals(uri)) {
            throw BizException.forbidden("请先修改初始密码");
        }

        if (handler instanceof HandlerMethod hm) {
            Perm perm = hm.getMethodAnnotation(Perm.class);
            if (perm == null) {
                perm = hm.getBeanType().getAnnotation(Perm.class);
            }
            if (perm != null && !user.hasPerm(perm.value())) {
                throw BizException.forbidden("无权限访问：" + perm.value());
            }
            if (request.getRequestURI().startsWith("/api/portal/") && !user.isHospitalUser()) {
                throw BizException.forbidden("该接口仅限院方账号访问");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        String token = request.getHeader("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        return request.getParameter("token");
    }
}
