package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LinkAmenityToResourceRequest {

    @NotBlank
    private String resourceCode;

    @NotBlank
    private String amenityCode;

    private Integer quantity;
    private Boolean optional;
    private Integer extraPrice;
}
