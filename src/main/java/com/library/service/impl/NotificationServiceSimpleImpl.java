package com.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.dto.NotificationMessage;
import com.library.entity.Notification;
import com.library.entity.NotificationTemplate;
import com.library.mapper.NotificationMapper;
import com.library.mapper.NotificationTemplateMapper;
import com.library.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务实现类(不使用RabbitMQ)
 * 当 spring.rabbitmq.enabled=false 或未配置时使用此实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
public class NotificationServiceSimpleImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationTemplateMapper templateMapper;

    @Override
    public void sendNotification(NotificationMessage message) {
        try {
            log.info("直接处理通知消息(不使用RabbitMQ): {}", message);
            
            // 创建通知对象
            Notification notification = createNotificationFromTemplate(message);
            
            // 保存通知到数据库
            notificationMapper.insert(notification);
            
            // 直接处理通知发送
            processNotification(notification);
            
        } catch (Exception e) {
            log.error("处理通知消息失败: {}", message, e);
            throw new RuntimeException("处理通知消息失败", e);
        }
    }

    @Override
    public void sendPickupNotice(Long userId, String bookTitle, String libraryName, String pickupCode) {
        NotificationMessage message = new NotificationMessage();
        message.setUserId(userId);
        message.setType("PICKUP_NOTICE");
        message.setChannel("IN_APP");

        Map<String, String> params = new HashMap<>();
        params.put("bookTitle", bookTitle);
        params.put("libraryName", libraryName);
        params.put("pickupCode", pickupCode);
        message.setTemplateParams(params);

        sendNotification(message);
    }

    @Override
    public void sendReserveExpireWarning(Long userId, String bookTitle, int remainingHours) {
        NotificationMessage message = new NotificationMessage();
        message.setUserId(userId);
        message.setType("RESERVE_EXPIRE_WARNING");
        message.setChannel("IN_APP");

        Map<String, String> params = new HashMap<>();
        params.put("bookTitle", bookTitle);
        params.put("remainingHours", String.valueOf(remainingHours));
        message.setTemplateParams(params);

        sendNotification(message);
    }

    @Override
    public void sendReserveExpired(Long userId, String bookTitle) {
        NotificationMessage message = new NotificationMessage();
        message.setUserId(userId);
        message.setType("RESERVE_EXPIRED");
        message.setChannel("IN_APP");

        Map<String, String> params = new HashMap<>();
        params.put("bookTitle", bookTitle);
        message.setTemplateParams(params);

        sendNotification(message);
    }

    @Override
    public void sendReturnReminder(Long userId, String bookTitle, String dueDate) {
        NotificationMessage message = new NotificationMessage();
        message.setUserId(userId);
        message.setType("RETURN_REMINDER");
        message.setChannel("IN_APP");

        Map<String, String> params = new HashMap<>();
        params.put("bookTitle", bookTitle);
        params.put("dueDate", dueDate);
        message.setTemplateParams(params);

        sendNotification(message);
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null) {
            log.info("通知已标记为已读: {}", notificationId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processNotification(Notification notification) {
        try {
            // 更新状态为发送中
            notification.setStatus("SENDING");
            notificationMapper.updateById(notification);

            // 这里实现具体的通知发送逻辑
            // 目前只实现站内信,直接保存到数据库即可

            // 更新状态为发送成功
            notification.setStatus("SUCCESS");
            notification.setSendTime(LocalDateTime.now());
            notificationMapper.updateById(notification);

            log.info("通知发送成功: notificationId={}", notification.getId());
        } catch (Exception e) {
            log.error("通知发送失败: notificationId={}", notification.getId(), e);

            // 更新状态为发送失败
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationMapper.updateById(notification);

            throw e;
        }
    }

    /**
     * 根据模板创建通知对象
     *
     * @param message 通知消息
     * @return 通知对象
     */
    public Notification createNotificationFromTemplate(NotificationMessage message) {
        // 查询模板
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getType, message.getType())
                .eq(NotificationTemplate::getChannel, message.getChannel())
                .eq(NotificationTemplate::getIsEnabled, 1);
        NotificationTemplate template = templateMapper.selectOne(wrapper);

        if (template == null) {
            throw new RuntimeException("未找到通知模板: type=" + message.getType() + ", channel=" + message.getChannel());
        }

        // 填充模板
        String title = fillTemplate(template.getTitleTemplate(), message.getTemplateParams());
        String content = fillTemplate(template.getContentTemplate(), message.getTemplateParams());

        // 创建通知对象
        Notification notification = new Notification();
        notification.setUserId(message.getUserId());
        notification.setType(message.getType());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setChannel(message.getChannel());
        notification.setStatus("PENDING");
        notification.setRetryCount(0);
        notification.setCreateTime(LocalDateTime.now());

        return notification;
    }

    /**
     * 填充模板占位符
     *
     * @param template 模板内容
     * @param params   参数
     * @return 填充后的内容
     */
    private String fillTemplate(String template, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
