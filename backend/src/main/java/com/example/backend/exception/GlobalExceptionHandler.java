package com.example.backend.exception;

import com.example.backend.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务参数异常。
     *
     * @param e 异常信息
     * @return 统一失败响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResponse.fail(resolveMessage(e.getMessage(), "请求参数不合法"));
    }

    /**
     * 处理日期解析异常。
     *
     * @param e 异常信息
     * @return 统一失败响应
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ApiResponse<Void> handleDateTimeParseException(DateTimeParseException e) {
        return ApiResponse.fail("日期格式错误，请使用yyyy-MM-dd");
    }

    /**
     * 处理请求参数类型不匹配异常。
     *
     * @param e 异常信息
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return ApiResponse.fail("请求参数类型错误: " + e.getName());
    }

    /**
     * 处理JSON请求体解析异常。
     *
     * @param e 异常信息
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ApiResponse.fail("请求体格式错误，请检查JSON内容");
    }

    /**
     * 处理方法参数校验异常。
     *
     * @param e 异常信息
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ApiResponse.fail("请求参数校验失败");
    }

    /**
     * 兜底处理未捕获异常。
     *
     * @param e 异常信息
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.fail(resolveMessage(e.getMessage(), "系统异常，请稍后重试"));
    }

    /**
     * 解析异常消息为空时的默认文案。
     *
     * @param message  原始消息
     * @param fallback 默认文案
     * @return 最终文案
     */
    private String resolveMessage(String message, String fallback) {
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }
        return message;
    }
}
