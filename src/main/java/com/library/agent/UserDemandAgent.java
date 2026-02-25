package com.library.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.agent.dto.ContractNetMessage;
import com.library.constant.LibraryConstants;
import com.library.service.*;
import com.library.util.AgentTaskManager;
import com.library.util.SpringContextUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.library.constant.LibraryConstants.BORROW_DAYS;
import static com.library.constant.LibraryConstants.RESERVATION_EXPIRE_DAYS;

//用户需求感知智能体（完整版：支持直接借书+调拨预留+无库存预约）
@Slf4j
public class UserDemandAgent extends Agent {

    private ObjectMapper mapper = new ObjectMapper();
    private AgentTaskManager taskManager;
    private BookCopyService bookCopyService;
    private BookTransferService transferService;
    private UserService userService;
    private BookBorrowService bookBorrowService;
    private BookReservationService bookReservationService; //新增：预约服务

    @Override
    protected void setup() {
        log.info("UserDemandAgent启动");

        taskManager = SpringContextUtil.getBean(AgentTaskManager.class);
        bookCopyService = SpringContextUtil.getBean(BookCopyService.class);
        transferService = SpringContextUtil.getBean(BookTransferService.class);
        userService = SpringContextUtil.getBean(UserService.class);
        bookBorrowService = SpringContextUtil.getBean(BookBorrowService.class);
        bookReservationService = SpringContextUtil.getBean(BookReservationService.class); //初始化

        addBehaviour(new TaskPollingBehaviour(this, 500));
    }

