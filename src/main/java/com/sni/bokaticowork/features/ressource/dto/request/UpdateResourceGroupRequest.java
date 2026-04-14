package com.sni.bokaticowork.features.ressource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateResourceGroupRequest {

    private String name;
    private String description;
    private Boolean portalVisible;
    private Boolean active;
}
