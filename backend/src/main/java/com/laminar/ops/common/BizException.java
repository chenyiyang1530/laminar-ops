package com.laminar.ops.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(500, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }

    public static BizException unauthorized(String msg) {
        return new BizException(401, msg);
    }

    public static BizException badRequest(String msg) {
        return new BizException(400, msg);
    }
}
