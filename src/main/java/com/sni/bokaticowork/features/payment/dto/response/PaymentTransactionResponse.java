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
        PaymentChannel channel,
        /** Le moyen en francais, operateur compris : « Chèque », « Mobile money hors plateforme (MTN MoMo Pay) ». */
        String paymentMethodLabel,
        /** Especes : montant tendu par le client, nul si non saisi. */
        BigDecimal tenderedAmount,
        /** Especes : monnaie rendue. */
        BigDecimal changeAmount,
        /** Session de caisse du guichet qui a saisi le paiement. */
        String cashSessionNumber,
        /** Cheque ou virement : banque. */
        String bankName,
        /** Tireur, donneur d'ordre ou titulaire du compte mobile money. */
        String payerName,
        /** Mobile money hors plateforme : numero du payeur. */
        String payerPhone
) {
}
