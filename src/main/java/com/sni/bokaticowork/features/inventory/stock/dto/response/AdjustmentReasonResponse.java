package com.sni.bokaticowork.features.inventory.stock.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdjustmentReasonResponse {

    private String reasonCode;

    private String label;

    private String counterpartAccount;

    private Boolean negativeOnly;

    private Boolean positiveOnly;

    private Boolean active;
}
