package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceRequest {

    @NotNull
    private String typeId;

    private String groupId;

    private String policyId;

    @NotBlank
    private String name;
    private String description;

    private Integer capacity;

    private String zone;
    private String locationLabel;

    private String status;

    private Boolean bookingEnabled;
    private Boolean portalVisible;

}
