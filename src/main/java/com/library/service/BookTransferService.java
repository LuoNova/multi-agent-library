package com.library.service;

import com.library.entity.BookTransfer;
import com.library.mapper.BookTransferMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

//调拨记录服务
@Service
public class BookTransferService {

    @Autowired
    private BookTransferMapper bookTransferMapper;

    //创建调拨记录
    public Long createTransferRecord(Long copyId, Long fromLibId, Long toLibId, String requestId) {
        BookTransfer transfer = new BookTransfer();
        transfer.setCopyId(copyId);
        transfer.setFromLibraryId(fromLibId);
        transfer.setToLibraryId(toLibId);
        transfer.setStatus("PENDING");
        transfer.setRequestId(requestId);
        transfer.setRequestTime(LocalDateTime.now());
        //预计到达时间：当前时间+30分钟
        transfer.setCompleteTime(LocalDateTime.now().plusMinutes(30));

        bookTransferMapper.insert(transfer);
        return transfer.getId();
    }

    //更新调拨状态为运输中
    public boolean startTransfer(Long transferId) {
        BookTransfer transfer = bookTransferMapper.selectById(transferId);
        if (transfer == null) return false;
        transfer.setStatus("IN_TRANSIT");
        return bookTransferMapper.updateById(transfer) > 0;
    }

    //完成调拨
    public boolean completeTransfer(Long transferId) {
        BookTransfer transfer = bookTransferMapper.selectById(transferId);
        if (transfer == null) return false;
        transfer.setStatus("COMPLETED");
        transfer.setCompleteTime(LocalDateTime.now());
        return bookTransferMapper.updateById(transfer) > 0;
    }
}