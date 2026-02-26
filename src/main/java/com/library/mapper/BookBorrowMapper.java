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

    //查询副本当前的未归还借阅记录（用于还书时校验）
    @Select("SELECT * FROM tb_book_borrow " +
            "WHERE copy_id = #{copyId} AND status = 'BORROWING' " +
            "ORDER BY id DESC LIMIT 1")
    BookBorrow selectCurrentBorrowByCopyId(@Param("copyId") Long copyId);
}