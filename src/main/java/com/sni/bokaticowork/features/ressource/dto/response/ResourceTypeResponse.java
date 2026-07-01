package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceTypeResponse {

    private String code;
    private String name;
    private String description;
    private Boolean active;
    private Integer bookableSlots;
}
