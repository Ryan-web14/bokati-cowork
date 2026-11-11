package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.OfflineMobileMoneyOperator;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Reglement direct d'une facture au guichet.
 *
 * <p>{@code cashSessionNumber} est obligatoire en especes ; pour un cheque, un virement ou un mobile
 * money hors plateforme il rattache le paiement a la session du guichet, sans entrer dans le compte
 * des especes. {@code providerReference} porte le numero du cheque, la reference du virement ou
 * l'identifiant de la transaction chez l'operateur.</p>
 */
public record PayInvoiceRequest(
        @NotNull PaymentMethod paymentMethod,
        BigDecimal amount,
        String walletNumber,
        String cashSessionNumber,
        String providerReference,
        String processedBy,
        String idempotencyKey,
        String metadataJson,
        BigDecimal tenderedAmount,
        String bankName,
        String payerName,
        String payerPhone,
        OfflineMobileMoneyOperator mobileMoneyOperator
) {
    public PayInvoiceRequest(PaymentMethod paymentMethod, BigDecimal amount, String walletNumber,
                             String cashSessionNumber, String providerReference, String processedBy,
                             String idempotencyKey, String metadataJson) {
        this(paymentMethod, amount, walletNumber, cashSessionNumber, providerReference, processedBy,
                idempotencyKey, metadataJson, null, null, null, null, null);
    }
}
