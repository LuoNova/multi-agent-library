package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.dto.BatchTransferProgressDTO;
import com.library.dto.TransferProgressDTO;
import com.library.entity.*;
import com.library.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//调拨进度服务
@Slf4j
@Service
public class TransferProgressService {

    @Autowired
    private BookTransferMapper transferMapper;
    
    @Autowired
    private TransferOrderMapper orderMapper;
    
    @Autowired
    private BookCopyMapper copyMapper;
    
    @Autowired
    private BookBiblioMapper biblioMapper;
    
    @Autowired
    private LibraryMapper libraryMapper;

    //查询单个调拨进度
    public TransferProgressDTO getTransferProgress(Long transferId) {
        log.info("查询调拨进度: transferId={}", transferId);
        
        //查询调拨记录
        BookTransfer transfer = transferMapper.selectById(transferId);
        if (transfer == null) {
            log.warn("调拨记录不存在: {}", transferId);
            return null;
        }
        
        return buildTransferProgressDTO(transfer);
    }

    //查询批量调拨进度
    public BatchTransferProgressDTO getBatchTransferProgress(Long orderId) {
        log.info("查询批量调拨进度: orderId={}", orderId);
        
        //查询调拨单
        TransferOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("调拨单不存在: {}", orderId);
            return null;
        }
        
