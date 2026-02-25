package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.BookBiblio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//书目信息Mapper
@Mapper
public interface BookBiblioMapper extends BaseMapper<BookBiblio> {

    //根据分类查热门书(用于智能体推荐)
    @Select("SELECT * FROM tb_book_biblio WHERE category = #{category} " +
            "ORDER BY monthly_borrow_count DESC LIMIT #{limit}")
    List<BookBiblio> selectHotByCategory(@Param("category") String category,
                                         @Param("limit") Integer limit);
}