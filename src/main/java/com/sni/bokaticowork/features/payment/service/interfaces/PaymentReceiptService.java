package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;

public interface PaymentReceiptService {
    PaymentReceiptResponse getByTransactionNumber(String transactionNumber);
    PaymentReceiptResponse getByReceiptNumber(String receiptNumber);
    byte[] generatePdfByTransactionNumber(String transactionNumber);
    byte[] generatePdfByReceiptNumber(String receiptNumber);
}
