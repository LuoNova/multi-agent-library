package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.TransferSuggestion;
import org.apache.ibatis.annotations.Mapper;

//调拨建议Mapper
@Mapper
public interface TransferSuggestionMapper extends BaseMapper<TransferSuggestion> {
}
