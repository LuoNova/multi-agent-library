package com.library.dto.fault;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "批量资源健康查询响应")
public class FaultHealthQueryResponse {

    private List<FaultHealthItemVO> results = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单个资源健康结果")
    public static class FaultHealthItemVO {
        private String resourceType;
        private Long resourceId;
        private boolean available;
        private boolean hasCriticalFault;
        private String faultSummary;
    }
}
