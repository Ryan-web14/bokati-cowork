package com.sni.bokaticowork.features.payment.service.interfaces;

public interface CashDocumentPdfService {
    byte[] generateMovementDocument(String movementNumber);
    byte[] generateRequestDocument(String requestNumber);
    byte[] generateSessionClosingReport(String sessionNumber);

    /** Donnees d'un bon d'entree ou de sortie pour l'apercu d'un modele de document (lot D2). */
    com.sni.bokaticowork.features.document.template.render.DocumentData previewData(String templateCode, String movementNumber);

    /** Modele d'un mouvement (bon de sortie ou d'entree), ou {@code null} ; le mouvement doit exister. */
    String templateCode(String movementNumber);
}
