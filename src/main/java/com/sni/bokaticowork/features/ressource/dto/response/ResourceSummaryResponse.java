package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceSummaryResponse {

    private String code;
    private String typeCode;
    private String name;
    private String status;
    private Boolean bookingEnabled;
    private Boolean portalVisible;
    private Boolean active;
}
