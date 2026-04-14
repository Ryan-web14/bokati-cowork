package com.sni.bokaticowork.features.ressource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateResourceRequest {

    private String name;

    private String description;

    private Integer capacity;

    private String zone;

    private String floorLabel;

    private String locationLabel;

    private Boolean bookingEnabled;

    private Boolean portalVisible;

}
