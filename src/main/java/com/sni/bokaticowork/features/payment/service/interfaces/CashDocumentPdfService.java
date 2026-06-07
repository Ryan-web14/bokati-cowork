package com.sni.bokaticowork.features.payment.service.interfaces;

public interface CashDocumentPdfService {
    byte[] generateMovementDocument(String movementNumber);
    byte[] generateRequestDocument(String requestNumber);
    byte[] generateSessionClosingReport(String sessionNumber);
}
