package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.RepairTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

//故障报修Mapper(新增)
@Mapper
public interface RepairTicketMapper extends BaseMapper<RepairTicket> {

    //查某设备的报修记录
    @Select("SELECT * FROM tb_repair_ticket WHERE equipment_id = #{equipmentId} ORDER BY report_time DESC")
    List<RepairTicket> selectByEquipmentId(Long equipmentId);

    //查某状态工单(用于Agent监控)
    @Select("SELECT * FROM tb_repair_ticket WHERE status = #{status}")
    List<RepairTicket> selectByStatus(String status);

    //查某馆的待处理报修(关联设备表查)
    @Select("SELECT rt.* FROM tb_repair_ticket rt JOIN tb_equipment e ON rt.equipment_id = e.id WHERE e.library_id = #{libraryId} AND rt.status = 'PENDING'")
    List<RepairTicket> selectPendingByLibrary(Long libraryId);
}