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

    /** Lot R4 : filtres par code (les identifiants restent acceptes). */
    private String typeCode;

    private String groupCode;

    private String policyCode;

    private String name;

    private String zone;

    private String locationLabel;

    /** Lot R5b : filtre par site et par etage. */
    private String siteCode;

    private String floorCode;

    private ResourceStatus status;

    private Boolean portalVisible;

    private Boolean bookingEnabled;

    private Boolean active;

    private Integer minCapacity;

    /**
     * Lot B10 : si renseigne, les ressources reservees (RESTRICTED) ne figurent que si leur
     * identifiant est dans cette liste (celles visibles par le client du portail).
     */
    private java.util.List<Long> restrictedVisibleIds;

}
