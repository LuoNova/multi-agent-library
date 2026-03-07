package com.library.task;

import com.library.service.InventoryBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//库存平衡定时任务
@Slf4j
@Component
public class InventoryBalanceTask {

    @Autowired
    private InventoryBalanceService balanceService;

    //每天凌晨2点执行库存平衡分析
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeInventoryBalance() {
        log.info("========== 开始执行库存平衡定时任务 ==========");
        try {
            balanceService.analyzeAndBalance();
            log.info("========== 库存平衡定时任务执行完成 ==========");
        } catch (Exception e) {
            log.error("库存平衡定时任务执行失败", e);
        }
    }
}
