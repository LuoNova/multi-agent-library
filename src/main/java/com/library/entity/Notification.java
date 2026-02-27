package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知记录实体类
 */
@Data
@TableName("tb_notification")
public class Notification {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 通知类型
     * PICKUP_NOTICE-取书通知
     * RESERVE_EXPIRE_WARNING-预留超期提醒
     * RESERVE_EXPIRED-预约超期通知
     * RETURN_REMINDER-还书提醒
     */
    private String type;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知渠道
     * IN_APP-站内信
     * SMS-短信
     * EMAIL-邮件
     */
    private String channel;

    /**
     * 发送状态
     * PENDING-待发送
     * SENDING-发送中
     * SUCCESS-发送成功
     * FAILED-发送失败
     */
    private String status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
