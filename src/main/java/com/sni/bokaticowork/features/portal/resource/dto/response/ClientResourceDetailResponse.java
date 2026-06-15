package com.sni.bokaticowork.features.portal.resource.dto.response;

import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClientResourceDetailResponse {

    private String code;
    private String name;
    private String description;
    private String typeCode;
    private String typeName;
    private String groupCode;
    private String groupName;
    private Integer capacity;
    private String zone;
    private String locationLabel;
    private Integer displayOrder;

    // Policy info
    private Integer minBookingDurationMinutes;
    private Integer maxBookingDurationMinutes;
    private Integer minBookingNoticeMinutes;
    private Integer cancellationNoticeMinutes;
    private Boolean allowCancellation;

    private List<AmenityResponse> amenities;
    private List<ResourcePricingRuleResponse> pricingRules;
}
