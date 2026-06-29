package com.sni.bokaticowork.features.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVariableDefinition implements Serializable {

    private String key;
    private String label;
    private String type;
    private boolean required;
    private String defaultValue;
    private String source;
    private String description;
}
