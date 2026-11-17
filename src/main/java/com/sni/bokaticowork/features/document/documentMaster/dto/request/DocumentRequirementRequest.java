package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentRequirementRequest {

    @NotNull
    private DocumentOwnerType ownerType;

    @NotBlank
    private String documentTypeCode;

    private String documentTypeName;

    private String customerType;

    private String businessLegalForm;

    private Boolean required;

    private Boolean active;

    /** Niveau KYC a partir duquel la piece est demandee (1 a 4, 1 par defaut). */
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(4)
    private Integer kycLevel;
}
