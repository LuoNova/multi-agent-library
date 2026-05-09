package com.library.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "座位简要")
public class SeatItemDTO {

    @Schema(description = "座位ID")
    private Long id;

    @Schema(description = "区域ID")
    private Long areaId;

    @Schema(description = "座位号")
    private String seatNo;

    @Schema(description = "是否有电源 0/1")
    private Integer hasPower;

    @Schema(description = "状态")
    private String status;
}
