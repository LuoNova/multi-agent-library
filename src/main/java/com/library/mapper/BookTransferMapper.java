package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.BookTransfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

//调拨记录Mapper
@Mapper
public interface BookTransferMapper extends BaseMapper<BookTransfer> {

    //查某馆待处理的调拨请求
    @Select("SELECT * FROM tb_book_transfer WHERE from_library_id = #{libraryId} AND status = 'PENDING'")
    List<BookTransfer> selectPendingByFromLibrary(Long libraryId);

    //查某馆作为目标的调拨
    @Select("SELECT * FROM tb_book_transfer WHERE to_library_id = #{libraryId} AND status = 'IN_TRANSIT'")
    List<BookTransfer> selectInTransitToLibrary(Long libraryId);
}