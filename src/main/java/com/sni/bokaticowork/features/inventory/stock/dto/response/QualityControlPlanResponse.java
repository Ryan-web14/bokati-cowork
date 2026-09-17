package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.QualityControlStage;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityDecision;
import com.sni.bokaticowork.features.inventory.stock.enums.QualitySamplingMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class QualityControlPlanResponse {

    private String planCode;

    private String name;

    private String itemCode;

    private String categoryCode;

    private QualityControlStage controlStage;

    private QualitySamplingMode samplingMode;

    private BigDecimal samplingParameter;

    private QualityDecision decisionOnFail;

    private Boolean quarantineOnReceipt;

    private Boolean active;

    private List<Criterion> criteria;

    @Data
    @Builder
    public static class Criterion {
        private String criterionCode;
        private String name;
        private String unit;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private Boolean booleanExpected;
        private Boolean blocking;
        private Integer position;
    }
}
