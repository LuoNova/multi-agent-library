package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//图书副本表(调拨操作此表)
@Data
@TableName("tb_book_copy")
public class BookCopy {
    //主键ID(图书条码号)
    @TableId(type = IdType.AUTO)
    private Long id;

    //书目ID
    private Long biblioId;

    //当前所在馆ID(调拨即修改此字段)
    private Long libraryId;

    //精确位置(如3楼A区12架)
    private String location;

    //状态:AVAILABLE/BORROWED/RESERVED/LOST/DAMAGED
    private String status;

    //在本馆累计被借次数
    private Integer localBorrowCount;

    //上次被借时间(用于计算闲置)
    private LocalDateTime lastBorrowTime;

    //创建时间
    private LocalDateTime createTime;

    //最后更新时间
    private LocalDateTime updateTime;
}