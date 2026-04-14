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
public class CreateResourcePricingRuleRequest {

    @NotBlank
    private String resourceCode;

    @NotBlank
    private String bookingUnit;

    @NotNull
    private Integer price;

    private Boolean active;
}
