package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.contract.model.TemplateVariableDefinition;
import lombok.Data;

import java.util.List;

@Data
public class UpdateContractTemplateRequest {

    private String name;

    private String description;

    private String language;

    private String category;

    private String htmlContent;

    private String cssContent;

    private String headerHtml;

    private String footerHtml;

    private List<TemplateVariableDefinition> variableDefinitions;

    private Boolean active;
}
