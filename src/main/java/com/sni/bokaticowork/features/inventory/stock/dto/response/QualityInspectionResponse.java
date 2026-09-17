package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.QualityDecision;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class QualityInspectionResponse {

    private String inspectionCode;

    private String planCode;

    private String itemCode;

    private Long lotId;

    private String lotNumber;

    private String locationCode;

    private BigDecimal sampledQuantity;

    private BigDecimal conformQuantity;

    private BigDecimal nonConformQuantity;

    private QualityDecision decision;

    private String decisionBy;

    private String decisionReason;

    private String inspectedBy;

    private Instant inspectedAt;

    /** Non-conformite ouverte par cette inspection, le cas echeant. */
    private String nonConformanceCode;

    /** Vrai lorsque le lot a ete immobilise par cette inspection. */
    private boolean lotQuarantined;

    private List<Result> results;

    @Data
    @Builder
    public static class Result {
        private String criterionCode;
        private String criterionName;
        private BigDecimal measuredValue;
        private Boolean measuredFlag;
        private Boolean conform;
        private Boolean blocking;
        private String notes;
    }
}
