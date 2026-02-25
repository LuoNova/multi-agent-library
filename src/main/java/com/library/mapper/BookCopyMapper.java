package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.BookCopy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

//图书副本Mapper(调拨核心)
@Mapper
public interface BookCopyMapper extends BaseMapper<BookCopy> {

    //查某馆某书目的可用副本
    @Select("SELECT * FROM tb_book_copy WHERE biblio_id = #{biblioId} " +
            "AND library_id = #{libraryId} AND status = 'AVAILABLE'")
    List<BookCopy> selectAvailableByBiblioAndLibrary(@Param("biblioId") Long biblioId,
                                                     @Param("libraryId") Long libraryId);

    //执行调拨(修改所在馆)
    @Update("UPDATE tb_book_copy SET library_id = #{toLibraryId}, status = 'IN_TRANSIT' WHERE id = #{copyId}")
    int updateForTransfer(@Param("copyId") Long copyId, @Param("toLibraryId") Long toLibraryId);

    //按状态统计数量
    @Select("SELECT COUNT(*) FROM tb_book_copy WHERE library_id = #{libraryId} " +
            "AND status = #{status}")
    Integer countByLibraryAndStatus(@Param("libraryId") Long libraryId,
                                    @Param("status") String status);
}