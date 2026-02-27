package com.library.service.impl;

import com.library.dto.PickupConfirmRequest;
import com.library.dto.PickupConfirmResponse;
import com.library.entity.*;
import com.library.mapper.*;
import com.library.service.NotificationService;
import com.library.exception.BusinessException;
import com.library.service.PickupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 取书确认服务实现类
 */
@Slf4j
@Service
public class PickupServiceImpl implements PickupService {

    @Autowired
    private BookBorrowMapper borrowMapper;

    @Autowired
    private BookCopyMapper copyMapper;

    @Autowired
    private BookBiblioMapper biblioMapper;

    @Autowired
    private LibraryMapper libraryMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PickupConfirmResponse confirmPickup(PickupConfirmRequest request) {
        log.info("开始处理取书确认: borrowId={}, userId={}", request.getBorrowId(), request.getUserId());

        // 1. 查询借阅记录
        BookBorrow borrow = borrowMapper.selectById(request.getBorrowId());
        if (borrow == null) {
            throw new BusinessException("借阅记录不存在");
        }

        // 2. 验证用户身份(仅限预约人本人取书)
        if (!borrow.getUserId().equals(request.getUserId())) {
            throw new BusinessException("仅限预约人本人取书");
        }

        // 3. 检查借阅状态(必须为RESERVED)
        if (!"RESERVED".equals(borrow.getStatus())) {
            throw new BusinessException("借阅状态异常,当前状态: " + borrow.getStatus());
        }

        // 4. 检查预留时间(必须在24小时内)
        LocalDateTime now = LocalDateTime.now();
        if (borrow.getPickupDeadline() != null && now.isAfter(borrow.getPickupDeadline())) {
            throw new BusinessException("预留已超期,请重新预约");
        }

        // 5. 更新借阅状态: RESERVED → BORROWING
        borrow.setStatus("BORROWING");
        borrow.setActualPickupTime(now);
        borrow.setBorrowTime(now);

        // 计算应还日期(假设借阅期限为30天)
        borrow.setDueTime(now.plusDays(30));

        borrowMapper.updateById(borrow);
        log.info("借阅状态已更新: RESERVED → BORROWING");

        // 6. 更新图书副本状态: RESERVED → BORROWED
        BookCopy copy = copyMapper.selectById(borrow.getCopyId());
        if (copy != null) {
            copy.setStatus("BORROWED");
            copy.setLastBorrowTime(now);
            copy.setLocalBorrowCount(copy.getLocalBorrowCount() + 1);
            copyMapper.updateById(copy);
            log.info("图书副本状态已更新: RESERVED → BORROWED");
        }

        // 7. 构建响应
        PickupConfirmResponse response = new PickupConfirmResponse();
        response.setBorrowId(borrow.getId());
        response.setBorrowDate(borrow.getBorrowTime());
        response.setDueDate(borrow.getDueTime());

        // 获取图书名称
        if (copy != null) {
            BookBiblio biblio = biblioMapper.selectById(copy.getBiblioId());
            if (biblio != null) {
                response.setBookTitle(biblio.getTitle());
            }
        }

        // 获取取书馆名称
        if (borrow.getPickupLibraryId() != null) {
            Library library = libraryMapper.selectById(borrow.getPickupLibraryId());
            if (library != null) {
                response.setLibraryName(library.getName());
            }
        }

        log.info("取书确认成功: borrowId={}", borrow.getId());

        // 8. 发送取书成功通知(可选)
        // notificationService.sendPickupSuccessNotice(borrow.getUserId(), response.getBookTitle());

        return response;
    }
}
