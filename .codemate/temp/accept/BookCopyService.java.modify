package com.library.service;

import com.library.entity.BookCopy;
import com.library.mapper.BookCopyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.library.constant.LibraryConstants.COPY_STATUS_AVAILABLE;
import static com.library.constant.LibraryConstants.COPY_STATUS_IN_TRANSIT;

//图书副本服务（修正版：executeTransfer添加biblioId参数）
@Slf4j
@Service
public class BookCopyService {

    @Autowired
    private BookCopyMapper bookCopyMapper;

    @Autowired
    private LibraryBiblioStatsService statsService;

    //查询可用副本
    public List<BookCopy> getAvailableCopies(Long libraryId, Long biblioId) {
        return bookCopyMapper.selectAvailableByBiblioAndLibrary(biblioId, libraryId);
    }

    //查询单个副本
    public BookCopy getById(Long copyId) {
        return bookCopyMapper.selectById(copyId);
    }

    //执行调拨（场景A：真实物流，书运输中不可借）
    @Transactional
    public boolean executeTransfer(Long copyId, Long fromLibraryId, Long toLibraryId, Long biblioId) {
        //1.检查副本状态
        BookCopy copy = bookCopyMapper.selectById(copyId);
        if (copy == null || !COPY_STATUS_AVAILABLE.equals(copy.getStatus())) {
            log.warn("副本{}状态异常，当前状态：{}", copyId, copy != null ? copy.getStatus() : "null");
            return false;
        }

        //2.更新副本状态为运输中（书已装箱发货，但还没到）
        //注意：libraryId保持原馆（书还在原馆物理位置，但即将运走）
        copy.setStatus(COPY_STATUS_IN_TRANSIT);
        copy.setUpdateTime(LocalDateTime.now());
        int updated = bookCopyMapper.updateById(copy);

        if (updated == 0) {
            return false;
        }

        //3.源馆库存减1（书已出库，不再可借）
        statsService.decrementStock(fromLibraryId, biblioId);

        //4.【删除】目标馆库存不加（等书实际到达后再加，在TransferService.completeTransfer中加）

        //5.【删除】立即改为AVAILABLE的逻辑（书还在路上，不能借）

        log.info("调拨发起成功：副本{}从馆{}运往馆{}，当前状态IN_TRANSIT",
                copyId, fromLibraryId, toLibraryId);
        return true;
    }

    //完成调拨（到达后改为AVAILABLE）
    @Transactional
    public boolean completeTransfer(Long copyId) {
        BookCopy copy = bookCopyMapper.selectById(copyId);
        if (copy == null) {
            return false;
        }
        copy.setStatus(COPY_STATUS_AVAILABLE);
        copy.setUpdateTime(LocalDateTime.now());
        return bookCopyMapper.updateById(copy) > 0;
    }

    //在 BookCopyService 中添加
    public boolean updateCopy(BookCopy copy) {
        return bookCopyMapper.updateById(copy) > 0;
    }
}