package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;

public interface PaymentReceiptService {
    PaymentReceiptResponse getByTransactionNumber(String transactionNumber);
    PaymentReceiptResponse getByReceiptNumber(String receiptNumber);
    byte[] generatePdfByTransactionNumber(String transactionNumber);
    byte[] generatePdfByReceiptNumber(String receiptNumber);

    /** La version d'impression de l'administration · l'original puis son duplicata, dans un meme PDF. */
    default byte[] generateAdminPdfByTransactionNumber(String transactionNumber) {
        return com.sni.bokaticowork.core.utils.pdf.PdfCopies.originalAndDuplicate(generatePdfByTransactionNumber(transactionNumber));
    }

    default byte[] generateAdminPdfByReceiptNumber(String receiptNumber) {
        return com.sni.bokaticowork.core.utils.pdf.PdfCopies.originalAndDuplicate(generatePdfByReceiptNumber(receiptNumber));
    }
}
