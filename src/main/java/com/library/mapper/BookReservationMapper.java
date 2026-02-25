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
}