package com.laminar.ops.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e, HttpServletRequest request) {
        markError(request, e.getCode(), e.getMessage());
        if (e.getCode() >= 500) {
            log.warn("业务异常: {}", e.getMessage());
        }
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e, HttpServletRequest request) {
        FieldError error = e instanceof MethodArgumentNotValidException m
                ? m.getBindingResult().getFieldError()
                : ((BindException) e).getBindingResult().getFieldError();
        String msg = error == null ? "参数校验失败" : error.getDefaultMessage();
        markError(request, 400, msg);
        return Result.fail(400, msg);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException e, HttpServletRequest request) {
        String msg = "数据已存在，请检查编码或名称是否重复";
        markError(request, 400, msg);
        log.warn("唯一键冲突: {}", e.getMessage());
        return Result.fail(400, msg);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        String msg = "数据关联校验失败，请检查关联数据或必填项";
        markError(request, 400, msg);
        log.warn("数据完整性异常: {}", e.getMessage());
        return Result.fail(400, msg);
    }

    /** 静态资源不存在（如浏览器自动请求 /favicon.ico），属正常 404，不按系统异常记录 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        log.debug("资源不存在: {}", e.getResourcePath());
        return Result.fail(404, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        markError(request, 500, e.getMessage());
        return Result.fail(500, "系统异常：" + e.getMessage());
    }

    private void markError(HttpServletRequest request, int code, String msg) {
        if (request != null) {
            request.setAttribute("bizErrorCode", code);
            request.setAttribute("bizErrorMsg", msg);
        }
    }
}
