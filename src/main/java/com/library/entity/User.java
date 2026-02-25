package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//用户表
@Data
@TableName("tb_user")
public class User {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //学号/工号
    private String studentNo;

    //姓名
    private String name;

    //手机号
    private String phone;

    //邮箱
    private String email;

    //最大可借册数
    private Integer maxBorrowCount;

    //当前借阅中数量
    private Integer currentBorrowCount;

    //信用分(预约违规扣分)
    private Integer creditScore;

    //状态:ACTIVE-正常,FROZEN-冻结
    private String status;

    //常用馆ID
    private Long preferredLibraryId;

    //创建时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;
}