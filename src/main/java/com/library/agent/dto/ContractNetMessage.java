package com.library.agent.dto;

import lombok.Data;

import java.util.Map;

//ContractNet协议消息体（CFP/PROPOSE/ACCEPT/REJECT）
@Data
public class ContractNetMessage {
    private String taskId;          //任务唯一标识
    private String taskType;        //BORROW_BOOK
    private Map<String, Object> constraints; //约束条件（书目ID、用户位置等）
    private long timestamp;

    //投标相关
    private Double score;           //投标评分（越高越优）
    private Map<String, Object> proposal;    //具体方案（库存数量、预计时间等）
    private String reason;          //拒绝原因

    //工厂方法：创建CFP
    public static ContractNetMessage cfp(String taskId, String type, Map<String, Object> constraints) {
        ContractNetMessage msg = new ContractNetMessage();
        msg.setTaskId(taskId);
        msg.setTaskType(type);
        msg.setConstraints(constraints);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    //工厂方法：创建PROPOSE
    public static ContractNetMessage propose(String taskId, double score, Map<String, Object> proposal) {
        ContractNetMessage msg = new ContractNetMessage();
        msg.setTaskId(taskId);
        msg.setScore(score);
        msg.setProposal(proposal);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    //工厂方法：创建REFUSE
    public static ContractNetMessage refuse(String taskId, String reason) {
        ContractNetMessage msg = new ContractNetMessage();
        msg.setTaskId(taskId);
        msg.setReason(reason);
        return msg;
    }
}