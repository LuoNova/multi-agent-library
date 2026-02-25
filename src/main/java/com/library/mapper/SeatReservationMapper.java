package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.SeatReservation;
import org.apache.ibatis.annotations.Mapper;

//座位预约Mapper
@Mapper
public interface SeatReservationMapper extends BaseMapper<SeatReservation> {
}