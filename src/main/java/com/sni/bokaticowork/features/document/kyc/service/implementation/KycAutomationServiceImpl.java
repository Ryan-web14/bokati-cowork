package com.sni.bokaticowork.features.document.kyc.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.KycRiskLevel;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycOcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class KycAutomationServiceImpl implements KycAutomationService {

    private final KycCaseRepository kycCaseRepository;
    private final DocumentRequirementRepository requirementRepository;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;
    private final KycDocumentRepository kycDocumentRepository;
    private final KycOcrService kycOcrService;

    @Override
    public void initializeMemberKyc(String memberId) {
        Member member = memberRepository.findByMemberIdAndDeletedFalse(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (!hasActiveRequirements(DocumentOwnerType.MEMBER, null)) {
            return;
        }
        ensureCase(DocumentOwnerType.MEMBER, member.getId());
        if (member.getStatus() != MemberStatus.PENDING) {
            member.setStatus(MemberStatus.PENDING);
            memberRepository.save(member);
        }
        syncMemberKyc(memberId);
    }

    @Override
    public void initializeCustomerKyc(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (!hasActiveRequirements(DocumentOwnerType.CUSTOMER, customer.getType() == null ? null : customer.getType().name())) {
            return;
        }
        ensureCase(DocumentOwnerType.CUSTOMER, customer.getId());
        if (customer.getStatus() == CustomerStatus.ACTIVE) {
            customer.setStatus(CustomerStatus.PENDING);
            customerRepository.save(customer);
        }
        syncCustomerKyc(customerId);
    }

    @Override
    public void syncMemberKyc(String memberId) {
        Member member = memberRepository.findByMemberIdAndDeletedFalse(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (!hasActiveRequirements(DocumentOwnerType.MEMBER, null)) {
            return;
        }
        KycCase kycCase = ensureCase(DocumentOwnerType.MEMBER, member.getId());
        KycCaseStatus targetStatus = switch (member.getStatus()) {
            case PENDING -> KycCaseStatus.IN_PROGRESS;
            case UNDER_REVIEW -> KycCaseStatus.UNDER_REVIEW;
            case PENDING_CORRECTION, REJECTED -> KycCaseStatus.PENDING_CORRECTION;
            case ACTIVE -> KycCaseStatus.APPROVED;
            case ARCHIVED -> KycCaseStatus.EXPIRED;
            case INACTIVE, SUSPENDED -> kycCase.getStatus();
        };
        applyCaseStatus(kycCase, targetStatus, "Synced from member status");
    }

    @Override
    public void syncCustomerKyc(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (!hasActiveRequirements(DocumentOwnerType.CUSTOMER, customer.getType() == null ? null : customer.getType().name())) {
            return;
        }
        KycCase kycCase = ensureCase(DocumentOwnerType.CUSTOMER, customer.getId());
        KycCaseStatus targetStatus = switch (customer.getStatus()) {
            case PENDING -> KycCaseStatus.IN_PROGRESS;
            case ACTIVE -> KycCaseStatus.APPROVED;
            case ARCHIVED -> KycCaseStatus.EXPIRED;
            case INACTIVE, SUSPENDED -> kycCase.getStatus();
        };
        applyCaseStatus(kycCase, targetStatus, "Synced from customer status");
    }

    @Async
    @Override
    @Transactional
    public void syncFromDocumentUpload(Document document) {
        if (document == null || document.getCategory() != DocumentCategory.KYC) {
            return;
        }
        KycCase kycCase = ensureCase(document.getOwnerType(), document.getOwnerId());
        attachOrRefreshKycDocument(kycCase, document);
        recomputeCaseState(kycCase);
    }

    @Async
    @Override
    @Transactional
    public void syncFromDocumentReview(Document document) {
        if (document == null || document.getCategory() != DocumentCategory.KYC) {
            return;
        }
        KycCase kycCase = ensureCase(document.getOwnerType(), document.getOwnerId());
        attachOrRefreshKycDocument(kycCase, document);
        recomputeCaseState(kycCase);
    }

    private boolean hasActiveRequirements(DocumentOwnerType ownerType, String customerType) {
        List<DocumentRequirement> requirements = requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(ownerType);
        if (ownerType != DocumentOwnerType.CUSTOMER || customerType == null) {
            return requirements.stream().anyMatch(item -> Boolean.TRUE.equals(item.getRequired()));
        }
        return requirements.stream()
                .filter(item -> item.getCustomerType() == null || item.getCustomerType().isBlank() || customerType.equalsIgnoreCase(item.getCustomerType()))
                .anyMatch(item -> Boolean.TRUE.equals(item.getRequired()));
    }

    private KycCase ensureCase(DocumentOwnerType ownerType, Long ownerId) {
        KycCase existing = kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(ownerType, ownerId).orElse(null);
        if (existing != null && existing.getStatus() != KycCaseStatus.EXPIRED) {
            return existing;
        }

        KycCase entity = KycCase.builder()
                .code(sequenceGenerator.next("KYCCASE", LocalDate.now()))
                .ownerType(ownerType)
                .ownerId(ownerId)
                .status(KycCaseStatus.IN_PROGRESS)
                .riskLevel(ownerType == DocumentOwnerType.BUSINESS ? KycRiskLevel.MEDIUM : KycRiskLevel.LOW)
                .kycLevel(1)
                .build();
        kycCaseRepository.save(entity);
        publishAutomationEvent("KYC_CASE_AUTO_CREATED", entity);
        return entity;
    }

    private void applyCaseStatus(KycCase kycCase, KycCaseStatus targetStatus, String reason) {
        if (kycCase.getStatus() == targetStatus) {
            return;
        }
        kycCase.setStatus(targetStatus);
        if (targetStatus == KycCaseStatus.APPROVED) {
            Instant now = Instant.now();
            kycCase.setReviewedAt(now);
            kycCase.setCompletedAt(now);
            if (kycCase.getSubmittedAt() == null) {
                kycCase.setSubmittedAt(now);
            }
        }
        if (targetStatus == KycCaseStatus.UNDER_REVIEW && kycCase.getSubmittedAt() == null) {
            kycCase.setSubmittedAt(Instant.now());
        }
        if (targetStatus == KycCaseStatus.IN_PROGRESS) {
            kycCase.setCompletedAt(null);
            kycCase.setReviewedAt(null);
            kycCase.setDecisionComment(null);
        }
        if (targetStatus == KycCaseStatus.PENDING_CORRECTION) {
            kycCase.setDecisionComment(reason);
            kycCase.setReviewedAt(Instant.now());
        }
        if (targetStatus == KycCaseStatus.EXPIRED) {
            kycCase.setCompletedAt(Instant.now());
        }
        kycCaseRepository.save(kycCase);
        publishAutomationEvent("KYC_CASE_AUTO_STATUS_SYNC", kycCase);
    }

    private void attachOrRefreshKycDocument(KycCase kycCase, Document document) {
        KycDocument kycDocument = kycDocumentRepository.findAllByDocument(document).stream()
                .filter(item -> item.getKycCase() != null
                        && kycCase.getId() != null
                        && kycCase.getId().equals(item.getKycCase().getId()))
                .findFirst()
                .or(() -> kycDocumentRepository.findAllByDocument(document).stream().findFirst())
                .orElseGet(() -> KycDocument.builder()
                        .kycCase(kycCase)
                        .ownerType(document.getOwnerType())
                        .ownerId(document.getOwnerId())
                        .document(document)
                        .documentType(document.getDocumentType().getCode())
                        .build());
        kycDocument.setKycCase(kycCase);
        kycDocument.setDocumentType(document.getDocumentType().getCode());
        kycDocument.setIssueDate(document.getIssueDate());
        kycDocument.setExpiryDate(document.getExpiryDate());
        kycDocument.setStatus(mapDocumentStatus(document.getStatus()));
        kycDocumentRepository.save(kycDocument);
        kycOcrService.process(kycDocument);
    }

    private void recomputeCaseState(KycCase kycCase) {
        List<DocumentRequirement> requirements = requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(kycCase.getOwnerType());
        List<KycDocument> documents = kycDocumentRepository.findAllByKycCaseOrderByIdAsc(kycCase);

        boolean hasRequiredRequirements = requirements.stream().anyMatch(item -> Boolean.TRUE.equals(item.getRequired()));
        boolean allRequiredPresent = requirements.stream()
                .filter(item -> Boolean.TRUE.equals(item.getRequired()))
                .allMatch(req -> documents.stream().anyMatch(doc -> req.getDocumentTypeCode().equalsIgnoreCase(doc.getDocumentType())));
        boolean anyRejected = documents.stream().anyMatch(doc -> doc.getStatus() == KycDocumentVerificationStatus.REJECTED);
        boolean allVerified = !documents.isEmpty()
                && requirements.stream()
                .filter(item -> Boolean.TRUE.equals(item.getRequired()))
                .allMatch(req -> documents.stream().anyMatch(doc ->
                        req.getDocumentTypeCode().equalsIgnoreCase(doc.getDocumentType())
                                && doc.getStatus() == KycDocumentVerificationStatus.VERIFIED));

        KycCaseStatus targetStatus;
        if (!hasRequiredRequirements) {
            targetStatus = KycCaseStatus.APPROVED;
        } else if (anyRejected) {
            targetStatus = KycCaseStatus.PENDING_CORRECTION;
        } else if (allVerified) {
            targetStatus = KycCaseStatus.APPROVED;
        } else if (allRequiredPresent) {
            targetStatus = KycCaseStatus.UNDER_REVIEW;
        } else {
            targetStatus = KycCaseStatus.IN_PROGRESS;
        }

        applyCaseStatus(kycCase, targetStatus, "Synced from KYC document lifecycle");
        syncOwnerStatusFromCase(kycCase);
    }

    private void syncOwnerStatusFromCase(KycCase kycCase) {
        if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
            Member member = memberRepository.findById(kycCase.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
            MemberStatus target = switch (kycCase.getStatus()) {
                case IN_PROGRESS, NOT_STARTED -> MemberStatus.PENDING;
                case SUBMITTED, UNDER_REVIEW -> MemberStatus.UNDER_REVIEW;
                case PENDING_CORRECTION, REJECTED -> MemberStatus.PENDING_CORRECTION;
                case APPROVED -> MemberStatus.ACTIVE;
                case RENEWAL_REQUIRED, EXPIRED -> MemberStatus.PENDING_CORRECTION;
            };
            if (member.getStatus() != target) {
                member.setStatus(target);
                memberRepository.save(member);
            }
            return;
        }

        if (kycCase.getOwnerType() == DocumentOwnerType.CUSTOMER) {
            Customer customer = customerRepository.findById(kycCase.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            CustomerStatus target = switch (kycCase.getStatus()) {
                case IN_PROGRESS, NOT_STARTED, SUBMITTED, UNDER_REVIEW, PENDING_CORRECTION, REJECTED -> CustomerStatus.PENDING;
                case APPROVED -> CustomerStatus.ACTIVE;
                case RENEWAL_REQUIRED, EXPIRED -> CustomerStatus.PENDING;
            };
            if (customer.getStatus() != target) {
                customer.setStatus(target);
                customerRepository.save(customer);
            }
        }
    }

    private KycDocumentVerificationStatus mapDocumentStatus(DocumentStatus status) {
        return switch (status) {
            case APPROVED, SIGNED -> KycDocumentVerificationStatus.VERIFIED;
            case REJECTED, ARCHIVED -> KycDocumentVerificationStatus.REJECTED;
            case EXPIRED -> KycDocumentVerificationStatus.EXPIRED;
            case DRAFT, UPLOADED, PENDING_REVIEW, NEEDS_CORRECTION, SUPERSEDED -> KycDocumentVerificationStatus.PENDING;
        };
    }

    private void publishAutomationEvent(String eventType, KycCase kycCase) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("kycCaseCode", kycCase.getCode());
        payload.put("ownerType", kycCase.getOwnerType());
        payload.put("ownerId", kycCase.getOwnerId());
        payload.put("status", kycCase.getStatus());
        outboxService.publish(eventType, "KYC_CASE", kycCase.getCode(), payload);
    }
}
