package com.sni.bokaticowork.features.document.kyc.service.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;

public interface KycAutomationService {
    void initializeMemberKyc(String memberId);
    void initializeCustomerKyc(String customerId);
    void syncMemberKyc(String memberId);
    void syncCustomerKyc(String customerId);
    void syncFromDocumentUpload(Document document);
    void syncFromDocumentReview(Document document);

    /** Recalcul du statut d'un dossier apres un changement de ses exigences (lot KG1). */
    void recompute(com.sni.bokaticowork.features.document.kyc.model.KycCase kycCase);
}
