package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentReceiptResponse(
        String receiptNumber,
        Instant receiptIssuedAt,
        String transactionNumber,
        String intentNumber,
        PaymentMethod paymentMethod,
        PaymentTransactionStatus transactionStatus,
        String provider,
        String providerReference,
        String depositId,
        String payerPhone,
        /** La reference de la transaction chez l'operateur (MTN, Airtel...) · vide hors mobile money. */
        String operatorReference,
        String purpose,
        String sourceType,
        String sourceCode,
        String customerType,
        String customerCode,
        String customerName,
        String customerEmail,
        String customerPhone,
        String billingAddressJson,
        BigDecimal paidAmount,
        BigDecimal allocatedAmount,
        BigDecimal advanceAmount,
        String currency,
        Instant paidAt,
        String receivedBy,
        List<PaymentReceiptAllocationResponse> allocations,
        /** Le moyen en francais, operateur compris : « Chèque », « Mobile money hors plateforme (MTN MoMo Pay) ». */
        String paymentMethodLabel,
        /** Especes : montant tendu par le client. */
        BigDecimal tenderedAmount,
        /** Especes : monnaie rendue. */
        BigDecimal changeAmount,
        /** Cheque ou virement : banque. */
        String bankName,
        /** Tireur, donneur d'ordre ou titulaire du compte mobile money. */
        String payerName
) {
}
