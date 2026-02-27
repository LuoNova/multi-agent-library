package com.library.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一响应结果
 */
@Data
@Schema(description = "统一响应结果")
public class Result<T> {

    @Schema(description = "状态码")
    private Integer code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    /**
     * 成功响应
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功响应(带自定义消息)
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应(带错误码)
     */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
