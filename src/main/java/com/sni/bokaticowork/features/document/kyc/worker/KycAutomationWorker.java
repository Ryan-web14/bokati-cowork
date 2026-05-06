package com.sni.bokaticowork.features.document.kyc.worker;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.config.KycAutomationProperties;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KycAutomationWorker {

    private static final Long SYSTEM_REVIEWER_ID = 0L;

    private final KycDocumentRepository kycDocumentRepository;
    private final KycCaseRepository kycCaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final OutboxService outboxService;
    private final KycAutomationProperties properties;

    @Scheduled(cron = "0 0 8 * * MON")
    @Transactional
    public void checkExpiringKycDocuments() {
        LocalDate today = LocalDate.now();
        for (Integer days : properties.getExpiry().getReminderDays()) {
            LocalDate target = today.plusDays(days);
            kycDocumentRepository.findAllByExpiryDateBetween(target, target).forEach(item ->
                    publishDocumentEvent("KYC_DOCUMENT_EXPIRY_REMINDER", item, Map.of("daysUntilExpiry", days)));
        }

        kycDocumentRepository.findAllByExpiryDateBeforeAndStatusNot(today, KycDocumentVerificationStatus.EXPIRED)
                .forEach(this::expireDocument);
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void remindIncompleteKycCases() {
        if (!Boolean.TRUE.equals(properties.getReminders().getEnabled())) {
            return;
        }
        Instant now = Instant.now();
        List<KycCaseStatus> statuses = List.of(KycCaseStatus.IN_PROGRESS, KycCaseStatus.PENDING_CORRECTION);
        kycCaseRepository.findAll().stream()
                .filter(item -> statuses.contains(item.getStatus()))
                .filter(item -> item.getReminderCount() == null || item.getReminderCount() < properties.getReminders().getMaxReminders())
                .filter(item -> shouldSendReminder(item, now))
                .forEach(item -> {
                    item.setLastReminderSentAt(now);
                    item.setReminderCount(item.getReminderCount() == null ? 1 : item.getReminderCount() + 1);
                    kycCaseRepository.save(item);
                    publishCaseEvent("KYC_INCOMPLETE_REMINDER", item);

                });
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void autoApproveEligibleDocuments() {
        kycDocumentRepository.findAllByStatusIn(List.of(KycDocumentVerificationStatus.PENDING)).stream()
                .filter(item -> item.getDocument() != null && item.getDocument().getDocumentType() != null)
                .filter(item -> item.getDocument().getDocumentType().getAutoApproveAfterDays() != null)
                .filter(item -> item.getDocument().getDocumentType().getAutoApproveAfterDays() > 0)
                .filter(this::hasReachedAutoApproveDelay)
                .forEach(item -> {
                    try {
                        DocumentReviewDecisionRequest decision = new DocumentReviewDecisionRequest();
                        decision.setReviewedBy(SYSTEM_REVIEWER_ID);
                        decision.setComment("Auto-approved after configured delay");
                        documentService.approve(item.getDocument().getCode(), decision);
                    } catch (Exception ex) {
                        log.warn("Unable to auto-approve KYC document {}", item.getId(), ex);
                    }
                });
    }

    private void expireDocument(KycDocument item) {
        item.setStatus(KycDocumentVerificationStatus.EXPIRED);
        kycDocumentRepository.save(item);
        Document document = item.getDocument();
        if (document != null && document.getStatus() != DocumentStatus.EXPIRED) {
            document.setStatus(DocumentStatus.EXPIRED);
            documentRepository.save(document);
        }
        KycCase kycCase = item.getKycCase();
        if (kycCase != null) {
            kycCase.setStatus(kycCase.getStatus() == KycCaseStatus.APPROVED
                    ? KycCaseStatus.RENEWAL_REQUIRED
                    : KycCaseStatus.PENDING_CORRECTION);
            kycCase.setDecisionComment("KYC document expired: " + item.getDocumentType());
            kycCaseRepository.save(kycCase);
            syncOwnerStatus(kycCase);
            publishCaseEvent("KYC_RENEWAL_REQUIRED", kycCase);

        }
        publishDocumentEvent("KYC_DOCUMENT_EXPIRED", item, Map.of());
    }

    private boolean shouldSendReminder(KycCase kycCase, Instant now) {
        Instant reference = kycCase.getLastReminderSentAt() == null ? kycCase.getStartedAt() : kycCase.getLastReminderSentAt();
        long inactiveDays = ChronoUnit.DAYS.between(reference, now);
        return properties.getReminders().getReminderAfterDays().stream().anyMatch(days -> inactiveDays >= days);
    }

    private boolean hasReachedAutoApproveDelay(KycDocument item) {
        Instant uploadedAt = item.getDocument().getUploadedAt();
        if (uploadedAt == null) {
            return false;
        }
        Integer delay = item.getDocument().getDocumentType().getAutoApproveAfterDays();
        return !uploadedAt.plus(delay, ChronoUnit.DAYS).isAfter(Instant.now());
    }

    private void syncOwnerStatus(KycCase kycCase) {
        if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
            memberRepository.findById(kycCase.getOwnerId()).ifPresent(member -> {
                member.setStatus(MemberStatus.PENDING_CORRECTION);
                memberRepository.save(member);
            });
        }
        if (kycCase.getOwnerType() == DocumentOwnerType.CUSTOMER) {
            customerRepository.findById(kycCase.getOwnerId()).ifPresent(customer -> {
                customer.setStatus(CustomerStatus.PENDING);
                customerRepository.save(customer);
            });
        }
    }

    private void publishCaseEvent(String eventType, KycCase kycCase) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("kycCaseCode", kycCase.getCode());
        payload.put("ownerType", kycCase.getOwnerType());
        payload.put("ownerId", kycCase.getOwnerId());
        payload.put("status", kycCase.getStatus());
        outboxService.publish(eventType, "KYC_CASE", kycCase.getCode(), payload);
    }

    private void publishDocumentEvent(String eventType, KycDocument document, Map<String, Object> extra) {
        HashMap<String, Object> payload = new HashMap<>(extra);
        payload.put("documentCode", document.getDocument() == null ? null : document.getDocument().getCode());
        payload.put("documentType", document.getDocumentType());
        payload.put("kycCaseCode", document.getKycCase() == null ? null : document.getKycCase().getCode());
        payload.put("expiryDate", document.getExpiryDate());
        payload.put("ownerType", document.getOwnerType());
        payload.put("ownerId", document.getOwnerId());
        outboxService.publish(eventType, "KYC_DOCUMENT", String.valueOf(document.getId()), payload);
    }
}
