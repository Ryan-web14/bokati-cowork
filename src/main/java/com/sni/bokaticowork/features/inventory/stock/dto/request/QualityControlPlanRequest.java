package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.QualityControlStage;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityDecision;
import com.sni.bokaticowork.features.inventory.stock.enums.QualitySamplingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QualityControlPlanRequest {

    @NotBlank
    private String name;

    /** Article vise. Exclusif avec categoryCode. */
    private String itemCode;

    /** Categorie visee. Exclusif avec itemCode. */
    private String categoryCode;

    @NotNull
    private QualityControlStage controlStage;

    @NotNull
    private QualitySamplingMode samplingMode;

    private BigDecimal samplingParameter;

    private QualityDecision decisionOnFail;

    /** Met le lot en quarantaine des sa reception, avant meme le controle. */
    private Boolean quarantineOnReceipt;

    private Boolean active;

    @NotEmpty
    @Valid
    private List<Criterion> criteria;

    @Data
    public static class Criterion {

        @NotBlank
        private String criterionCode;

        @NotBlank
        private String name;

        private String unit;

        private BigDecimal minValue;

        private BigDecimal maxValue;

        /** Vrai pour un critere d observation, sans mesure numerique. */
        private Boolean booleanExpected;

        private Boolean blocking;

        private Integer position;
    }
}
