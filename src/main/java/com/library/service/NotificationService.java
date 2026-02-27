package com.library.service;

import com.library.dto.NotificationMessage;
import com.library.entity.Notification;

import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 发送通知(异步)
     *
     * @param message 通知消息
     */
    void sendNotification(NotificationMessage message);

    /**
     * 发送取书通知
     *
     * @param userId      用户ID
     * @param bookTitle   图书名称
     * @param libraryName 取书馆名称
     * @param pickupCode  取书码
     */
    void sendPickupNotice(Long userId, String bookTitle, String libraryName, String pickupCode);

    /**
     * 发送预留超期提醒
     *
     * @param userId        用户ID
     * @param bookTitle     图书名称
     * @param remainingHours 剩余小时数
     */
    void sendReserveExpireWarning(Long userId, String bookTitle, int remainingHours);

    /**
     * 发送预约超期通知
     *
     * @param userId    用户ID
     * @param bookTitle 图书名称
     */
    void sendReserveExpired(Long userId, String bookTitle);

    /**
     * 发送还书提醒
     *
     * @param userId    用户ID
     * @param bookTitle 图书名称
     * @param dueDate   应还日期
     */
    void sendReturnReminder(Long userId, String bookTitle, String dueDate);

    /**
     * 获取用户通知列表
     *
     * @param userId 用户ID
     * @return 通知列表
     */
    List<Notification> getUserNotifications(Long userId);

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     */
    void markAsRead(Long notificationId);

    /**
     * 处理通知发送(消费者调用)
     *
     * @param notification 通知对象
     */
    void processNotification(Notification notification);
}
