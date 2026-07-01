package com.sni.bokaticowork.features.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProposeAmendmentRequest {

    @NotBlank(message = "La description de l'avenant est requise")
    private String description;

    private LocalDate effectiveDate;
}
