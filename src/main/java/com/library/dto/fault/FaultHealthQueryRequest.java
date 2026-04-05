package com.library.dto.fault;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "批量资源健康查询请求")
public class FaultHealthQueryRequest {

    @Schema(description = "资源列表，可为空数组")
    private List<FaultHealthResourceRef> resources = new ArrayList<>();

    @Data
    @Schema(description = "单个资源引用")
    public static class FaultHealthResourceRef {
        @Schema(description = "LIBRARY / SEAT_AREA / SEAT / EQUIPMENT", example = "SEAT")
        private String resourceType;
        @Schema(description = "资源主键")
        private Long resourceId;
    }
}
