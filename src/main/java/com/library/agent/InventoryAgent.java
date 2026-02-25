package com.library.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.agent.dto.ContractNetMessage;
import com.library.entity.BookCopy;
import com.library.service.BookCopyService;
import com.library.service.LibraryBiblioStatsService;
import com.library.service.LibraryService;
import com.library.util.SpringContextUtil;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//库存智能体（每馆一个实例，Contractor角色）
@Slf4j
public class InventoryAgent extends Agent {

    private Long libraryId;         //本馆ID（从参数传入）
    private String libraryName;     //本馆名称
    private ObjectMapper mapper = new ObjectMapper();

    //Service引用（通过SpringContextUtil获取）
    private BookCopyService bookCopyService;
    private LibraryBiblioStatsService statsService;
    private LibraryService libraryService;

    @Override
    protected void setup() {
        //获取传入参数
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            this.libraryId = Long.parseLong(args[0].toString());
            this.libraryName = args[1].toString();
        }

        log.info("库存智能体启动，管辖: {} (ID: {})", libraryName, libraryId);

        //获取SpringBean
        bookCopyService = SpringContextUtil.getBean(BookCopyService.class);
        statsService = SpringContextUtil.getBean(LibraryBiblioStatsService.class);
        libraryService = SpringContextUtil.getBean(LibraryService.class);

        //注册到DF服务
        registerService();

        //添加CFP监听行为
        addBehaviour(new CFPHandler());
    }

    //注册到目录服务（便于UserDemandAgent发现）
    private void registerService() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("INVENTORY_SERVICE");
            sd.setName("Inventory-" + libraryName);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            log.info("已注册到DF服务: {}", getAID().getName());
        } catch (FIPAException e) {
            log.error("注册DF服务失败", e);
        }
    }

    //CFP消息处理器
    private class CFPHandler extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchProtocol("FIPA-Contract-Net")
            );

            ACLMessage msg = receive(mt);
            if (msg != null) {
                handleCFP(msg);
            } else {
                block();
            }
        }
    }

    //处理CFP（评估能力并投标）
    private void handleCFP(ACLMessage cfp) {
        try {
            ContractNetMessage request = mapper.readValue(cfp.getContent(), ContractNetMessage.class);
            String taskId = request.getTaskId();
            Long biblioId = Long.valueOf(request.getConstraints().get("biblioId").toString());
            Long userLibraryId = Long.valueOf(request.getConstraints().get("userLibraryId").toString());

            log.info("[{}] 收到CFP，书目: {}, 用户所在馆: {}", libraryName, biblioId, userLibraryId);

            //评估本馆能力
            EvaluationResult eval = evaluateCapability(biblioId, userLibraryId);

            ACLMessage reply = cfp.createReply();

            if (eval.canServe) {
                //发送PROPOSE（投标）
                reply.setPerformative(ACLMessage.PROPOSE);
                ContractNetMessage proposal = ContractNetMessage.propose(
                        taskId,
                        eval.score,
                        Map.of(
                                "libraryId", libraryId,
                                "libraryName", libraryName,
                                "strategy", eval.strategy,
                                "availableCopies", eval.availableCopies,
                                "estimatedMinutes", eval.estimatedMinutes,
                                "copyId", eval.suggestedCopyId //建议调拨的副本ID
                        )
                );
                reply.setContent(mapper.writeValueAsString(proposal));
                log.info("[{}] 发送投标，评分: {}, 策略: {}", libraryName, eval.score, eval.strategy);
            } else {
                //发送REFUSE
                reply.setPerformative(ACLMessage.REFUSE);
                ContractNetMessage refuse = ContractNetMessage.refuse(taskId, eval.reason);
                reply.setContent(mapper.writeValueAsString(refuse));
                log.info("[{}] 拒绝投标，原因: {}", libraryName, eval.reason);
            }

            send(reply);

        } catch (Exception e) {
            log.error("处理CFP异常", e);
        }
    }

    //核心决策算法：评估服务能力
    private EvaluationResult evaluateCapability(Long biblioId, Long userLibraryId) {
        EvaluationResult result = new EvaluationResult();

        //1.查询本馆可用库存
        List<BookCopy> availableCopies = bookCopyService.getAvailableCopies(libraryId, biblioId);
        int stock = availableCopies.size();

        //2.查询统计信息（压力）
        var stats = statsService.getStats(libraryId, biblioId);
        int pressure = stats != null ? stats.getReservationPendingCount() : 0;

        if (stock > 0) {
            //有库存
            result.availableCopies = stock;
            result.suggestedCopyId = availableCopies.get(0).getId(); //取第一本

            if (libraryId.equals(userLibraryId)) {
                //A1:用户就在本馆 -> 最优方案（满分100）
                result.canServe = true;
                result.strategy = "LOCAL_LOAN";
                result.score = 100.0;
                result.estimatedMinutes = 5;
            } else {
                //A2:书在本馆，但用户在别的馆 -> 提供调拨方案
                double distance = libraryService.calculateDistance(libraryId, userLibraryId);
                result.canServe = true;
                result.strategy = "TRANSFER_PROVIDE";
                //评分公式：基础80分 - 距离成本(每公里-10分) - 压力成本(每人-5分)
                result.score = 80.0 - (distance * 10) - (pressure * 5);
                result.estimatedMinutes = 30 + (int) (distance * 15); //30分钟基础+运输时间
            }
        } else {
            //无库存
            result.canServe = false;
            result.reason = "本馆库存不足，当前可用: 0";
        }

        return result;
    }

    //评估结果内部类
    private static class EvaluationResult {
        boolean canServe;
        String strategy;        //LOCAL_LOAN或TRANSFER_PROVIDE
        double score;           //投标评分
        int availableCopies;    //可用副本数
        int estimatedMinutes;   //预计完成时间（分钟）
        Long suggestedCopyId;   //建议调拨的副本ID
        String reason;          //拒绝原因
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        log.info("库存智能体关闭: {}", libraryName);
    }
}