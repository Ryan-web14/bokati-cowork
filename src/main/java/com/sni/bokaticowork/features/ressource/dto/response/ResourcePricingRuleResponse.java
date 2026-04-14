package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourcePricingRuleResponse {

    private Long id;
    private String resourceCode;
    private String bookingUnit;
    private Integer price;
    private Boolean active;
}
