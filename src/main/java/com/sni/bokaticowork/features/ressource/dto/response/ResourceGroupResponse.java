package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceGroupResponse {

    private String code;
    private String name;
    private String description;
    private Boolean portalVisible;
    private Boolean active;
}
