package com.library.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.library.entity.LibraryBiblioStats;
import com.library.mapper.LibraryBiblioStatsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

//馆藏书目统计服务（修正版：只操作实际存在的字段）
@Service
public class LibraryBiblioStatsService {

    @Autowired
    private LibraryBiblioStatsMapper statsMapper;

    //根据馆ID和书目ID查询统计
    public LibraryBiblioStats getStats(Long libraryId, Long biblioId) {
        return statsMapper.selectByLibraryAndBiblio(libraryId, biblioId);
    }

    //调拨出库：源馆库存减1
    public boolean decrementStock(Long libraryId, Long biblioId) {
        LambdaUpdateWrapper<LibraryBiblioStats> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LibraryBiblioStats::getLibraryId, libraryId)
                .eq(LibraryBiblioStats::getBiblioId, biblioId)
                .setSql("stock_count = stock_count - 1");
        return statsMapper.update(null, wrapper) > 0;
    }

    //调拨入库：目标馆库存加1
    public boolean incrementStock(Long libraryId, Long biblioId) {
        LambdaUpdateWrapper<LibraryBiblioStats> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LibraryBiblioStats::getLibraryId, libraryId)
                .eq(LibraryBiblioStats::getBiblioId, biblioId)
                .setSql("stock_count = stock_count + 1");
        return statsMapper.update(null, wrapper) > 0;
    }

    //初始化统计记录（如果不存在）
    public void initStatsIfNotExists(Long libraryId, Long biblioId) {
        LibraryBiblioStats exist = getStats(libraryId, biblioId);
        if (exist == null) {
            LibraryBiblioStats stats = new LibraryBiblioStats();
            stats.setLibraryId(libraryId);
            stats.setBiblioId(biblioId);
            stats.setStockCount(0);
            stats.setBorrowCount30d(0);
            stats.setReservationPendingCount(0);
            stats.setLastCalculatedTime(LocalDateTime.now());
            statsMapper.insert(stats);
        }
    }
}