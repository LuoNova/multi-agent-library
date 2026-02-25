package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//座位表
@Data
@TableName("tb_seat")
public class Seat {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //区域ID
    private Long areaId;

    //座位编号(如:A-01)
    private String seatNo;

    //是否有独立电源(0-无,1-有)
    private Integer hasPower;

    //状态:AVAILABLE/OCCUPIED/DISABLED
    private String status;

    //创建时间
    private LocalDateTime createTime;
}