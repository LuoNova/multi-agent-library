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
@Schema(description = "座位区域简要")
public class SeatAreaItemDTO {

    @Schema(description = "区域ID")
    private Long id;

    @Schema(description = "所属馆ID")
    private Long libraryId;

    @Schema(description = "区域名称")
    private String name;

    @Schema(description = "楼层")
    private Integer floor;
}
