package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.Equipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

//设备Mapper(新增)
@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {

    //查某馆的所有设备
    @Select("SELECT * FROM tb_equipment WHERE library_id = #{libraryId}")
    List<Equipment> selectByLibraryId(Long libraryId);

    //查某状态的设备(用于故障统计)
    @Select("SELECT * FROM tb_equipment WHERE status = #{status}")
    List<Equipment> selectByStatus(String status);
}