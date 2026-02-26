package com.library.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//图书状态管理器，统一管理借阅记录和副本的状态转换
@Slf4j
@Component
public class BookStateManager {

    //借阅记录状态枚举
    public enum BorrowStatus {
        TRANSFERRING,  //调拨中
        RESERVED,      //已预留
        BORROWING,     //借阅中
        RETURNED       //已归还
    }

    //副本状态枚举
    public enum CopyStatus {
        AVAILABLE,     //可用
        IN_TRANSIT,    //运输中
        RESERVED,      //已预留
        BORROWED       //已借出
    }

    //借阅记录状态转换规则
    private static final Map<BorrowStatus, Set<BorrowStatus>> BORROW_STATUS_TRANSITIONS = new HashMap<>();

    //副本状态转换规则
    private static final Map<CopyStatus, Set<CopyStatus>> COPY_STATUS_TRANSITIONS = new HashMap<>();

    static {
        //初始化借阅记录状态转换规则
        BORROW_STATUS_TRANSITIONS.put(BorrowStatus.TRANSFERRING,
                EnumSet.of(BorrowStatus.RESERVED, BorrowStatus.BORROWING));
        BORROW_STATUS_TRANSITIONS.put(BorrowStatus.RESERVED,
                EnumSet.of(BorrowStatus.BORROWING));
        BORROW_STATUS_TRANSITIONS.put(BorrowStatus.BORROWING,
                EnumSet.of(BorrowStatus.RETURNED));
        BORROW_STATUS_TRANSITIONS.put(BorrowStatus.RETURNED,
                EnumSet.noneOf(BorrowStatus.class));

        //初始化副本状态转换规则
        COPY_STATUS_TRANSITIONS.put(CopyStatus.AVAILABLE,
                EnumSet.of(CopyStatus.IN_TRANSIT, CopyStatus.RESERVED, CopyStatus.BORROWED));
        COPY_STATUS_TRANSITIONS.put(CopyStatus.IN_TRANSIT,
                EnumSet.of(CopyStatus.AVAILABLE));
        COPY_STATUS_TRANSITIONS.put(CopyStatus.RESERVED,
                EnumSet.of(CopyStatus.BORROWED, CopyStatus.AVAILABLE));
        COPY_STATUS_TRANSITIONS.put(CopyStatus.BORROWED,
                EnumSet.of(CopyStatus.AVAILABLE));
    }

    //验证借阅记录状态转换是否合法
    public boolean isValidBorrowStatusTransition(String fromStatus, String toStatus) {
        try {
            BorrowStatus from = BorrowStatus.valueOf(fromStatus);
            BorrowStatus to = BorrowStatus.valueOf(toStatus);

            Set<BorrowStatus> allowedTransitions = BORROW_STATUS_TRANSITIONS.get(from);
            if (allowedTransitions == null) {
                log.warn("借阅状态{}没有定义转换规则", fromStatus);
                return false;
            }

            if (!allowedTransitions.contains(to)) {
                log.warn("非法的借阅状态转换: {} -> {}", fromStatus, toStatus);
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            log.error("无效的借阅状态: from={}, to={}", fromStatus, toStatus, e);
            return false;
        }
    }

    //验证副本状态转换是否合法
    public boolean isValidCopyStatusTransition(String fromStatus, String toStatus) {
        try {
            CopyStatus from = CopyStatus.valueOf(fromStatus);
            CopyStatus to = CopyStatus.valueOf(toStatus);

            Set<CopyStatus> allowedTransitions = COPY_STATUS_TRANSITIONS.get(from);
            if (allowedTransitions == null) {
                log.warn("副本状态{}没有定义转换规则", fromStatus);
                return false;
            }

            if (!allowedTransitions.contains(to)) {
                log.warn("非法的副本状态转换: {} -> {}", fromStatus, toStatus);
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            log.error("无效的副本状态: from={}, to={}", fromStatus, toStatus, e);
            return false;
        }
    }

    //执行借阅记录状态转换（带验证）
    public boolean transitionBorrowStatus(String fromStatus, String toStatus) {
        if (!isValidBorrowStatusTransition(fromStatus, toStatus)) {
            return false;
        }

        log.info("借阅记录状态转换成功: {} -> {}", fromStatus, toStatus);
        return true;
    }

    //执行副本状态转换（带验证）
    public boolean transitionCopyStatus(String fromStatus, String toStatus) {
        if (!isValidCopyStatusTransition(fromStatus, toStatus)) {
            return false;
        }

        log.info("副本状态转换成功: {} -> {}", fromStatus, toStatus);
        return true;
    }

    //获取借阅记录状态的合法转换目标
    public Set<String> getValidBorrowStatusTransitions(String fromStatus) {
        try {
            BorrowStatus from = BorrowStatus.valueOf(fromStatus);
            Set<BorrowStatus> transitions = BORROW_STATUS_TRANSITIONS.get(from);
            if (transitions == null) {
                return Set.of();
            }

            return transitions.stream()
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (IllegalArgumentException e) {
            log.error("无效的借阅状态: {}", fromStatus, e);
            return Set.of();
        }
    }

    //获取副本状态的合法转换目标
    public Set<String> getValidCopyStatusTransitions(String fromStatus) {
        try {
            CopyStatus from = CopyStatus.valueOf(fromStatus);
            Set<CopyStatus> transitions = COPY_STATUS_TRANSITIONS.get(from);
            if (transitions == null) {
                return Set.of();
            }

            return transitions.stream()
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (IllegalArgumentException e) {
            log.error("无效的副本状态: {}", fromStatus, e);
            return Set.of();
        }
    }
}
