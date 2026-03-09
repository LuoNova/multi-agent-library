package com.library.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.library.entity.BookBorrow;
import com.library.entity.BookCopy;
import com.library.entity.BookBiblio;
import com.library.entity.User;
import com.library.mapper.*;
import com.library.service.NotificationService;
import com.library.service.ReservationFulfillmentService;
import com.library.constant.LibraryConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超期释放定时任务
 * 每小时执行一次,检查并处理超期未取的预留记录
 */
@Slf4j
@Component
public class ReservationExpireTask {

    @Autowired
    private BookBorrowMapper borrowMapper;

    @Autowired
    private BookCopyMapper copyMapper;

    @Autowired
    private BookBiblioMapper biblioMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReservationFulfillmentService fulfillmentService;

    /**
     * 超期释放任务
     * 每小时执行一次: 0 * * * * ?
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void releaseExpiredReservations() {
        log.info("========== 开始执行超期释放任务 ==========");
        LocalDateTime now = LocalDateTime.now();

        try {
            //1.查询超期预留记录
            //条件:status=RESERVED AND pickupDeadline<now
            LambdaQueryWrapper<BookBorrow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BookBorrow::getStatus, LibraryConstants.BORROW_STATUS_RESERVED)
                    .isNotNull(BookBorrow::getPickupDeadline)
                    .lt(BookBorrow::getPickupDeadline, now);

            List<BookBorrow> expiredBorrows = borrowMapper.selectList(wrapper);
            log.info("查询到{}条超期预留记录", expiredBorrows.size());

            if (expiredBorrows.isEmpty()) {
                log.info("没有超期预留记录,任务结束");
                return;
            }

            //2.处理每条超期记录
            int successCount = 0;
            int failCount = 0;

            for (BookBorrow borrow : expiredBorrows) {
                try {
                    processExpiredBorrow(borrow);
                    successCount++;
                } catch (Exception e) {
                    log.error("处理超期记录失败: borrowId={}", borrow.getId(), e);
                    failCount++;
                }
            }

            log.info("超期释放任务完成: 成功{}条, 失败{}条", successCount, failCount);

        } catch (Exception e) {
            log.error("超期释放任务执行失败", e);
            throw e;
        }

        log.info("========== 超期释放任务结束 ==========");
    }

    /**
     * 处理单条超期记录
     *
     * @param borrow 借阅记录
     */
    private void processExpiredBorrow(BookBorrow borrow) {
        log.info("处理超期记录: borrowId={}, userId={}", borrow.getId(), borrow.getUserId());

        //1.更新借阅状态:RESERVED→CANCELLED
        borrow.setStatus(LibraryConstants.BORROW_STATUS_CANCELLED);
        borrowMapper.updateById(borrow);
        log.info("借阅状态已更新: RESERVED → CANCELLED");

        //2.更新图书副本状态:RESERVED→AVAILABLE
        BookCopy copy = copyMapper.selectById(borrow.getCopyId());
        if (copy != null) {
            copy.setStatus(LibraryConstants.COPY_STATUS_AVAILABLE);
            copyMapper.updateById(copy);
            log.info("图书副本状态已更新: RESERVED → AVAILABLE, copyId={}", copy.getId());
        }

        //3.回滚用户借阅计数并扣除信用分
        User user = userMapper.selectById(borrow.getUserId());
        if (user != null) {
            //回滚借阅计数:-1
            if (user.getCurrentBorrowCount() > 0) {
                user.setCurrentBorrowCount(user.getCurrentBorrowCount() - 1);
            }

            //扣除信用分:-5
            int oldCreditScore = user.getCreditScore();
            int newCreditScore = Math.max(0, oldCreditScore - 5);
            user.setCreditScore(newCreditScore);

            //信用分过低冻结账号
            if (newCreditScore < 60) {
                user.setStatus(LibraryConstants.USER_STATUS_FROZEN);
            }

            userMapper.updateById(user);
            log.info("用户计数/信用已更新: userId={}, currentBorrowCount={}, creditScore:{}->{}",
                    user.getId(), user.getCurrentBorrowCount(), oldCreditScore, newCreditScore);
        }

        //4.发送超期通知
        String bookTitle = "未知图书";
        if (copy != null) {
            BookBiblio biblio = biblioMapper.selectById(copy.getBiblioId());
            if (biblio != null) {
                bookTitle = biblio.getTitle();
            }
        }

        notificationService.sendReserveExpired(borrow.getUserId(), bookTitle);
        log.info("超期通知已发送: userId={}, bookTitle={}", borrow.getUserId(), bookTitle);

        //5.自动兑现下一位预约者(如有)
        if (copy != null) {
            ReservationFulfillmentService.FulfillmentResult result = fulfillmentService.fulfillNextForAvailableCopy(copy.getId());
            log.info("预约兑现结果: type={}, reservationId={}, borrowId={}, transferId={}",
                    result.getType(), result.getReservationId(), result.getBorrowId(), result.getTransferId());
        }
    }
}
