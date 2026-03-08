package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.TransferOrder;
import org.apache.ibatis.annotations.Mapper;

//调拨单Mapper
@Mapper
public interface TransferOrderMapper extends BaseMapper<TransferOrder> {
}
