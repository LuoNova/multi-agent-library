package com.library.service;

import com.library.entity.BookTransfer;
import com.library.entity.Notification;
import com.library.mapper.BookBiblioMapper;
import com.library.mapper.BookTransferMapper;
import com.library.mapper.LibraryMapper;
import com.library.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//调拨通知服务
@Slf4j
@Service
public class TransferNotificationService {

    @Autowired
    private NotificationMapper notificationMapper;
    
    @Autowired
    private BookTransferMapper transferMapper;
    
    @Autowired
    private BookBiblioMapper biblioMapper;
    
    @Autowired
    private LibraryMapper libraryMapper;

    //发送调拨发起通知
    public void sendTransferInitiatedNotification(Long transferId, Long userId) {
        log.info("发送调拨发起通知: transferId={}, userId={}", transferId, userId);
        
        BookTransfer transfer = transferMapper.selectById(transferId);
        if (transfer == null) {
            log.warn("调拨记录不存在,无法发送通知: transferId={}", transferId);
            return;
        }
        
        //只对用户请求调拨发送通知,库存平衡调拨不发送
        if (!"USER_REQUEST".equals(transfer.getTransferReason())) {
            log.info("库存平衡调拨,不发送通知: transferId={}", transferId);
            return;
        }
        
        //构建通知内容
        String bookTitle = getBookTitle(transfer.getCopyId());
        String fromLibraryName = getLibraryName(transfer.getFromLibraryId());
        String toLibraryName = getLibraryName(transfer.getToLibraryId());
        String estimatedTime = formatTime(transfer.getEstimatedArrivalTime());
        
        String title = "图书调拨通知";
        String content = String.format(
            "您好,图书《%s》已从%s发出,正在调拨至%s,预计%s到达,请留意查收。",
            bookTitle, fromLibraryName, toLibraryName, estimatedTime
        );
        
        //创建通知
        createNotification(userId, "TRANSFER_INITIATED", title, content);
        
        log.info("调拨发起通知发送成功: userId={}, transferId={}", userId, transferId);
    }

    //发送调拨到达通知
    public void sendTransferArrivedNotification(Long transferId, Long userId) {
        log.info("发送调拨到达通知: transferId={}, userId={}", transferId, userId);
        
        BookTransfer transfer = transferMapper.selectById(transferId);
        if (transfer == null) {
            log.warn("调拨记录不存在,无法发送通知: transferId={}", transferId);
            return;
        }
        
        //只对用户请求调拨发送通知,库存平衡调拨不发送
        if (!"USER_REQUEST".equals(transfer.getTransferReason())) {
            log.info("库存平衡调拨,不发送通知: transferId={}", transferId);
            return;
        }
        
        //构建通知内容
        String bookTitle = getBookTitle(transfer.getCopyId());
        String toLibraryName = getLibraryName(transfer.getToLibraryId());
        String actualTime = formatTime(transfer.getActualArrivalTime());
        
        String title = "图书到达通知";
        String content = String.format(
            "您好,您调拨的图书《%s》已于%s到达%s,请及时处理。",
            bookTitle, actualTime, toLibraryName
        );
        
        //创建通知
        createNotification(userId, "TRANSFER_ARRIVED", title, content);
        
        log.info("调拨到达通知发送成功: userId={}, transferId={}", userId, transferId);
    }

    //发送调拨延迟告警(控制台打印)
    public void sendTransferDelayAlert(BookTransfer transfer) {
        log.warn("=== 调拨延迟告警 ===");
        log.warn("调拨ID: {}", transfer.getId());
        log.warn("副本ID: {}", transfer.getCopyId());
        log.warn("图书: {}", getBookTitle(transfer.getCopyId()));
        log.warn("源馆: {}", getLibraryName(transfer.getFromLibraryId()));
        log.warn("目标馆: {}", getLibraryName(transfer.getToLibraryId()));
        log.warn("预计到达时间: {}", formatTime(transfer.getEstimatedArrivalTime()));
        log.warn("已超时: {} 小时", calculateDelayHours(transfer));
        log.warn("请及时跟进处理!");
        log.warn("===================");
    }

    //创建通知记录
    private void createNotification(Long userId, String type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setChannel("IN_APP");
        notification.setStatus("SENT");
        notification.setSendTime(LocalDateTime.now());
        
        notificationMapper.insert(notification);
    }

    //获取图书标题
    private String getBookTitle(Long copyId) {
        //TODO: 根据copyId查询书目信息
        return "未知图书";
    }

    //获取馆名称
    private String getLibraryName(Long libraryId) {
        if (libraryId == null) {
            return "未知馆";
        }
        var library = libraryMapper.selectById(libraryId);
        return library != null ? library.getName() : "未知馆";
    }

    //格式化时间
    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "未知";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return time.format(formatter);
    }

    //计算延迟小时数
    private long calculateDelayHours(BookTransfer transfer) {
        if (transfer.getEstimatedArrivalTime() == null) {
            return 0;
        }
        return java.time.Duration.between(
            transfer.getEstimatedArrivalTime(),
            LocalDateTime.now()
        ).toHours();
    }
}
