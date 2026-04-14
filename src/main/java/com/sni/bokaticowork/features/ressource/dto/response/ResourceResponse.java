package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceResponse {

    private String code;
    private String typeCode;
    private String groupCode;
    private String policyCode;
    private String name;
    private String description;
    private Integer capacity;
    private String zone;
    private String locationLabel;
    private String status;
    private Boolean bookingEnabled;
    private Boolean portalVisible;
    private Integer displayOrder;
    private Boolean active;
}
