package com.library.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务异常类
 * 用于业务逻辑中抛出的可预期异常
 */
@Setter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    @Getter
    private Integer code;

    /**
     * 错误信息
     */
    private String message;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
