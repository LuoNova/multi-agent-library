package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//馆信息表
@Data
@TableName("tb_library")
public class Library {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //馆名(理科馆/文科馆)
    private String name;

    //地理位置描述
    private String locationDesc;

    //开放时间(如08:00-22:00)
    private String openTime;

    //创建时间
    private LocalDateTime createTime;
}