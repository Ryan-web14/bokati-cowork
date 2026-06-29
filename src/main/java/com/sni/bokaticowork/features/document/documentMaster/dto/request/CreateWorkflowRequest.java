package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateWorkflowRequest {
    @NotBlank private String name;
    private String description;
    private String workflowType;
    private DocumentSpace space;
    private String documentTypeCode;
    @Valid @NotEmpty private List<WorkflowStepRequest> steps;

    @Data
    public static class WorkflowStepRequest {
        @NotBlank private String stepName;
        @NotBlank private String approverType;
        @NotBlank private String approverValue;
        private Boolean required;
        private Integer autoApproveDays;
    }
}
