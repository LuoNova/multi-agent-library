package com.library.service;

import com.library.constant.LibraryConstants;
import com.library.entity.BookBorrow;
import com.library.entity.BookCopy;
import com.library.mapper.BookBorrowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

//借阅服务（完整版）
@Service
public class BookBorrowService {

    @Autowired
    private BookBorrowMapper bookBorrowMapper;

    @Autowired
    private BookCopyService bookCopyService;

    //检查用户是否已借阅该书
    public boolean hasBorrowedBiblio(Long userId, Long biblioId) {
        return bookBorrowMapper.countUnreturnedByUserAndBiblio(userId, biblioId) > 0;
    }

    //直接借书（本馆有库存时调用）
    @Transactional
    public boolean directBorrow(Long userId, Long copyId, Long biblioId) {
        BookCopy copy = bookCopyService.getById(copyId);
        if (copy == null || !LibraryConstants.COPY_STATUS_AVAILABLE.equals(copy.getStatus())) {
            return false;
        }

        //创建借阅记录（状态为BORROWING）
        BookBorrow borrow = new BookBorrow();
        borrow.setCopyId(copyId);
        borrow.setUserId(userId);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setDueTime(LocalDateTime.now().plusDays(LibraryConstants.BORROW_DAYS));
        borrow.setStatus(LibraryConstants.BORROW_STATUS_BORROWING);
        bookBorrowMapper.insert(borrow);

        //更新副本状态为已借出，并增加本馆借阅计数
        copy.setStatus(LibraryConstants.COPY_STATUS_BORROWED);
        copy.setLastBorrowTime(LocalDateTime.now());
        copy.setLocalBorrowCount(copy.getLocalBorrowCount() + 1);  //修正：使用localBorrowCount
        bookCopyService.updateCopy(copy);

        return true;
    }

    //调拨完成后预留（创建借阅记录但状态为RESERVED，24小时内有效）
    @Transactional
    public boolean reserveAfterTransfer(Long userId, Long copyId) {
        BookCopy copy = bookCopyService.getById(copyId);
        if (copy == null) {
            return false;
        }

        //创建借阅记录（状态为RESERVED）
        BookBorrow borrow = new BookBorrow();
        borrow.setCopyId(copyId);
        borrow.setUserId(userId);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setDueTime(LocalDateTime.now().plusHours(LibraryConstants.RESERVE_HOURS));
        borrow.setStatus(LibraryConstants.BORROW_STATUS_RESERVED);
        bookBorrowMapper.insert(borrow);

        //更新副本状态为预留
        copy.setStatus(LibraryConstants.COPY_STATUS_RESERVED);
        bookCopyService.updateCopy(copy);

        return true;
    }

    //用户实际取书（将RESERVED改为BORROWING）
    @Transactional
    public boolean confirmPickup(Long borrowId) {
        BookBorrow borrow = bookBorrowMapper.selectById(borrowId);
        if (borrow == null || !LibraryConstants.BORROW_STATUS_RESERVED.equals(borrow.getStatus())) {
            return false;
        }

        //检查是否在预留期内
        if (LocalDateTime.now().isAfter(borrow.getDueTime())) {
            return false;
        }

        //更新为正式借阅状态
        borrow.setStatus(LibraryConstants.BORROW_STATUS_BORROWING);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setDueTime(LocalDateTime.now().plusDays(LibraryConstants.BORROW_DAYS));
        bookBorrowMapper.updateById(borrow);

        //更新副本状态为已借出
        BookCopy copy = bookCopyService.getById(borrow.getCopyId());
        copy.setStatus(LibraryConstants.COPY_STATUS_BORROWED);
        bookCopyService.updateCopy(copy);

        return true;
    }

    //释放预留（超时或用户取消）
    @Transactional
    public boolean releaseReservation(Long borrowId) {
        BookBorrow borrow = bookBorrowMapper.selectById(borrowId);
        if (borrow == null || !LibraryConstants.BORROW_STATUS_RESERVED.equals(borrow.getStatus())) {
            return false;
        }

        //更新记录状态为RETURNED
        borrow.setStatus(LibraryConstants.BORROW_STATUS_RETURNED);
        borrow.setReturnTime(LocalDateTime.now());
        bookBorrowMapper.updateById(borrow);

        //更新副本状态为可用
        BookCopy copy = bookCopyService.getById(borrow.getCopyId());
        copy.setStatus(LibraryConstants.COPY_STATUS_AVAILABLE);
        bookCopyService.updateCopy(copy);

        return true;
    }
}