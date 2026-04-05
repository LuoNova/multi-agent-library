package com.library.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.library.dto.fault.FaultHealthQueryRequest;
import com.library.dto.fault.FaultHealthQueryResponse;
import com.library.service.FaultService;
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

/**
 * 故障报修智能体：在 DF 注册，接收 ACL REQUEST，执行与 REST 一致的资源健康批量查询。
 * 请求体可与 POST /api/fault/health/query 相同：{"resources":[...]}，可选字段 "type":"RESOURCE_HEALTH_QUERY"。
 * 成功回复 INFORM：{"type":"RESOURCE_HEALTH_RESPONSE","results":[...]}。
 */
@Slf4j
public class FaultRepairAgent extends Agent {

    private ObjectMapper objectMapper;
    private FaultService faultService;

    @Override
    protected void setup() {
        log.info("FaultRepairAgent 启动");
        objectMapper = SpringContextUtil.getBean(ObjectMapper.class);
        faultService = SpringContextUtil.getBean(FaultService.class);
        registerToDF();
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg == null) {
                    block();
                    return;
                }
                handleHealthRequest(msg);
            }
        });
    }

    private void registerToDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("FAULT_SERVICE");
            sd.setName("FaultRepair");
            dfd.addServices(sd);
            DFService.register(this, dfd);
            log.info("FaultRepairAgent 已注册 DF: {} (type=FAULT_SERVICE)", getAID().getName());
        } catch (FIPAException e) {
            log.error("FaultRepairAgent 注册 DF 失败", e);
        }
    }

    private void handleHealthRequest(ACLMessage msg) {
        try {
            String raw = msg.getContent();
            if (raw == null || raw.isBlank()) {
                replyFailure(msg, "empty content");
                return;
            }
            JsonNode root = objectMapper.readTree(raw);
            FaultHealthQueryRequest req = objectMapper.treeToValue(root, FaultHealthQueryRequest.class);
            if (req == null) {
                req = new FaultHealthQueryRequest();
            }
            FaultHealthQueryResponse resp = faultService.healthQuery(req);
            JsonNode resultsNode = objectMapper.valueToTree(resp.getResults());
            ObjectNode out = objectMapper.createObjectNode();
            out.put("type", "RESOURCE_HEALTH_RESPONSE");
            out.set("results", resultsNode);
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(objectMapper.writeValueAsString(out));
            send(reply);
        } catch (Exception ex) {
            log.error("FaultRepairAgent 处理健康查询失败", ex);
            replyFailure(msg, ex.getMessage() != null ? ex.getMessage() : "error");
        }
    }

    private void replyFailure(ACLMessage msg, String err) {
        try {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.FAILURE);
            ObjectNode o = objectMapper.createObjectNode();
            o.put("error", err);
            reply.setContent(objectMapper.writeValueAsString(o));
            send(reply);
        } catch (Exception e) {
            log.warn("FaultRepairAgent 发送失败回复异常: {}", e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            log.warn("FaultRepairAgent 注销 DF 异常: {}", e.getMessage());
        }
        log.info("FaultRepairAgent 关闭");
    }
}
