package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.LibraryBiblioStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

//馆藏书目统计Mapper(智能体决策)
@Mapper
public interface LibraryBiblioStatsMapper extends BaseMapper<LibraryBiblioStats> {

    //查特定馆的书目统计(投标决策)
    @Select("SELECT * FROM tb_library_biblio_stats WHERE library_id = #{libraryId} AND biblio_id = #{biblioId}")
    LibraryBiblioStats selectByLibraryAndBiblio(@Param("libraryId") Long libraryId,
                                                @Param("biblioId") Long biblioId);

    //增加预约排队人数
    @Update("UPDATE tb_library_biblio_stats SET reservation_pending_count = reservation_pending_count + 1 WHERE library_id = #{libraryId} AND biblio_id = #{biblioId}")
    int incrementPendingCount(@Param("libraryId") Long libraryId, @Param("biblioId") Long biblioId);
}