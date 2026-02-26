package com.library.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

//异常处理工具类，统一处理系统中的异常
@Slf4j
@Component
public class ExceptionHandler {

    //处理业务异常，返回友好的错误信息
    public Map<String, Object> handleBusinessException(String operation, Exception e) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("operation", operation);
        error.put("errorMessage", getFriendlyErrorMessage(e));
        error.put("errorType", e.getClass().getSimpleName());

        log.error("业务操作失败: {}, 错误类型: {}, 错误信息: {}",
                operation, e.getClass().getSimpleName(), e.getMessage(), e);

        return error;
    }

    //处理Agent通信异常，返回友好的错误信息
    public Map<String, Object> handleAgentException(String taskId, Exception e) {
        Map<String, Object> error = new HashMap<>();
        error.put("taskId", taskId);
        error.put("status", "FAILED");
        error.put("message", getFriendlyErrorMessage(e));
        error.put("errorType", e.getClass().getSimpleName());

        log.error("Agent任务失败: taskId={}, 错误类型: {}, 错误信息: {}",
                taskId, e.getClass().getSimpleName(), e.getMessage(), e);

        return error;
    }

    //获取友好的错误信息
    private String getFriendlyErrorMessage(Exception e) {
        //根据异常类型返回友好的错误信息
        if (e instanceof IllegalArgumentException) {
            return "参数错误: " + e.getMessage();
        } else if (e instanceof IllegalStateException) {
            return "状态错误: " + e.getMessage();
        } else if (e instanceof RetryTemplate.RetryExhaustedException) {
            return "操作重试失败，请稍后重试";
        } else if (e instanceof InterruptedException) {
            return "操作被中断";
        } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            return e.getMessage();
        } else {
            return "系统内部错误，请联系管理员";
        }
    }

    //包装异常，添加上下文信息
    public RuntimeException wrapException(String context, Exception e) {
        String message = String.format("%s: %s", context, e.getMessage());
        log.error("包装异常: {}", message, e);

        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(message, e);
        }
    }

    //检查异常是否可重试
    public boolean isRetryable(Exception e) {
        //以下异常类型不建议重试
        if (e instanceof IllegalArgumentException ||
            e instanceof IllegalStateException) {
            return false;
        }

        //以下异常类型建议重试
        return true;
    }
}
