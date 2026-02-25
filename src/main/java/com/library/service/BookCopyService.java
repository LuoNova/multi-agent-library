package com.library.service;

import com.library.entity.BookCopy;
import com.library.mapper.BookCopyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

//图书副本服务（修正版：executeTransfer添加biblioId参数）
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

    //执行调拨（关键修正：添加fromLibraryId和biblioId参数）
    @Transactional
    public boolean executeTransfer(Long copyId, Long fromLibraryId, Long toLibraryId, Long biblioId) {
        //1.确保目标馆统计记录存在
        statsService.initStatsIfNotExists(toLibraryId, biblioId);

        //2.更新副本表
        BookCopy copy = bookCopyMapper.selectById(copyId);
        if (copy == null || !"AVAILABLE".equals(copy.getStatus())) {
            return false;
        }

        copy.setLibraryId(toLibraryId);
        copy.setStatus("IN_TRANSIT");
        copy.setUpdateTime(LocalDateTime.now());
        int updated = bookCopyMapper.updateById(copy);

        if (updated == 0) {
            return false;
        }

        //3.源馆库存减1
        statsService.decrementStock(fromLibraryId, biblioId);

        //4.目标馆库存加1
        statsService.incrementStock(toLibraryId, biblioId);

        //5.【新增】立即完成调拨（模拟运输即时到达，演示用）
        copy.setStatus("AVAILABLE");  //关键修复：立即改为可借状态
        copy.setUpdateTime(LocalDateTime.now());
        bookCopyMapper.updateById(copy);

        //同时更新调拨记录为已完成（可选）
        //transferService.completeTransfer(transferId);

        return true;
    }

    //完成调拨（到达后改为AVAILABLE）
    @Transactional
    public boolean completeTransfer(Long copyId) {
        BookCopy copy = bookCopyMapper.selectById(copyId);
        if (copy == null) {
            return false;
        }
        copy.setStatus("AVAILABLE");
        copy.setUpdateTime(LocalDateTime.now());
        return bookCopyMapper.updateById(copy) > 0;
    }

    //在 BookCopyService 中添加
    public boolean updateCopy(BookCopy copy) {
        return bookCopyMapper.updateById(copy) > 0;
    }
}