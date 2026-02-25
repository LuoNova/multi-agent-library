package com.library.service;

import com.library.entity.Library;
import com.library.mapper.LibraryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

//馆信息服务（提供距离计算等）
@Service
public class LibraryService {

    @Autowired
    private LibraryMapper libraryMapper;

    //根据ID获取馆信息
    public Library getById(Long id) {
        return libraryMapper.selectById(id);
    }

    //计算两馆之间的距离（简化版：实际应基于经纬度计算）
    //返回单位：公里
    public double calculateDistance(Long fromLibraryId, Long toLibraryId) {
        // TODO 实际项目中应根据tb_library的latitude/longitude计算
        //这里模拟：理科馆(1)到文科馆(2)距离0.8公里
        if (fromLibraryId.equals(toLibraryId)) {
            return 0.0;
        }
        //简单模拟：id差值*0.8公里
        return Math.abs(fromLibraryId - toLibraryId) * 0.8;
    }
}