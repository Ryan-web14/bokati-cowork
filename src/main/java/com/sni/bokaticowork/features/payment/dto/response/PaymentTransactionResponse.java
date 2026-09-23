package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.PaymentChannel;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentTransactionResponse(
        String transactionNumber,
        String intentNumber,
        PaymentMethod paymentMethod,
        String provider,
        String providerReference,
        String receiptNumber,
        BigDecimal amount,
        String currency,
        PaymentTransactionStatus status,
        Instant paidAt,
        String receivedBy,
        String failureReason,
        String metadataJson,
        /**
         * D'ou vient l'encaissement · {@code SELF_SERVICE} le client depuis son espace,
         * {@code BACK_OFFICE} un agent, {@code SYSTEM} une automatisation. Nul pour les
         * transactions anterieures a ce suivi.
         */
        PaymentChannel channel
) {
}
