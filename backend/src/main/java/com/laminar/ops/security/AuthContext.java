package com.laminar.ops.security;

import com.laminar.ops.common.BizException;

public class AuthContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("登录已失效，请重新登录");
        }
        return user;
    }

    public static Long userId() {
        return require().getUserId();
    }

    public static String nickName() {
        return require().getNickName();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
