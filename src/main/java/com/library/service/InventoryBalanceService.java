package com.library.service;

import com.library.entity.TransferSuggestion;

import java.util.List;

//库存平衡服务接口
public interface InventoryBalanceService {

    //分析库存并执行平衡调拨
    void analyzeAndBalance();

    //生成调拨建议列表
    List<TransferSuggestion> generateTransferSuggestions();

    //执行调拨建议（自动审批模式）
    void executeSuggestion(Long suggestionId);

    //审批调拨建议（人工审批模式）
    void approveSuggestion(Long suggestionId, Long approverId);

    //拒绝调拨建议
    void rejectSuggestion(Long suggestionId, Long approverId, String reason);

    //获取待审批的调拨建议
    List<TransferSuggestion> getPendingSuggestions();

    //手动触发库存平衡分析
    void triggerManualBalance();
}
