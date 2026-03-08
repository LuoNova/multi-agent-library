package com.library.dto;

import lombok.Data;
import java.time.LocalDateTime;

//调拨进度响应DTO
@Data
public class TransferProgressDTO {
    //调拨记录ID
    private Long transferId;
    
    //副本ID
    private Long copyId;
    
    //图书信息
    private BookInfo bookInfo;
    
    //源馆信息
    private LibraryInfo fromLibrary;
    
    //目标馆信息
    private LibraryInfo toLibrary;
    
    //状态
    private String status;
    
    //状态文本
    private String statusText;
    
    //进度百分比(0-100)
    private Integer progressPercentage;
    
    //时间信息
    private TimeInfo timeInfo;
    
    //调拨原因
    private String transferReason;
    
    //调拨原因文本
    private String transferReasonText;
    
    //图书信息内部类
    @Data
    public static class BookInfo {
        private String title;
        private String author;
        private String isbn;
    }
    
    //馆信息内部类
    @Data
    public static class LibraryInfo {
        private Long id;
        private String name;
        private String location;
    }
    
    //时间信息内部类
    @Data
    public static class TimeInfo {
        private LocalDateTime requestTime;
        private LocalDateTime estimatedArrivalTime;
        private LocalDateTime actualArrivalTime;
        private String usedDuration;
        private String remainingDuration;
    }
}
