package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;

public record PayInvoiceResponse(
        BillingDocumentResponse invoice,
        PaymentTransactionResponse transaction
) {
}
