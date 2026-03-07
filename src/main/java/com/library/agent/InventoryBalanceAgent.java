package com.library.agent;

import com.library.service.InventoryBalanceService;
import com.library.util.SpringContextUtil;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import lombok.extern.slf4j.Slf4j;

//库存平衡智能体（负责主动调拨）
@Slf4j
public class InventoryBalanceAgent extends Agent {

    private InventoryBalanceService balanceService;

    @Override
    protected void setup() {
        log.info("========== InventoryBalanceAgent 启动 ==========");

        //获取库存平衡服务
        balanceService = SpringContextUtil.getBean(InventoryBalanceService.class);

        //添加定时行为：每天凌晨2点执行库存平衡分析
        //这里简化为每24小时执行一次（实际应该使用cron表达式）
        addBehaviour(new TickerBehaviour(this, 24 * 60 * 60 * 1000L) {
            @Override
            protected void onTick() {
                try {
                    log.info("========== 开始执行库存平衡分析 ==========");
                    balanceService.analyzeAndBalance();
                    log.info("========== 库存平衡分析完成 ==========");
                } catch (Exception e) {
                    log.error("库存平衡分析执行失败", e);
                }
            }
        });

        log.info("InventoryBalanceAgent 初始化完成，将在每天凌晨2点执行库存平衡分析");
    }

    @Override
    protected void takeDown() {
        log.info("========== InventoryBalanceAgent 关闭 ==========");
    }
}
