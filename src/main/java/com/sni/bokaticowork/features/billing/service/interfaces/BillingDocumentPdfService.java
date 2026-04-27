package com.sni.bokaticowork.features.billing.service.interfaces;

public interface BillingDocumentPdfService {
    byte[] generatePdf(String documentNumber);
}
