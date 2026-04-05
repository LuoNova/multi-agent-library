package com.library.agent;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import lombok.extern.slf4j.Slf4j;

/**
 * 座位智能体（仅用于在 DF 图形化界面展示，无实际业务逻辑）。
 * 座位预约与可用性查询仍由 SeatReservationService / SeatAvailabilityService 等模块提供。
 */
@Slf4j
public class SeatAgent extends Agent {

    @Override
    protected void setup() {
        log.info("座位智能体启动（仅注册到 DF 供界面展示）");
        registerToDF();
    }

    private void registerToDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("SEAT_SERVICE");
            sd.setName("SeatReservation");
            dfd.addServices(sd);
            DFService.register(this, dfd);
            log.info("座位智能体已注册到 DF: {} (type=SEAT_SERVICE)", getAID().getName());
        } catch (FIPAException e) {
            log.error("座位智能体注册 DF 失败", e);
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            log.warn("座位智能体注销 DF 时异常: {}", e.getMessage());
        }
        log.info("座位智能体关闭");
    }
}
