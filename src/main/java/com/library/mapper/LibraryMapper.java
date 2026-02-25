package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.Library;
import org.apache.ibatis.annotations.Mapper;

//馆信息Mapper
@Mapper
public interface LibraryMapper extends BaseMapper<Library> {
}