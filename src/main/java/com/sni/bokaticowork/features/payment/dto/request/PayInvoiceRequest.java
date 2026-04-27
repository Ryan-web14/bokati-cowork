
package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayInvoiceRequest(
        @NotNull PaymentMethod paymentMethod,
        BigDecimal amount,
        String walletNumber,
        String cashSessionNumber,
        String providerReference,
        String processedBy,
        String idempotencyKey,
        String metadataJson
) {
}