package com.sni.bokaticowork.features.portal.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ClientSpendingSummaryResponse {

    private BigDecimal totalInvoiced;
    private BigDecimal totalPaid;
    private BigDecimal totalBalanceDue;
    private long invoiceCount;
    private long unpaidCount;
    private long overdueCount;
}
