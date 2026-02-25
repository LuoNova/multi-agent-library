package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.SeatArea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

//座位区域Mapper
@Mapper
public interface SeatAreaMapper extends BaseMapper<SeatArea> {

    //查某馆的所有区域
    @Select("SELECT * FROM tb_seat_area WHERE library_id = #{libraryId}")
    List<SeatArea> selectByLibraryId(Long libraryId);
}