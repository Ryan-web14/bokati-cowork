package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;

public interface BillingEmailService {
    /** Envoi explicite ou relance · part toujours. */
    boolean sendDocument(String documentNumber);
    /** Envoi automatique · ne part pas si le membre destinataire a coupe cet evenement. */
    boolean sendDocumentIfAllowed(String documentNumber, EventType event);
    void sendDocumentAsync(String documentNumber);
    void sendPaymentConfirmation(BillingDocumentResponse document, PaymentTransactionResponse transaction);
}
