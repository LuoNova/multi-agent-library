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

    //根据副本ID、用户ID和状态查询借阅记录（用于调拨完成时查找）
    @Select("SELECT * FROM tb_book_borrow WHERE copy_id = #{copyId} AND user_id = #{userId} AND status = #{status} LIMIT 1")
    BookBorrow selectByCopyIdAndUserAndStatus(@Param("copyId") Long copyId,
                                              @Param("userId") Long userId,
                                              @Param("status") String status);

    //检查用户是否已有该书的活跃借阅（TRANSFERRING/RESERVED/BORROWING）
    @Select("SELECT COUNT(*) > 0 FROM tb_book_borrow WHERE user_id = #{userId} AND " +
            "EXISTS(SELECT 1 FROM tb_book_copy c WHERE c.id = copy_id AND c.biblio_id = #{biblioId}) " +
            "AND status IN ('TRANSFERRING', 'RESERVED', 'BORROWING')")
    boolean hasActiveBorrow(@Param("userId") Long userId, @Param("biblioId") Long biblioId);
}