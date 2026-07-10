package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.contract.enums.ContractAmendmentSectionAction;
import com.sni.bokaticowork.features.contract.enums.ContractSectionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContractAmendmentSectionRequest {

    @NotNull(message = "L'action (ADD, MODIFY, REMOVE) est requise")
    private ContractAmendmentSectionAction action;

    /** Pour MODIFY et REMOVE : titre ou référence de la section originale ciblée */
    private String targetSectionRef;

    private ContractSectionType sectionType;

    /** Nouveau titre (pour ADD et MODIFY) */
    private String title;

    /** Nouveau contenu · supporte les tokens {{variable}} (pour ADD et MODIFY) */
    private String content;

    /** Position dans l'avenant (0-based) */
    private Integer sectionOrder;
}
