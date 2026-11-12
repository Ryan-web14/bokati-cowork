package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;

public interface PaymentReceiptService {
    PaymentReceiptResponse getByTransactionNumber(String transactionNumber);
    PaymentReceiptResponse getByReceiptNumber(String receiptNumber);
    byte[] generatePdfByTransactionNumber(String transactionNumber);
    byte[] generatePdfByReceiptNumber(String receiptNumber);

    /**
     * La version d'impression de l'administration : l'original puis son duplicata, dans un meme PDF.
     * Declaree ici sans corps : l'implementation la porte, sous sa transaction (une methode par
     * defaut de l'interface s'executait hors transaction, et le recu lisait l'intention de paiement
     * apres la fermeture de la session).
     */
    byte[] generateAdminPdfByTransactionNumber(String transactionNumber);

    byte[] generateAdminPdfByReceiptNumber(String receiptNumber);
}