        return buildBatchTransferProgressDTO(order);
    }

    //查询用户调拨列表
    public List<TransferProgressDTO> getUserTransfers(Long userId, String status, int page, int size) {
        log.info("查询用户调拨列表: userId={}, status={}, page={}, size={}", userId, status, page, size);
        
        //TODO: 需要根据用户ID查询相关的调拨记录
        //这里需要关联BookBorrow表,找到用户相关的调拨
        //暂时返回空列表,后续完善
        
        return new ArrayList<>();
    }

    //构建单个调拨进度DTO
    private TransferProgressDTO buildTransferProgressDTO(BookTransfer transfer) {
        TransferProgressDTO dto = new TransferProgressDTO();
        
        //基本信息
        dto.setTransferId(transfer.getId());
        dto.setCopyId(transfer.getCopyId());
        dto.setStatus(transfer.getStatus());
        dto.setStatusText(getStatusText(transfer.getStatus()));
        dto.setTransferReason(transfer.getTransferReason());
        dto.setTransferReasonText(getTransferReasonText(transfer.getTransferReason()));
        
        //计算进度百分比
        dto.setProgressPercentage(calculateProgressPercentage(transfer));
        
        //查询副本和书目信息
        BookCopy copy = copyMapper.selectById(transfer.getCopyId());
        if (copy != null) {
            BookBiblio biblio = biblioMapper.selectById(copy.getBiblioId());
            if (biblio != null) {
                TransferProgressDTO.BookInfo bookInfo = new TransferProgressDTO.BookInfo();
                bookInfo.setTitle(biblio.getTitle());
                bookInfo.setAuthor(biblio.getAuthor());
                bookInfo.setIsbn(biblio.getIsbn());
                dto.setBookInfo(bookInfo);
            }
        }
        
        //查询馆信息
        Library fromLib = libraryMapper.selectById(transfer.getFromLibraryId());
        if (fromLib != null) {
            TransferProgressDTO.LibraryInfo fromLibInfo = new TransferProgressDTO.LibraryInfo();
            fromLibInfo.setId(fromLib.getId());
            fromLibInfo.setName(fromLib.getName());
            fromLibInfo.setLocation(fromLib.getLocationDesc());
            dto.setFromLibrary(fromLibInfo);
        }
        
        Library toLib = libraryMapper.selectById(transfer.getToLibraryId());
        if (toLib != null) {
            TransferProgressDTO.LibraryInfo toLibInfo = new TransferProgressDTO.LibraryInfo();
            toLibInfo.setId(toLib.getId());
            toLibInfo.setName(toLib.getName());
            toLibInfo.setLocation(toLib.getLocationDesc());
            dto.setToLibrary(toLibInfo);
        }
        
        //时间信息
        TransferProgressDTO.TimeInfo timeInfo = new TransferProgressDTO.TimeInfo();
        timeInfo.setRequestTime(transfer.getRequestTime());
        timeInfo.setEstimatedArrivalTime(transfer.getEstimatedArrivalTime());
        timeInfo.setActualArrivalTime(transfer.getActualArrivalTime());
        
        //计算已用时长
        if (transfer.getRequestTime() != null) {
            Duration usedDuration = Duration.between(transfer.getRequestTime(), LocalDateTime.now());
            timeInfo.setUsedDuration(formatDuration(usedDuration));
        }
        
        //计算剩余时长
        if (transfer.getEstimatedArrivalTime() != null && "IN_TRANSIT".equals(transfer.getStatus())) {
            Duration remainingDuration = Duration.between(LocalDateTime.now(), transfer.getEstimatedArrivalTime());
            if (!remainingDuration.isNegative()) {
                timeInfo.setRemainingDuration("预计" + formatDuration(remainingDuration));
            } else {
                timeInfo.setRemainingDuration("已超时");
            }
        }
        
        dto.setTimeInfo(timeInfo);
        
        return dto;
    }

    //构建批量调拨进度DTO
    private BatchTransferProgressDTO buildBatchTransferProgressDTO(TransferOrder order) {
        BatchTransferProgressDTO dto = new BatchTransferProgressDTO();
        
        //基本信息
        dto.setOrderId(order.getId());
        dto.setBiblioId(order.getBiblioId());
        dto.setStatus(order.getStatus());
        dto.setStatusText(getStatusText(order.getStatus()));
        
        //查询书目信息
        BookBiblio biblio = biblioMapper.selectById(order.getBiblioId());
        if (biblio != null) {
            TransferProgressDTO.BookInfo bookInfo = new TransferProgressDTO.BookInfo();
            bookInfo.setTitle(biblio.getTitle());
            bookInfo.setAuthor(biblio.getAuthor());
            dto.setBookInfo(bookInfo);
        }
        
        //查询馆信息
        Library fromLib = libraryMapper.selectById(order.getFromLibraryId());
        if (fromLib != null) {
            TransferProgressDTO.LibraryInfo fromLibInfo = new TransferProgressDTO.LibraryInfo();
            fromLibInfo.setId(fromLib.getId());
            fromLibInfo.setName(fromLib.getName());
            dto.setFromLibrary(fromLibInfo);
        }
        
        Library toLib = libraryMapper.selectById(order.getToLibraryId());
        if (toLib != null) {
            TransferProgressDTO.LibraryInfo toLibInfo = new TransferProgressDTO.LibraryInfo();
            toLibInfo.setId(toLib.getId());
            toLibInfo.setName(toLib.getName());
            dto.setToLibrary(toLibInfo);
        }
        
        //查询所有调拨记录
        List<BookTransfer> transfers = transferMapper.selectList(
            new LambdaQueryWrapper<BookTransfer>()
                .eq(BookTransfer::getOrderId, order.getId())
        );
        
        //统计进度信息
        BatchTransferProgressDTO.ProgressInfo progressInfo = new BatchTransferProgressDTO.ProgressInfo();
        progressInfo.setTotalQuantity(order.getPlannedQuantity());
        
        int completedCount = 0;
        int inTransitCount = 0;
        int pendingCount = 0;
        
        for (BookTransfer transfer : transfers) {
            if ("COMPLETED".equals(transfer.getStatus())) {
                completedCount++;
            } else if ("IN_TRANSIT".equals(transfer.getStatus())) {
                inTransitCount++;
            } else if ("PENDING".equals(transfer.getStatus())) {
                pendingCount++;
            }
        }
        
        progressInfo.setCompletedQuantity(completedCount);
        progressInfo.setInTransitQuantity(inTransitCount);
        progressInfo.setPendingQuantity(pendingCount);
        
        //计算整体进度百分比
        if (progressInfo.getTotalQuantity() > 0) {
            int percentage = (completedCount * 100) / progressInfo.getTotalQuantity();
            progressInfo.setProgressPercentage(percentage);
        } else {
            progressInfo.setProgressPercentage(0);
        }
        
        dto.setProgressInfo(progressInfo);
        
        //时间信息
        BatchTransferProgressDTO.BatchTimeInfo timeInfo = new BatchTransferProgressDTO.BatchTimeInfo();
        timeInfo.setCreateTime(order.getCreateTime());
        timeInfo.setCompleteTime(order.getCompleteTime());
        
        if (order.getCreateTime() != null) {
            Duration usedDuration = Duration.between(order.getCreateTime(), LocalDateTime.now());
            timeInfo.setUsedDuration(formatDuration(usedDuration));
        }
        
        dto.setTimeInfo(timeInfo);
        
        //调拨记录列表
        List<BatchTransferProgressDTO.TransferRecordInfo> recordInfos = transfers.stream()
            .map(transfer -> {
                BatchTransferProgressDTO.TransferRecordInfo recordInfo = new BatchTransferProgressDTO.TransferRecordInfo();
                recordInfo.setTransferId(transfer.getId());
                recordInfo.setCopyId(transfer.getCopyId());
                recordInfo.setStatus(transfer.getStatus());
                recordInfo.setStatusText(getStatusText(transfer.getStatus()));
                recordInfo.setProgressPercentage(calculateProgressPercentage(transfer));
                return recordInfo;
            })
            .collect(Collectors.toList());
        
        dto.setTransferRecords(recordInfos);
        
        return dto;
    }

    //计算进度百分比
    private Integer calculateProgressPercentage(BookTransfer transfer) {
        if ("PENDING".equals(transfer.getStatus())) {
            return 0;
        } else if ("COMPLETED".equals(transfer.getStatus())) {
            return 100;
        } else if ("CANCELED".equals(transfer.getStatus())) {
            return 0;
        } else if ("IN_TRANSIT".equals(transfer.getStatus())) {
            //根据时间计算进度
            if (transfer.getRequestTime() != null && transfer.getEstimatedArrivalTime() != null) {
                Duration totalDuration = Duration.between(transfer.getRequestTime(), transfer.getEstimatedArrivalTime());
                Duration usedDuration = Duration.between(transfer.getRequestTime(), LocalDateTime.now());
                
                long totalSeconds = totalDuration.getSeconds();
                long usedSeconds = usedDuration.getSeconds();
                
                if (totalSeconds > 0) {
                    int percentage = (int) ((usedSeconds * 100) / totalSeconds);
                    //最大95%,到达后才会变为100%
                    return Math.min(percentage, 95);
                }
            }
            return 50; //默认50%
        }
        return 0;
    }

    //获取状态文本
    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "待处理";
            case "IN_TRANSIT":
                return "运输中";
            case "COMPLETED":
                return "已完成";
            case "CANCELED":
                return "已取消";
            default:
                return "未知";
        }
    }

    //获取调拨原因文本
    private String getTransferReasonText(String reason) {
        if ("USER_REQUEST".equals(reason)) {
            return "用户请求";
        } else if ("INVENTORY_BALANCE".equals(reason)) {
            return "库存平衡";
        }
        return "未知";
    }

    //格式化时长
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        
        if (hours > 24) {
            long days = hours / 24;
            long remainHours = hours % 24;
            return days + "天" + remainHours + "小时";
        } else if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }
}
