package com.sni.bokaticowork.features.document.kyc.service.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;

public interface KycAutomationService {
    void initializeMemberKyc(String memberId);
    void initializeCustomerKyc(String customerId);

    /** Entreprise creee : dossier KYC ouvert si une exigence d'entreprise s'applique. */
    void initializeBusinessKyc(Long businessId);

    /** Dossiers manquants (membres, clients hors membres, entreprises) : comptes, et crees si {@code apply}. */
    java.util.Map<String, Integer> backfillMissingCases(boolean apply);
    void syncMemberKyc(String memberId);
    void syncCustomerKyc(String customerId);
    void syncFromDocumentUpload(Document document);
    void syncFromDocumentReview(Document document);

    /**
     * Lot KG2 : met a jour la piece d'un document (recto ou verso) et recalcule le dossier. Appele
     * par la synchronisation programmee apres validation ; ne pas appeler dans la transaction d'un depot.
     */
    void applyDocumentSync(Document document);

    /** Recalcul du statut d'un dossier apres un changement de ses exigences (lot KG1). */
    void recompute(com.sni.bokaticowork.features.document.kyc.model.KycCase kycCase);
}
