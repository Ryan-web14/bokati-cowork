package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentLifecycleAutomationService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
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
    private final KycAutomationService kycAutomationService;
    private final OutboxService outboxService;

    @Override
    public int expireDocuments() {
        LocalDate today = LocalDate.now();
        List<Document> documents = documentRepository.findAllByStatusInAndExpiryDateBefore(EXPIRABLE_STATUSES, today);
        int processed = 0;
        for (Document document : documents) {
            if (document.getExpiryDate() == null || !document.getExpiryDate().isBefore(today)) {
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
                publishPreExpiryEvent(doc, days);
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
