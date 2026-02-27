package com.library.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 通知消息DTO
 */
@Data
public class NotificationMessage implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 通知类型
     */
    private String type;

    /**
     * 通知渠道
     */
    private String channel;

    /**
     * 模板参数(用于填充模板占位符)
     */
    private Map<String, String> templateParams;

    /**
     * 业务ID(可选,用于关联业务数据)
     */
    private Long businessId;
}
