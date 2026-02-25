package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.BookBorrow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

//借阅记录Mapper(新增)
@Mapper
public interface BookBorrowMapper extends BaseMapper<BookBorrow> {
    //查询用户未归还的某书目数量（防止重复借同一本书）
    @Select("SELECT COUNT(*) FROM tb_book_borrow bb " +
            "JOIN tb_book_copy bc ON bb.copy_id = bc.id " +
            "WHERE bb.user_id = #{userId} " +
            "AND bc.biblio_id = #{biblioId} " +
            "AND bb.status IN ('BORROWING', 'RESERVED')")
    int countUnreturnedByUserAndBiblio(@Param("userId") Long userId,
                                       @Param("biblioId") Long biblioId);
}