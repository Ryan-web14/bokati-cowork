package com.sni.bokaticowork.features.portal.billing.dto.response;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ClientInvoiceResponse {

    private String documentNumber;
    private BillingDocumentType documentType;
    private BillingDocumentStatus status;
    private String title;
    private String description;
    private String customerName;
    private String customerEmail;
    private String currency;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Instant issuedAt;
    private Instant paidAt;
    private String paymentReference;
    private String paymentInstructions;
    private String bankDetailsJson;
    private String sourceType;
    private String sourceCode;
    private String resolvedSourceLabel;
    private List<BillingDocumentLineResponse> lines;
}
