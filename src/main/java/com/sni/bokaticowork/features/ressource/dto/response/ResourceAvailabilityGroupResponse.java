package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceAvailabilityGroupResponse {

    private String resourceCode;
    private String resourceName;
    private List<ResourceAvailabilityResponse> availabilities;
}