    //轮询任务队列的行为（完整内部类）
    private class TaskPollingBehaviour extends TickerBehaviour {
        public TaskPollingBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            AgentTaskManager.TaskRequest request = taskManager.pollTask();
            if (request != null) {
                processBorrowRequest(request);
            }
        }
    }

    //处理借书请求（完整方法）
    private void processBorrowRequest(AgentTaskManager.TaskRequest request) {
        String taskId = request.getTaskId();
        Long userId = request.getUserId();
        Long biblioId = request.getBiblioId();
        Long userLibraryId = request.getPreferredLibraryId();

        try {
            log.info("[{}] 处理借书请求，用户: {}, 书目: {}, 目标馆: {}",
                    taskId, userId, biblioId, userLibraryId);

            //步骤1：检查用户借阅权限
            String checkResult = userService.checkBorrowPermission(userId, biblioId);
            if (checkResult != null) {
                completeWithError(taskId, "权限检查失败: " + checkResult);
                return;
            }

            //步骤2：检查目标馆是否有可用库存
            List<com.library.entity.BookCopy> localCopies = bookCopyService.getAvailableCopies(userLibraryId, biblioId);

            if (!localCopies.isEmpty()) {
                //场景A：本馆有书 -> 直接借书
                log.info("[{}] 本馆有库存，执行直接借书", taskId);

                Long copyId = localCopies.get(0).getId();
                boolean success = bookBorrowService.directBorrow(userId, copyId, biblioId);

                if (success) {
                    userService.incrementBorrowCount(userId);

                    Map<String, Object> result = new HashMap<>();
                    result.put("taskId", taskId);
                    result.put("status", "SUCCESS");
                    result.put("strategy", "LOCAL_LOAN");
                    result.put("libraryId", userLibraryId);
                    result.put("copyId", copyId);
                    result.put("message", "借书成功，请在" + BORROW_DAYS + "天内归还");
                    //TODO: 预留座位功能（Phase 2）

                    String resultJson = mapper.writeValueAsString(result);
                    taskManager.completeTask(taskId, resultJson);
                    log.info("[{}] 直接借书成功", taskId);
                } else {
                    completeWithError(taskId, "直接借书失败（副本状态异常）");
                }

            } else {
                //场景B：本馆无书 -> 发起Contract Net协商调拨
                log.info("[{}] 本馆无库存，发起跨馆调拨协商", taskId);
                addBehaviour(new ContractNetInitiator(taskId, biblioId, userLibraryId, userId));
            }

        } catch (Exception e) {
            log.error("[{}] 处理借书请求异常", taskId, e);
            completeWithError(taskId, "系统异常: " + e.getMessage());
        }
    }

    //Contract Net发起者（完整内部类，包含无库存预约逻辑）
    private class ContractNetInitiator extends OneShotBehaviour {

        private String taskId;
        private Long biblioId;
        private Long userLibraryId;
        private Long userId;
        private List<ContractNetMessage> proposals = new ArrayList<>();
        private Map<AID, ACLMessage> proposeMessages = new ConcurrentHashMap<>();

        public ContractNetInitiator(String taskId, Long biblioId, Long userLibraryId, Long userId) {
            this.taskId = taskId;
            this.biblioId = biblioId;
            this.userLibraryId = userLibraryId;
            this.userId = userId;
        }

        @Override
        public void action() {
            try {
                //1.发现所有库存Agent
                AID[] inventoryAgents = discoverInventoryAgents();
                if (inventoryAgents.length == 0) {
                    completeWithError("无可用库存服务");
                    return;
                }

                //2.发送CFP（Call For Proposal）
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                cfp.setProtocol("FIPA-Contract-Net");
                cfp.setConversationId(taskId);

                ContractNetMessage cfpContent = ContractNetMessage.cfp(
                        taskId,
                        "BORROW_BOOK",
                        new HashMap<String, Object>() {{
                            put("biblioId", biblioId);
                            put("userLibraryId", userLibraryId);
                            put("userId", userId);
                        }}
                );

                cfp.setContent(mapper.writeValueAsString(cfpContent));

                for (AID agent : inventoryAgents) {
                    cfp.addReceiver(agent);
                }

                myAgent.send(cfp);
                log.info("[{}] 已发送CFP给 {} 个Agent", taskId, inventoryAgents.length);

                //3.等待并收集投标（3秒超时）
                long deadline = System.currentTimeMillis() + 3000;
                int receivedCount = 0;

                while (System.currentTimeMillis() < deadline && receivedCount < inventoryAgents.length) {
                    MessageTemplate mt = MessageTemplate.MatchConversationId(taskId);
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        receivedCount++;
                        ContractNetMessage proposal = mapper.readValue(reply.getContent(), ContractNetMessage.class);

                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            proposals.add(proposal);
                            proposeMessages.put(reply.getSender(), reply);
                            log.info("[{}] 收到投标，来自: {}, 评分: {}",
                                    taskId, reply.getSender().getLocalName(), proposal.getScore());
                        } else if (reply.getPerformative() == ACLMessage.REFUSE) {
                            log.info("[{}] 收到拒绝，来自: {}, 原因: {}",
                                    taskId, reply.getSender().getLocalName(), proposal.getReason());
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }

                //4.评标决策（关键修改：无投标时创建预约而非报错）
                if (proposals.isEmpty()) {
                    //场景C：全馆无库存 -> 创建预约记录
                    log.info("[{}] 所有馆均无库存，转为预约处理", taskId);

                    Long reservationId = bookReservationService.createReservation(userId, biblioId, userLibraryId);

                    Map<String, Object> result = new HashMap<>();
                    result.put("taskId", taskId);
                    result.put("status", "SUCCESS");
                    result.put("strategy", "RESERVATION"); //预约策略
                    result.put("reservationId", reservationId);
                    result.put("pickupLibraryId", userLibraryId);
                    result.put("message", "全馆暂无库存，已为您预约，" +
                            RESERVATION_EXPIRE_DAYS + "天内有效，书到货后将优先通知您取书");
                    //TODO: 当书归还时，自动触发调拨并通知用户（Phase 2）

                    String resultJson = mapper.writeValueAsString(result);
                    taskManager.completeTask(taskId, resultJson);
                    log.info("[{}] 已创建预约记录，ID: {}", taskId, reservationId);
                    return;
                }

                //选择评分最高的投标
                ContractNetMessage bestProposal = proposals.stream()
                        .max(Comparator.comparingDouble(ContractNetMessage::getScore))
                        .orElseThrow(() -> new RuntimeException("无有效投标"));

                Long selectedLibraryId = Long.valueOf(bestProposal.getProposal().get("libraryId").toString());
                String strategy = bestProposal.getProposal().get("strategy").toString();

                log.info("[{}] 选择最优方案: {}馆, 策略: {}, 评分: {}",
                        taskId, selectedLibraryId, strategy, bestProposal.getScore());

                //5.发送Accept/Reject通知
                sendAcceptance(bestProposal, selectedLibraryId);

                //6.执行调拨
                boolean transferSuccess = executeTransfer(bestProposal, selectedLibraryId);

                if (!transferSuccess) {
                    completeWithError("调拨执行失败");
                    return;
                }

                //7.调拨完成后，自动为用户预留24小时
                Long copyId = Long.valueOf(bestProposal.getProposal().get("copyId").toString());
                boolean reserveSuccess = bookBorrowService.reserveAfterTransfer(userId, copyId);

                if (reserveSuccess) {
                    userService.incrementBorrowCount(userId);

                    Map<String, Object> result = new HashMap<>();
                    result.put("taskId", taskId);
                    result.put("status", "SUCCESS");
                    result.put("strategy", "TRANSFER_PROVIDE");
                    result.put("sourceLibraryId", selectedLibraryId);
                    result.put("targetLibraryId", userLibraryId);
                    result.put("copyId", copyId);
                    result.put("reserveHours", LibraryConstants.RESERVE_HOURS);
                    result.put("message", "调拨已完成，已为您预留" + LibraryConstants.RESERVE_HOURS + "小时，请尽快取书");
                    //TODO: 推送通知功能（WebSocket/SMS）
                    //TODO: 预留座位功能（Phase 2）

                    String resultJson = mapper.writeValueAsString(result);
                    taskManager.completeTask(taskId, resultJson);
                    log.info("[{}] 调拨完成并已预留，等待用户取书", taskId);
                } else {
                    completeWithError("调拨完成但预留失败（副本异常）");
                }

            } catch (Exception e) {
                log.error("[{}] 调拨协商异常", taskId, e);
                completeWithError("系统异常: " + e.getMessage());
            }
        }

        //发现库存Agent（完整方法）
        private AID[] discoverInventoryAgents() {
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("INVENTORY_SERVICE");
                template.addServices(sd);

                DFAgentDescription[] results = DFService.search(myAgent, template);
                AID[] agents = new AID[results.length];
                for (int i = 0; i < results.length; i++) {
                    agents[i] = results[i].getName();
                }
                return agents;
            } catch (FIPAException e) {
                log.error("发现服务失败", e);
                return new AID[0];
            }
        }

        //发送中标/落标通知（完整方法）
        private void sendAcceptance(ContractNetMessage bestProposal, Long selectedLibraryId) {
            for (Map.Entry<AID, ACLMessage> entry : proposeMessages.entrySet()) {
                AID agent = entry.getKey();
                ACLMessage originalMsg = entry.getValue();
                ACLMessage response = originalMsg.createReply();

                Long agentLibraryId = Long.valueOf(bestProposal.getProposal().get("libraryId").toString());

                if (agentLibraryId.equals(selectedLibraryId)) {
                    response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    try {
                        response.setContent(mapper.writeValueAsString(bestProposal));
                    } catch (Exception e) {
                        log.error("序列化Accept消息失败", e);
                    }
                    myAgent.send(response);
                    log.info("[{}] 向 {} 发送ACCEPT", taskId, agent.getLocalName());
                } else {
                    response.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    myAgent.send(response);
                }
            }
        }

        //执行调拨（完整方法）
        private boolean executeTransfer(ContractNetMessage proposal, Long fromLibraryId) {
            try {
                Long copyId = Long.valueOf(proposal.getProposal().get("copyId").toString());

                Long transferId = transferService.createTransferRecord(copyId, fromLibraryId, userLibraryId, taskId);

                boolean success = bookCopyService.executeTransfer(copyId, fromLibraryId, userLibraryId, biblioId);

                if (success) {
                    transferService.completeTransfer(transferId);
                    log.info("[{}] 调拨执行成功", taskId);
                    return true;
                }
                return false;
            } catch (Exception e) {
                log.error("[{}] 调拨执行异常", taskId, e);
                return false;
            }
        }

        //错误处理（完整方法）
        private void completeWithError(String errorMsg) {
            try {
                Map<String, Object> error = new HashMap<>();
                error.put("taskId", taskId);
                error.put("status", "FAILED");
                error.put("message", errorMsg);
                String resultJson = mapper.writeValueAsString(error);
                taskManager.completeTask(taskId, resultJson);
                log.info("[{}] 协商失败: {}", taskId, errorMsg);
            } catch (Exception e) {
                log.error("序列化错误结果失败", e);
            }
        }
    }

    //错误处理辅助方法（用于processBorrowRequest）
    private void completeWithError(String taskId, String errorMsg) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("taskId", taskId);
            error.put("status", "FAILED");
            error.put("message", errorMsg);
            String resultJson = mapper.writeValueAsString(error);
            taskManager.completeTask(taskId, resultJson);
        } catch (Exception e) {
            log.error("序列化错误结果失败", e);
        }
    }

    @Override
    protected void takeDown() {
        log.info("UserDemandAgent关闭");
    }
}