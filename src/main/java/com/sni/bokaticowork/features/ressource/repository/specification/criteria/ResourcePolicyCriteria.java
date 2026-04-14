package com.sni.bokaticowork.features.ressource.repository.specification.criteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourcePolicyCriteria {

    private String code;
    private String name;
    private Boolean allowCancellation;
    private Boolean active;
}
