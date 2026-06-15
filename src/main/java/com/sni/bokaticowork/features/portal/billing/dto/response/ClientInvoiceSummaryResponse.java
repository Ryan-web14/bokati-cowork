package com.sni.bokaticowork.features.portal.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ClientInvoiceSummaryResponse {

    private String documentNumber;
    private BillingDocumentType documentType;
    private BillingDocumentStatus status;
    private String title;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Instant issuedAt;
    private Instant paidAt;
    private String sourceType;
    private String sourceCode;
    private String resolvedSourceLabel;
}
