package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

//座位Mapper
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    //查某区域的可用座位
    @Select("SELECT * FROM tb_seat WHERE area_id = #{areaId} AND status = 'AVAILABLE'")
    List<Seat> selectAvailableByArea(Long areaId);

    //修改座位状态
    @Update("UPDATE tb_seat SET status = #{status} WHERE id = #{seatId}")
    int updateStatus(@Param("seatId") Long seatId, @Param("status") String status);
}