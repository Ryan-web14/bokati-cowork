package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentLifecycleAutomationService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentLifecycleAutomationServiceImpl implements DocumentLifecycleAutomationService {

    private static final List<DocumentStatus> EXPIRABLE_STATUSES = List.of(
            DocumentStatus.APPROVED,
            DocumentStatus.SIGNED,
            DocumentStatus.PENDING_REVIEW
    );

    private static final List<Integer> EXPIRY_REMINDER_DAYS = List.of(30, 7);

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final KycAutomationService kycAutomationService;
    private final OutboxService outboxService;

    private com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService documentService;
    private com.sni.bokaticowork.features.document.documentMaster.repository.DocumentVersionRepository versionRepository;
    private com.sni.bokaticowork.features.document.documentMaster.repository.DocumentReviewRepository reviewRepository;

    /** Lot KG3 : l'approbation automatique passe par l'approbation normale (trace de revue, KYC, evenements). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setAutoApproveCollaborators(
            @org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService documentService,
            com.sni.bokaticowork.features.document.documentMaster.repository.DocumentVersionRepository versionRepository,
            com.sni.bokaticowork.features.document.documentMaster.repository.DocumentReviewRepository reviewRepository) {
        this.documentService = documentService;
        this.versionRepository = versionRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public int expireDocuments() {
        LocalDate today = LocalDate.now();
        List<Document> documents = documentRepository.findAllByStatusInAndExpiryDateBefore(EXPIRABLE_STATUSES, today);
        int processed = 0;
        for (Document document : documents) {
            if (document.getExpiryDate() == null || !document.getExpiryDate().isBefore(today)) {
                continue;
            }
            // Lot KG3 : une piece KYC expire par la tache KYC (delai de grace, renouvellement).
            if (isKyc(document)) {
                continue;
            }
            document.setStatus(DocumentStatus.EXPIRED);
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            kycAutomationService.syncFromDocumentReview(document);
            publishExpiredEvent(document);
            processed++;
        }
        return processed;
    }

    @Override
    public int notifyPreExpiry() {
        LocalDate today = LocalDate.now();
        int total = 0;
        for (int days : EXPIRY_REMINDER_DAYS) {
            LocalDate target = today.plusDays(days);
            List<Document> docs = documentRepository.findAllByStatusInAndExpiryDateBetween(
                    EXPIRABLE_STATUSES, target, target);
            for (Document doc : docs) {
                if (isKyc(doc)) {
                    // Lot KG3 : rappels des pieces KYC par la tache KYC (un seul courriel).
                    continue;
                }
                // A document sits on a threshold for a whole day, and this worker runs hourly, so
                // it matches 24 times. Thresholds are walked from the furthest out to the nearest,
                // hence "strictly closer than the one already sent".
                Integer alreadySent = doc.getExpiryReminderSentDays();
                if (alreadySent != null && alreadySent <= days) {
                    continue;
                }
                publishPreExpiryEvent(doc, days);
                doc.setExpiryReminderSentDays(days);
                documentRepository.save(doc);
                total++;
            }
        }
        return total;
    }

    private void publishExpiredEvent(Document document) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("documentCode", document.getCode());
        payload.put("ownerType", document.getOwnerType());
        payload.put("ownerId", document.getOwnerId());
        payload.put("status", document.getStatus());
        outboxService.publish("DOCUMENT_AUTO_EXPIRED", "DOCUMENT", document.getCode(), payload);
    }

    /**
     * Lot KG3 : une seule regle d'approbation automatique, pour tous les documents (KYC compris).
     *
     * <ul>
     *   <li>{@code autoApprove} sans delai : approuve au depot (voir le depot) ;</li>
     *   <li>{@code autoApproveAfterDays} &gt; 0 : approuve quand la version courante attend depuis ce
     *       delai, <b>jamais</b> si le document a deja ete refuse ou renvoye en correction ;</li>
     *   <li>par l'approbation normale (trace de revue, piece KYC, evenements), une transaction par
     *       document : une erreur n'annule pas les autres.</li>
     * </ul>
     *
     * <p>Avant : il fallait aussi {@code autoApprove}, qui approuvait deja au depot (la regle ne
     * servait jamais), et la tache KYC approuvait de son cote, y compris des pieces corrigees.</p>
     */
    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public int autoApproveDocuments() {
        List<DocumentType> autoApproveTypes = documentTypeRepository.findAllByAutoApproveAfterDaysIsNotNull();
        int approved = 0;
        Instant now = Instant.now();
        for (DocumentType type : autoApproveTypes) {
            if (type.getAutoApproveAfterDays() == null || type.getAutoApproveAfterDays() <= 0) {
                continue;
            }
            Instant threshold = now.minus(type.getAutoApproveAfterDays(), ChronoUnit.DAYS);
            List<Document> candidates = documentRepository.findAllByStatusAndDocumentTypeAndUploadedAtBefore(
                    DocumentStatus.PENDING_REVIEW, type, threshold);
            for (Document doc : candidates) {
                if (!eligibleForAutoApproval(doc, threshold)) {
                    continue;
                }
                try {
                    var decision = new com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest();
                    decision.setReviewedBy(com.sni.bokaticowork.core.utils.constant.SystemActors.SYSTEM_USER_ID);
                    decision.setComment("Approbation automatique après " + type.getAutoApproveAfterDays() + " jour(s) sans revue");
                    if (documentService != null) {
                        documentService.approve(doc.getCode(), decision);
                    }
                    publishAutoApprovedEvent(doc);
                    approved++;
                } catch (RuntimeException ex) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).warn("Approbation automatique impossible pour {} : {}", doc.getCode(), ex.getMessage());
                }
            }
        }
        return approved;
    }

    /** Version courante deposee avant le seuil, et jamais refusee ni renvoyee en correction. */
    boolean eligibleForAutoApproval(Document doc, Instant threshold) {
        if (versionRepository != null) {
            var current = versionRepository.findByDocumentAndCurrentTrue(doc).orElse(null);
            if (current != null && current.getUploadedAt() != null && current.getUploadedAt().isAfter(threshold)) {
                return false;
            }
        }
        if (reviewRepository != null) {
            boolean contested = reviewRepository.findAllByDocumentOrderByReviewedAtDesc(doc).stream()
                    .anyMatch(review -> review.getReviewStatus() == com.sni.bokaticowork.features.document.documentMaster.enums.DocumentReviewStatus.REJECTED
                            || review.getReviewStatus() == com.sni.bokaticowork.features.document.documentMaster.enums.DocumentReviewStatus.NEEDS_CORRECTION);
            if (contested) {
                return false;
            }
        }
        return true;
    }

    private static boolean isKyc(Document document) {
        return document.getCategory() == com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory.KYC;
    }

    private void publishAutoApprovedEvent(Document document) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("documentCode", document.getCode());
        payload.put("ownerType", document.getOwnerType());
        payload.put("ownerId", document.getOwnerId());
        payload.put("documentType", document.getTypeCode());
        outboxService.publish("DOCUMENT_AUTO_APPROVED", "DOCUMENT", document.getCode(), payload);
    }

    private void publishPreExpiryEvent(Document document, int daysUntilExpiry) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("documentCode", document.getCode());
        payload.put("ownerType", document.getOwnerType());
        payload.put("ownerId", document.getOwnerId());
        payload.put("daysUntilExpiry", daysUntilExpiry);
        payload.put("expiryDate", document.getExpiryDate());
        payload.put("space", document.getSpace());
        outboxService.publish("DOCUMENT_EXPIRY_REMINDER", "DOCUMENT", document.getCode(), payload);
    }
}
