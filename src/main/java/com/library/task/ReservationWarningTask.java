package com.library.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.entity.BookBorrow;
import com.library.entity.BookCopy;
import com.library.entity.BookBiblio;
import com.library.mapper.*;
import com.library.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 预留超期提前提醒定时任务
 * 每小时执行一次,检查预留时间在20-21小时的记录,发送提醒通知
 */
@Slf4j
@Component
public class ReservationWarningTask {

    @Autowired
    private BookBorrowMapper borrowMapper;

    @Autowired
    private BookCopyMapper copyMapper;

    @Autowired
    private BookBiblioMapper biblioMapper;

    @Autowired
    private NotificationService notificationService;

    /**
     * 预留超期提前提醒任务
     * 每小时执行一次: 0 0 * * * ?
     * 提前4小时提醒(预留20小时时发送)
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void sendReservationWarning() {
        log.info("========== 开始执行预留超期提前提醒任务 ==========");
        LocalDateTime now = LocalDateTime.now();

        try {
            // 1. 查询即将超期的预留记录
            // 条件: status = RESERVED AND pickupDeadline 在 now+4小时 到 now+5小时 之间
            // 即预留时间在19-20小时之间(提前4-5小时提醒)
            LocalDateTime deadlineStart = now.plusHours(4);
            LocalDateTime deadlineEnd = now.plusHours(5);

            LambdaQueryWrapper<BookBorrow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BookBorrow::getStatus, "RESERVED")
                    .between(BookBorrow::getPickupDeadline, deadlineStart, deadlineEnd);

            List<BookBorrow> warningBorrows = borrowMapper.selectList(wrapper);
            log.info("查询到{}条即将超期的预留记录", warningBorrows.size());

            if (warningBorrows.isEmpty()) {
                log.info("没有即将超期的预留记录,任务结束");
                return;
            }

            // 2. 发送提醒通知
            int successCount = 0;
            int failCount = 0;

            for (BookBorrow borrow : warningBorrows) {
                try {
                    sendWarningNotification(borrow);
                    successCount++;
                } catch (Exception e) {
                    log.error("发送提醒通知失败: borrowId={}", borrow.getId(), e);
                    failCount++;
                }
            }

            log.info("预留超期提前提醒任务完成: 成功{}条, 失败{}条", successCount, failCount);

        } catch (Exception e) {
            log.error("预留超期提前提醒任务执行失败", e);
        }

        log.info("========== 预留超期提前提醒任务结束 ==========");
    }

    /**
     * 发送提醒通知
     *
     * @param borrow 借阅记录
     */
    private void sendWarningNotification(BookBorrow borrow) {
        log.info("发送预留超期提醒: borrowId={}, userId={}", borrow.getId(), borrow.getUserId());

        // 计算剩余小时数
        long remainingHours = ChronoUnit.HOURS.between(LocalDateTime.now(), borrow.getPickupDeadline());
        int remainingHoursInt = (int) Math.max(0, remainingHours);

        // 获取图书名称
        String bookTitle = "未知图书";
        BookCopy copy = copyMapper.selectById(borrow.getCopyId());
        if (copy != null) {
            BookBiblio biblio = biblioMapper.selectById(copy.getBiblioId());
            if (biblio != null) {
                bookTitle = biblio.getTitle();
            }
        }

        // 发送提醒通知
        notificationService.sendReserveExpireWarning(borrow.getUserId(), bookTitle, remainingHoursInt);
        log.info("预留超期提醒已发送: userId={}, bookTitle={}, remainingHours={}",
                borrow.getUserId(), bookTitle, remainingHoursInt);
    }
}
