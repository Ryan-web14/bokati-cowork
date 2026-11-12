package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.OfflineMobileMoneyOperator;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Paiement recu hors de la plateforme et saisi au guichet sur une intention.
 *
 * @param paymentMethod     {@code CHEQUE}, {@code BANK_TRANSFER} ou {@code MOBILE_MONEY_OFFLINE}
 * @param amount            montant a imputer, le reste a payer s'il est absent
 * @param reference         numero du cheque, reference du virement, identifiant chez l'operateur
 * @param bankName          banque tiree (cheque, obligatoire) ou d'origine (virement)
 * @param payerName         tireur, donneur d'ordre ou titulaire du compte mobile money
 * @param payerPhone        numero du payeur (mobile money)
 * @param operator          {@code MTN_MOMO} ou {@code AIRTEL_MONEY} (mobile money, obligatoire)
 * @param cashSessionNumber session du guichet qui saisit, facultative
 */
public record RegisterOfflinePaymentRequest(
        @NotNull PaymentMethod paymentMethod,
        BigDecimal amount,
        @Size(max = 120) String reference,
        @Size(max = 180) String bankName,
        @Size(max = 180) String payerName,
        @Size(max = 40) String payerPhone,
        OfflineMobileMoneyOperator operator,
        String cashSessionNumber,
        String receivedBy,
        String metadataJson
) {
}
