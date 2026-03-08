package com.library.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.entity.BookTransfer;
import com.library.mapper.BookTransferMapper;
import com.library.service.TransferNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

//调拨延迟检测定时任务
@Slf4j
@Component
public class TransferDelayCheckTask {

    @Autowired
    private BookTransferMapper transferMapper;
    
    @Autowired
    private TransferNotificationService notificationService;

    //每小时检查一次延迟调拨
    @Scheduled(cron = "0 0 * * * ?")
    public void checkTransferDelay() {
        log.info("开始执行调拨延迟检测任务");
        
        //查询所有运输中且超过预计到达时间的调拨记录
        List<BookTransfer> delayedTransfers = transferMapper.selectList(
            new LambdaQueryWrapper<BookTransfer>()
                .eq(BookTransfer::getStatus, "IN_TRANSIT")
                .lt(BookTransfer::getEstimatedArrivalTime, LocalDateTime.now())
        );
        
        if (delayedTransfers.isEmpty()) {
            log.info("未发现延迟调拨");
            return;
        }
        
        log.warn("发现{}条延迟调拨", delayedTransfers.size());
        
        //发送延迟告警
        for (BookTransfer transfer : delayedTransfers) {
            notificationService.sendTransferDelayAlert(transfer);
        }
        
        log.info("调拨延迟检测任务完成");
    }
}
