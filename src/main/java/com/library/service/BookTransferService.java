package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.config.BusinessRulesProperties;
import com.library.entity.BookTransfer;
import com.library.mapper.BookTransferMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

//调拨记录服务（职责：专注于调拨记录的CRUD操作）
@Slf4j
@Service
public class BookTransferService {

    @Autowired
    private BookTransferMapper bookTransferMapper;

    @Autowired
    private BusinessRulesProperties businessRulesProperties;

    //创建调拨记录
    public Long createTransferRecord(Long copyId, Long fromLibId, Long toLibId, String requestId) {
        BookTransfer transfer = new BookTransfer();
        transfer.setCopyId(copyId);
        transfer.setFromLibraryId(fromLibId);
        transfer.setToLibraryId(toLibId);
        transfer.setStatus("PENDING");
        transfer.setRequestId(requestId);
        transfer.setRequestTime(LocalDateTime.now());
        //预计到达时间：当前时间+配置的调拨预计时间
        transfer.setCompleteTime(LocalDateTime.now().plusMinutes(
                businessRulesProperties.getTransfer().getEstimatedMinutes()));

        bookTransferMapper.insert(transfer);
        log.info("创建调拨记录成功: transferId={}, copyId={}, from={}, to={}",
                transfer.getId(), copyId, fromLibId, toLibId);
        return transfer.getId();
    }

    //更新调拨状态为运输中
    public boolean startTransfer(Long transferId) {
        BookTransfer transfer = bookTransferMapper.selectById(transferId);
        if (transfer == null) {
            log.warn("调拨记录不存在: transferId={}", transferId);
            return false;
        }
        transfer.setStatus("IN_TRANSIT");
        boolean success = bookTransferMapper.updateById(transfer) > 0;
        if (success) {
            log.info("调拨状态更新为运输中: transferId={}", transferId);
        }
        return success;
    }

    //完成调拨
    public boolean completeTransfer(Long transferId) {
        BookTransfer transfer = bookTransferMapper.selectById(transferId);
        if (transfer == null) {
            log.warn("调拨记录不存在: transferId={}", transferId);
            return false;
        }
        transfer.setStatus("COMPLETED");
        transfer.setCompleteTime(LocalDateTime.now());
        boolean success = bookTransferMapper.updateById(transfer) > 0;
        if (success) {
            log.info("调拨完成: transferId={}", transferId);
        }
        return success;
    }

    //根据ID查询调拨记录
    public BookTransfer getById(Long transferId) {
        return bookTransferMapper.selectById(transferId);
    }

    //根据副本ID查询调拨记录
    public List<BookTransfer> getByCopyId(Long copyId) {
        LambdaQueryWrapper<BookTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookTransfer::getCopyId, copyId);
        return bookTransferMapper.selectList(wrapper);
    }

    //根据请求ID查询调拨记录
    public List<BookTransfer> getByRequestId(String requestId) {
        LambdaQueryWrapper<BookTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookTransfer::getRequestId, requestId);
        return bookTransferMapper.selectList(wrapper);
    }

    //查询进行中的调拨记录
    public List<BookTransfer> getActiveTransfers() {
        LambdaQueryWrapper<BookTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BookTransfer::getStatus, "PENDING", "IN_TRANSIT");
        return bookTransferMapper.selectList(wrapper);
    }
}