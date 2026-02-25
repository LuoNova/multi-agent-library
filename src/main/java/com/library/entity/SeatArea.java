package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

//座位区域表
@Data
@TableName("tb_seat_area")
public class SeatArea {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //所属馆ID
    private Long libraryId;

    //区域名称(如:3楼自习区)
    private String name;

    //楼层
    private Integer floor;

    //座位总数
    private Integer seatCount;

    //开放时间
    private String openTime;

    //是否有电源(0-无,1-有)
    private Integer hasPower;

    //状态:OPEN/CLOSED/MAINTAIN
    private String status;
}