package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.BookReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//图书预约Mapper
@Mapper
public interface BookReservationMapper extends BaseMapper<BookReservation> {

    //查某书在某馆的待处理预约数(库存压力评估)
    @Select("SELECT COUNT(*) FROM tb_book_reservation WHERE biblio_id = #{biblioId} AND pickup_library_id = #{libraryId} AND status = 'PENDING'")
    Integer countPendingByBiblioAndLibrary(@Param("biblioId") Long biblioId,
                                           @Param("libraryId") Long libraryId);

    //查用户预约列表
    @Select("SELECT * FROM tb_book_reservation WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<BookReservation> selectByUserId(Long userId);

    //查询某书目最早的一条待处理预约（按预约时间升序）
    @Select("SELECT * FROM tb_book_reservation " +
            "WHERE biblio_id = #{biblioId} AND status = 'PENDING' " +
            "ORDER BY reserve_time ASC LIMIT 1")
    BookReservation selectFirstPendingByBiblio(@Param("biblioId") Long biblioId);

    //查询用户的有效预约数（可选，用于业务校验）
    @Select("SELECT COUNT(*) FROM tb_book_reservation " +
            "WHERE user_id = #{userId} AND status = 'PENDING'")
    int countPendingByUser(@Param("userId") Long userId);

    //根据副本ID和状态查询预约记录（用于调拨回调时查找关联预约）
    @Select("SELECT * FROM tb_book_reservation " +
            "WHERE copy_id = #{copyId} AND status = #{status} LIMIT 1")
    BookReservation selectByCopyIdAndStatus(@Param("copyId") Long copyId,
                                            @Param("status") String status);
}