package com.sni.bokaticowork.features.ressource.repository.specification.criteria;

import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceSearchCriteria {
    private String code;

    private Long typeId;

    private Long  groupId;

    private Long policyId;

    private String name;

    private String zone;

    private String locationLabel;

    private ResourceStatus status;

    private Boolean portalVisible;

    private Boolean bookingEnabled;

    private Boolean active;

    private Integer minCapacity;

}
