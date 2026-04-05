package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//设备表(新增)
@Data
@TableName("tb_equipment")
public class Equipment {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //所在馆ID
    private Long libraryId;

    //所属区域(tb_seat_area.id)，可空
    private Long areaId;

    //设备类型:自助借还机/书架/电脑/空调等
    private String type;

    //设备名称/编号
    private String name;

    //具体位置描述
    private String location;

    //状态:NORMAL/FAULT/MAINTAIN/DISABLED
    private String status;

    //备注
    private String remark;

    //安装时间
    private LocalDateTime installTime;

    //创建时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;
}