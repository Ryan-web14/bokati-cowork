package com.sni.bokaticowork.features.contract.dto.response;

import com.sni.bokaticowork.features.contract.model.TemplateVariableDefinition;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ContractTemplateResponse {
    private String code;
    private String name;
    private String description;
    private String language;
    private Integer version;
    private String category;
    private String htmlContent;
    private String cssContent;
    private String headerHtml;
    private String footerHtml;
    private List<TemplateVariableDefinition> variableDefinitions;
    private List<ContractTemplateSectionResponse> sections;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
