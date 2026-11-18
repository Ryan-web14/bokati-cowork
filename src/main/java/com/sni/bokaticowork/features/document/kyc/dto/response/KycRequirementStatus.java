package com.sni.bokaticowork.features.document.kyc.dto.response;

import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycRequirementStatus {
    private String documentTypeCode;
    private String documentTypeName;
    private boolean required;
    private Boolean requiresBackSide;
    private KycDocumentVerificationStatus status;
    private String documentCode;
    private String backDocumentCode;
    /** Groupe d'alternatives de la piece (OU) et son libelle ; nuls pour une exigence seule. */
    private String alternativeGroup;
    private String alternativeGroupLabel;
    /** Une autre piece du meme groupe remplit deja l'exigence : celle-ci n'est plus attendue. */
    private boolean satisfiedByAlternative;
}
