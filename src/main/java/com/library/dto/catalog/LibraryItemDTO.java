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
@Schema(description = "图书馆简要信息")
public class LibraryItemDTO {

    @Schema(description = "馆ID")
    private Long id;

    @Schema(description = "馆名")
    private String name;
}
