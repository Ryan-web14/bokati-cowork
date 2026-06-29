package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class WorkflowResponse {
    private String code;
    private String name;
    private String description;
    private String workflowType;
    private DocumentSpace space;
    private String documentTypeCode;
    private Boolean active;
    private Instant createdAt;
    private List<StepResponse> steps;

    @Data @Builder
    public static class StepResponse {
        private Long id;
        private Integer stepOrder;
        private String stepName;
        private String approverType;
        private String approverValue;
        private Boolean required;
        private Integer autoApproveDays;
    }
}
