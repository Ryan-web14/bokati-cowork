package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;

/**
 * @param fullRefund pour un remboursement ou une annulation, vrai seulement quand il solde la
 *                   transaction d'origine · un remboursement partiel n'annule ni les factures ni
 *                   ce qu'elles reglaient. Sans objet pour un paiement reussi.
 */
public record PaymentTransactionWorkflowEvent(
        String transactionNumber,
        PaymentTransactionStatus status,
        boolean fullRefund
) {

    public PaymentTransactionWorkflowEvent(String transactionNumber, PaymentTransactionStatus status) {
        this(transactionNumber, status, true);
    }
}
