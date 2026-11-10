package com.laminar.ops.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 操作审计：记录所有非 GET 的业务写操作（登录/登出除外）。
 */
@Component
@RequiredArgsConstructor
public class OperLogInterceptor implements HandlerInterceptor {

    private final JdbcTemplate jdbc;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("operStart", System.currentTimeMillis());
        request.setAttribute("operUser", AuthContext.get());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) return;
        LoginUser user = (LoginUser) request.getAttribute("operUser");
        long cost = System.currentTimeMillis() - ((Long) request.getAttribute("operStart") == null
                ? System.currentTimeMillis() : (Long) request.getAttribute("operStart"));
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        Object bizCode = request.getAttribute("bizErrorCode");
        String bizMsg = (String) request.getAttribute("bizErrorMsg");
        boolean failed = ex != null || bizCode != null;
        String errorMsg = ex != null ? truncate(ex.getMessage()) : truncate(bizMsg);
        try {
            jdbc.update("insert into sys_oper_log(title,business_type,method,request_method,oper_name,oper_url,oper_ip," +
                            "oper_param,status,error_msg,cost_time,oper_time) values(?,?,?,?,?,?,?,?,?,?,?,now())",
                    request.getRequestURI(), method, handler.toString(), method,
                    user == null ? "匿名" : user.getNickName(), request.getRequestURI(), ip,
                    request.getQueryString() == null ? "" : request.getQueryString(),
                    failed ? "1" : "0", errorMsg, cost);
        } catch (Exception ignored) {
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
