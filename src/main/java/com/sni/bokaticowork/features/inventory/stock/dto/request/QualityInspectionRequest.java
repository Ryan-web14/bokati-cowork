package com.sni.bokaticowork.features.inventory.stock.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QualityInspectionRequest {

    /** Plan applique. Resolu automatiquement depuis l article si absent. */
    private String planCode;

    private BigDecimal sampledQuantity;

    private BigDecimal conformQuantity;

    private BigDecimal nonConformQuantity;

    private String inspectedBy;

    /** Motif de la decision, obligatoire pour une derogation. */
    private String decisionReason;

    /** Qui prend la decision. Doit etre renseigne pour une derogation. */
    private String decisionBy;

    /** Accepte le lot malgre un critere bloquant en echec, sous responsabilite explicite. */
    private Boolean acceptByDerogation;

    @NotEmpty
    @Valid
    private List<Measure> measures;

    @Data
    public static class Measure {

        private String criterionCode;

        private BigDecimal measuredValue;

        private Boolean measuredFlag;

        private String notes;
    }
}
