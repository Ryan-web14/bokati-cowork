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
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
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
    private final BusinessService businessService;

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
            case ACTIVE -> kycCase.getStatus();
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
            // Lot KG1 : activer un client n'approuve plus son dossier (avant : APPROVED, sans courriel
            // ni portefeuille, meme pour un dossier tout juste cree). Le dossier se decide sur ses pieces.
            case ACTIVE -> kycCase.getStatus();
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

    private com.sni.bokaticowork.features.document.kyc.service.support.KycRequirementResolver requirementResolver;
    private com.sni.bokaticowork.features.payment.service.interfaces.WalletService walletService;

    /** Lot KG1 : exigences du dossier (globales et propres au dossier) calculees en un seul endroit. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setRequirementResolver(com.sni.bokaticowork.features.document.kyc.service.support.KycRequirementResolver requirementResolver) {
        this.requirementResolver = requirementResolver;
    }

    /** Lot KG1 : portefeuille par defaut cree aussi quand le dossier est approuve par le recalcul. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setWalletService(@org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.payment.service.interfaces.WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    @Transactional
    public void recompute(KycCase kycCase) {
        recomputeCaseState(kycCase);
    }

    private boolean hasActiveRequirements(DocumentOwnerType ownerType, String customerType) {
        return hasActiveRequirements(ownerType, customerType, null);
    }

    private boolean hasActiveRequirements(DocumentOwnerType ownerType, String customerType, String businessLegalForm) {
        List<DocumentRequirement> requirements = requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(ownerType);
        return filterRequirements(requirements, ownerType, customerType, businessLegalForm)
                .anyMatch(item -> Boolean.TRUE.equals(item.getRequired()));
    }

    List<DocumentRequirement> filteredRequirements(DocumentOwnerType ownerType, String customerType, String businessLegalForm) {
        List<DocumentRequirement> requirements = requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(ownerType);
        return filterRequirements(requirements, ownerType, customerType, businessLegalForm).toList();
    }

    private java.util.stream.Stream<DocumentRequirement> filterRequirements(
            List<DocumentRequirement> requirements, DocumentOwnerType ownerType, String customerType, String businessLegalForm) {
        java.util.stream.Stream<DocumentRequirement> stream = requirements.stream();
        if (ownerType == DocumentOwnerType.CUSTOMER && customerType != null) {
            stream = stream.filter(item -> item.getCustomerType() == null
                    || item.getCustomerType().isBlank()
                    || customerType.equalsIgnoreCase(item.getCustomerType()));
        }
        if (ownerType == DocumentOwnerType.BUSINESS && businessLegalForm != null) {
            stream = stream.filter(item -> item.getBusinessLegalForm() == null
                    || item.getBusinessLegalForm().isBlank()
                    || businessLegalForm.equalsIgnoreCase(item.getBusinessLegalForm()));
        }
        return stream;
    }

    private String resolveBusinessLegalForm(DocumentOwnerType ownerType, Long ownerId) {
        if (ownerType != DocumentOwnerType.BUSINESS) {
            return null;
        }
        try {
            return businessService.serviceBusinessById(ownerId).getLegalForm();
        } catch (Exception e) {
            return null;
        }
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
        KycCaseStatus previousStatus = kycCase.getStatus();
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
        if (targetStatus == KycCaseStatus.APPROVED && previousStatus != KycCaseStatus.APPROVED) {
            // Lot KG1 : memes effets que l'approbation par l'equipe (portefeuille, courriel d'approbation).
            provisionDefaultWallet(kycCase);
            publishAutomationEvent("KYC_CASE_APPROVED", kycCase);
            return;
        }
        String eventType = previousStatus == KycCaseStatus.APPROVED && targetStatus != KycCaseStatus.EXPIRED
                ? "KYC_CASE_REOPENED"
                : "KYC_CASE_AUTO_STATUS_SYNC";
        publishAutomationEvent(eventType, kycCase);
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
        if (kycCase.getStatus() == KycCaseStatus.REJECTED) {
            // Refus definitif : le recalcul n'y touche jamais.
            return;
        }
        List<DocumentRequirement> requirements = requirementResolver != null
                ? requirementResolver.resolve(kycCase)
                : filteredRequirements(kycCase.getOwnerType(), null, resolveBusinessLegalForm(kycCase.getOwnerType(), kycCase.getOwnerId()));
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
                                && doc.getStatus() == KycDocumentVerificationStatus.VERIFIED
                                && backSideVerified(doc)));

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

        // Un dossier soumis par son titulaire reste soumis tant qu'il attend la revue.
        if (targetStatus == KycCaseStatus.UNDER_REVIEW && kycCase.getStatus() == KycCaseStatus.SUBMITTED) {
            targetStatus = KycCaseStatus.SUBMITTED;
        }
        applyCaseStatus(kycCase, targetStatus, "Synced from KYC document lifecycle");
        syncOwnerStatusFromCase(kycCase);
    }

    /** Verso exige approuve quand le type a un verso (lot KG1). */
    private static boolean backSideVerified(KycDocument piece) {
        Document front = piece.getDocument();
        boolean requiresBack = front != null && front.getDocumentType() != null
                && Boolean.TRUE.equals(front.getDocumentType().getRequiresBackSide());
        if (piece.getBackDocument() == null) {
            return !requiresBack;
        }
        return piece.getBackDocument().getStatus() == DocumentStatus.APPROVED || piece.getBackDocument().getStatus() == DocumentStatus.SIGNED;
    }

    private void provisionDefaultWallet(KycCase kycCase) {
        if (walletService == null) {
            return;
        }
        try {
            if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
                memberRepository.findById(kycCase.getOwnerId())
                        .ifPresent(member -> walletService.getOrCreate("MEMBER", member.getMemberId(), "XAF"));
            } else if (kycCase.getOwnerType() == DocumentOwnerType.CUSTOMER) {
                customerRepository.findById(kycCase.getOwnerId())
                        .ifPresent(customer -> walletService.getOrCreate("CUSTOMER", customer.getCustomerId(), "XAF"));
            }
        } catch (RuntimeException exception) {
            // Comme l'approbation par l'equipe : un echec du portefeuille n'annule pas l'approbation.
            org.slf4j.LoggerFactory.getLogger(KycAutomationServiceImpl.class)
                    .warn("Portefeuille par defaut non cree pour le dossier {} : {}", kycCase.getCode(), exception.getMessage());
        }
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
            // Lot KG1 : un membre actif dont le dossier est rouvert pour une piece de plus reste actif.
            if (member.getStatus() == MemberStatus.ACTIVE && (target == MemberStatus.PENDING || target == MemberStatus.UNDER_REVIEW)) {
                return;
            }
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
            // Lot KG1 : un client actif dont le dossier est rouvert pour une piece de plus reste actif
            // (comme un membre) ; une piece refusee, une expiration ou un refus le remettent en attente.
            if (customer.getStatus() == CustomerStatus.ACTIVE && (kycCase.getStatus() == KycCaseStatus.IN_PROGRESS
                    || kycCase.getStatus() == KycCaseStatus.NOT_STARTED || kycCase.getStatus() == KycCaseStatus.SUBMITTED
                    || kycCase.getStatus() == KycCaseStatus.UNDER_REVIEW)) {
                return;
            }
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
            // Lot KG1 : une piece renvoyee en correction est a refaire (le dossier attend une correction),
            // et l'approbation automatique ne la prend plus.
            case NEEDS_CORRECTION -> KycDocumentVerificationStatus.REJECTED;
            case DRAFT, UPLOADED, PENDING_REVIEW, SUPERSEDED -> KycDocumentVerificationStatus.PENDING;
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
