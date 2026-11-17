package com.sni.bokaticowork.features.document.kyc.dto.request;

import com.sni.bokaticowork.features.document.kyc.enums.KycCrossRuleType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Regle de controle croise. Champs exiges selon {@code ruleType} (FIELD_MATCH par defaut, comme avant) :
 * FIELD_MATCH : {@code documentTypeCode1}, {@code documentTypeCode2}, {@code fieldToCompare} ;
 * DECLARED_VS_OCR et PROFILE_MATCH : {@code fieldToCompare} ; MIN_VALIDITY : {@code parameter} (jours).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycCrossValidationRuleRequest {

    @Size(max = 80)
    private String code;

    @Size(max = 200)
    private String name;

    private String description;

    private KycCrossRuleType ruleType;

    @Size(max = 1000)
    private String documentTypeCode1;

    @Size(max = 1000)
    private String documentTypeCode2;

    @Size(max = 80)
    private String fieldToCompare;

    @Size(max = 200)
    private String parameter;

    private Boolean blocking;

    private Boolean active;
}
