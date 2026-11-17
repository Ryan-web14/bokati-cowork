package com.sni.bokaticowork.features.document.kyc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycCrossValidationRuleResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private com.sni.bokaticowork.features.document.kyc.enums.KycCrossRuleType ruleType;
    private String parameter;
    /** Regle preenregistree : modifiable et desactivable, pas supprimable. */
    private Boolean systemRule;
    private String documentTypeCode1;
    private String documentTypeCode2;
    private String fieldToCompare;
    private Boolean blocking;
    private Boolean active;
}
