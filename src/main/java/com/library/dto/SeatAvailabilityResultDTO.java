package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

//座位可用性查询结果DTO
@Data
@Schema(description = "座位可用性查询结果")
public class SeatAvailabilityResultDTO {

    @Schema(description = "可用座位总数(列表模式)")
    private Long total;

    @Schema(description = "可用座位列表(列表模式)")
    private List<SeatInfoDTO> seats;

    @Schema(description = "满足条件的座位总数(自动分配模式)")
    private Long availableTotal;

    @Schema(description = "系统自动分配的推荐座位(自动分配模式)")
    private SeatInfoDTO assignedSeat;
}

