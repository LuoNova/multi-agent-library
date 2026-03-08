package com.library.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

//批量调拨进度响应DTO
@Data
public class BatchTransferProgressDTO {
    //调拨单ID
    private Long orderId;
    
    //书目ID
    private Long biblioId;
    
    //图书信息
    private TransferProgressDTO.BookInfo bookInfo;
    
    //源馆信息
    private TransferProgressDTO.LibraryInfo fromLibrary;
    
    //目标馆信息
    private TransferProgressDTO.LibraryInfo toLibrary;
    
    //状态
    private String status;
    
    //状态文本
    private String statusText;
    
    //进度信息
    private ProgressInfo progressInfo;
    
    //时间信息
    private BatchTimeInfo timeInfo;
    
    //调拨记录列表
    private List<TransferRecordInfo> transferRecords;
    
    //进度信息内部类
    @Data
    public static class ProgressInfo {
        private Integer totalQuantity;
        private Integer completedQuantity;
        private Integer inTransitQuantity;
        private Integer pendingQuantity;
        private Integer progressPercentage;
    }
    
    //批量时间信息内部类
    @Data
    public static class BatchTimeInfo {
        private LocalDateTime createTime;
        private LocalDateTime completeTime;
        private String usedDuration;
    }
    
    //调拨记录信息内部类
    @Data
    public static class TransferRecordInfo {
        private Long transferId;
        private Long copyId;
        private String status;
        private String statusText;
        private Integer progressPercentage;
    }
}
