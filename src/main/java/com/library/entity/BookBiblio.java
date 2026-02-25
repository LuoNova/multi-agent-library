package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

//书目信息表
@Data
@TableName("tb_book_biblio")
public class BookBiblio {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //ISBN
    private String isbn;

    //书名
    private String title;

    //作者
    private String author;

    //出版社
    private String publisher;

    //出版日期
    private LocalDate publishDate;

    //分类
    private String category;

    //累计总借阅次数(全馆)
    private Integer totalBorrowCount;

    //近30天借阅次数
    private Integer monthlyBorrowCount;

    //创建时间
    private LocalDateTime createTime;
}