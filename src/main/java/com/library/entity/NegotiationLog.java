package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//协商过程日志表(用于论文展示算法效率)
@Data
@TableName("tb_negotiation_log")
public class NegotiationLog {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //协商任务ID
    private String taskId;

    //任务类型:BOOK_BORROW/SEAT_RESERVE
    private String taskType;

    //发起者Agent
    private String initiatorAgent;

    //参与者Agent
    private String participantAgent;

    //消息类型:CFP/PROPOSE/ACCEPT/REJECT
    private String messageType;

    //消息内容(JSON)
    private String contentJson;

    //创建时间
    private LocalDateTime createTime;
}