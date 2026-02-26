package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//馆藏书目统计表(智能体决策依据)
@Data
@TableName("tb_library_biblio_stats")
public class LibraryBiblioStats {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //馆ID
    private Long libraryId;

    //书目ID
    private Long biblioId;

    //当前库存副本数
    private Integer stockCount;

    //近30天借出次数
    @TableField("borrow_count_30d")
    private Integer borrowCount30d;

    //当前排队预约人数
    private Integer reservationPendingCount;

    //平均借阅天数
    private BigDecimal avgBorrowDuration;

    //统计更新时间
    private LocalDateTime lastCalculatedTime;
}