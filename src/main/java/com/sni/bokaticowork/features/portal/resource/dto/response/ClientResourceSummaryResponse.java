package com.sni.bokaticowork.features.portal.resource.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClientResourceSummaryResponse {

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
    private List<String> amenityNames;
    private Integer baseHourlyPrice;
}
