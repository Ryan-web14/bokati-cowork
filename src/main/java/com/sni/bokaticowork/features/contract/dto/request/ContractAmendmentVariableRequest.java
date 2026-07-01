package com.sni.bokaticowork.features.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContractAmendmentVariableRequest {

    @NotBlank(message = "La clé de variable est requise")
    private String variableKey;

    /** Valeur actuelle dans le contrat original (pour la piste d'audit) */
    private String previousValue;

    @NotBlank(message = "La nouvelle valeur est requise")
    private String newValue;
}
