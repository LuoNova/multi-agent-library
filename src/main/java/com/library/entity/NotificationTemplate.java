package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知模板实体类
 */
@Data
@TableName("tb_notification_template")
public class NotificationTemplate {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知类型
     * PICKUP_NOTICE-取书通知
     * RESERVE_EXPIRE_WARNING-预留超期提醒
     * RESERVE_EXPIRED-预约超期通知
     * RETURN_REMINDER-还书提醒
     */
    private String type;

    /**
     * 通知渠道
     * IN_APP-站内信
     * SMS-短信
     * EMAIL-邮件
     */
    private String channel;

    /**
     * 标题模板
     */
    private String titleTemplate;

    /**
     * 内容模板(支持占位符,如{bookTitle}, {libraryName}等)
     */
    private String contentTemplate;

    /**
     * 是否启用: 1-启用, 0-禁用
     */
    private Integer isEnabled;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
