package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;

public interface BillingEmailService {
    boolean sendDocument(String documentNumber);
    void sendPaymentConfirmation(BillingDocumentResponse document, PaymentTransactionResponse transaction);
}
