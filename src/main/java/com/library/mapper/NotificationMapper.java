package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知记录Mapper接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
