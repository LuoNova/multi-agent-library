package com.library.consumer;

import com.library.dto.NotificationMessage;
import com.library.entity.Notification;
import com.library.mapper.NotificationMapper;
import com.library.service.impl.NotificationServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 通知消息消费者
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class NotificationConsumer {

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 消费通知消息
     *
     * @param message 通知消息
     * @param channel RabbitMQ通道
     * @param tag     消息标签
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.NOTIFICATION_QUEUE}")
    public void handleNotification(
            NotificationMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("收到通知消息: {}", message);

            // 创建通知对象
            Notification notification = notificationService.createNotificationFromTemplate(message);

            // 保存通知到数据库
            notificationMapper.insert(notification);

            // 处理通知发送
            notificationService.processNotification(notification);

            // 手动确认消息
            channel.basicAck(tag, false);
            log.info("通知消息处理成功: notificationId={}", notification.getId());

        } catch (Exception e) {
            log.error("处理通知消息失败", e);
            try {
                // 消息处理失败,拒绝消息并重新入队
                // 第一个参数: 消息标签
                // 第二个参数: 是否批量拒绝
                // 第三个参数: 是否重新入队
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("拒绝消息失败", ex);
            }
        }
    }
}
