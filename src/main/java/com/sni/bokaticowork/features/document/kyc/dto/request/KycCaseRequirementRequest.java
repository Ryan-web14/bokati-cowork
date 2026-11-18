package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycCaseRequirementRequest {

    @NotBlank
    private String documentTypeCode;

    private String documentTypeName;

    private Boolean required;

    /** Groupe d'alternatives : meme groupe = OU (une piece suffit) ; vide = exigence seule. Chaine vide pour le retirer. */
    private String alternativeGroup;
    /** Libelle du groupe pour le titulaire. */
    private String alternativeGroupLabel;

}
