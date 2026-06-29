package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class WorkflowInstanceResponse {
    private Long id;
    private String workflowCode;
    private String workflowName;
    private String documentCode;
    private String documentTitle;
    private Integer currentStep;
    private String currentStepName;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private List<ActionResponse> actions;

    @Data @Builder
    public static class ActionResponse {
        private Integer stepOrder;
        private String stepName;
        private String action;
        private Long actorId;
        private String actorEmail;
        private Instant actedAt;
        private String comment;
    }
}
