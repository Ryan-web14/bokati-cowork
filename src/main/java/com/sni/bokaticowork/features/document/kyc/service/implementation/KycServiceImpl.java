package com.sni.bokaticowork.features.document.kyc.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.repository.OutboxEventRepository;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.dto.request.UpdateStatusRequest;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentCorrectionRequest;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.KycRiskLevel;
import com.sni.bokaticowork.features.document.kyc.dto.request.CreateKycCaseRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycAssignRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycBulkApproveRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycBulkRejectRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseBulkApproveRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseBulkRejectRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseCorrectionRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseNoteRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseRequirementRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycDecisionRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycRiskLevelRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycBulkActionResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycBulkItemResult;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseRequirementResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseNoteResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDashboardResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentOcrResultResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycExpiryDocumentStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycTimelineEntryResponse;
import com.sni.bokaticowork.features.document.kyc.mapper.interfaces.KycMapper;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCaseNote;
import com.sni.bokaticowork.features.document.kyc.model.KycCaseRequirement;
import com.sni.bokaticowork.features.document.kyc.model.KycCrossValidationRule;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycDocumentOcrResult;
import com.sni.bokaticowork.features.document.kyc.model.KycVerification;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseNoteRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRequirementRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycCrossValidationRuleRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentOcrResultRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycVerificationRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Entities;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class KycServiceImpl implements KycService {

    private final KycCaseRepository caseRepository;
    private final KycDocumentRepository documentRepository;
    private final KycVerificationRepository verificationRepository;
    private final KycCaseNoteRepository noteRepository;
    private final KycDocumentOcrResultRepository ocrResultRepository;
    private final KycCrossValidationRuleRepository crossValidationRuleRepository;
    private final KycCaseRequirementRepository caseRequirementRepository;
    private final DocumentRequirementRepository requirementRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final CustomerService customerService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final BusinessService businessService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;
    private final OutboxEventRepository outboxEventRepository;
    private final KycAutomationService kycAutomationService;
    private final SpringTemplateEngine templateEngine;
    private final KycMapper mapper;
    private final WalletService walletService;
    private final Locale appLocale;

    @Override
    public KycCaseResponse createCase(CreateKycCaseRequest request) {
        OwnerResolution owner = resolveOwner(request.getOwnerType(), request.getOwnerCode());
        KycCase existing = caseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(owner.ownerType(), owner.ownerId())
                .orElse(null);
        if (existing != null && existing.getStatus() != KycCaseStatus.REJECTED && existing.getStatus() != KycCaseStatus.APPROVED) {
            return toResponse(existing);
        }

        KycCase entity = KycCase.builder()
                .code(sequenceGenerator.next("KYCCASE", LocalDate.now()))
                .ownerType(owner.ownerType())
                .ownerId(owner.ownerId())
                .status(KycCaseStatus.IN_PROGRESS)
                .riskLevel(defaultRiskLevel(owner.ownerType()))
                .kycLevel(1)
                .build();
        caseRepository.save(entity);

        if (request.getDocumentTypeCodes() != null && !request.getDocumentTypeCodes().isEmpty()) {
            for (String typeCode : request.getDocumentTypeCodes()) {
                String normalizedCode = typeCode.trim().toUpperCase(Locale.ROOT);
                DocumentType docType = documentTypeRepository.findByCode(normalizedCode)
                        .orElseThrow(() -> new BadRequestException("Unknown document type: " + typeCode));
                KycCaseRequirement caseReq = KycCaseRequirement.builder()
                        .kycCase(entity)
                        .documentTypeCode(docType.getCode())
                        .documentTypeName(docType.getName())
                        .required(Boolean.TRUE)
                        .build();
                caseRequirementRepository.save(caseReq);
            }
        }

        String ownerName = null;

        if (owner.ownerType() == DocumentOwnerType.MEMBER) {
            memberService.ChangeStatus(owner.ownerCode(), new UpdateStatusRequest(MemberStatus.PENDING.name()));
            ownerName = memberService.getByMemberIdForService(owner.ownerId).getDisplayName();
        } else if (owner.ownerType() == DocumentOwnerType.CUSTOMER) {
            kycAutomationService.syncCustomerKyc(owner.ownerCode());
        }

        publishCaseEvent("KYC_CASE_CREATED", entity, null);

        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public KycCaseResponse getByCode(String code) {
        return toResponse(serviceCase(code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycCaseResponse> list(DocumentOwnerType ownerType) {
        List<KycCase> items = ownerType == null ? caseRepository.findAll() : caseRepository.findAllByOwnerTypeOrderByStartedAtDesc(ownerType);
        return items.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<KycCaseResponse> search(KycCaseStatus status, DocumentOwnerType ownerType, Instant submittedAfter,
                                                     Instant submittedBefore, Long reviewedBy, Boolean pendingReviewOnly,
                                                     Integer expiringWithinDays, KycRiskLevel riskLevel, Pageable pageable) {
        Specification<KycCase> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (ownerType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerType"), ownerType));
        }
        if (submittedAfter != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("submittedAt"), submittedAfter));
        }
        if (submittedBefore != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("submittedAt"), submittedBefore));
        }
        if (reviewedBy != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reviewedBy"), reviewedBy));
        }
        if (Boolean.TRUE.equals(pendingReviewOnly)) {
            spec = spec.and((root, query, cb) -> root.get("status").in(KycCaseStatus.SUBMITTED, KycCaseStatus.UNDER_REVIEW));
        }
        if (riskLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("riskLevel"), riskLevel));
        }
        if (expiringWithinDays != null && expiringWithinDays > 0) {
            LocalDate today = LocalDate.now();
            Set<Long> ids = documentRepository.findAllByExpiryDateBetween(today, today.plusDays(expiringWithinDays)).stream()
                    .map(KycDocument::getKycCase)
                    .filter(Objects::nonNull)
                    .map(KycCase::getId)
                    .collect(java.util.stream.Collectors.toSet());
            spec = spec.and((root, query, cb) -> ids.isEmpty() ? cb.disjunction() : root.get("id").in(ids));
        }
        Page<KycCase> page = caseRepository.findAll(spec, pageable);
        return new PaginatedResponse<>(page.map(this::toResponse));
    }

    @Override
    public KycCaseResponse submit(String code) {
        KycCase kycCase = serviceCase(code);
        CaseAssessment assessment = assess(kycCase);
        if (!assessment.missingDocumentTypeCodes().isEmpty()) {
            throw new BadRequestException(
                    "KYC case is incomplete. Missing required documents: "
                            + String.join(", ", assessment.missingDocumentTypeCodes()));
        }

        kycCase.setStatus(KycCaseStatus.SUBMITTED);
        kycCase.setSubmittedAt(Instant.now());
        kycCase.setRiskLevel(calculateRiskLevel(kycCase));
        kycCase.setKycLevel(calculateKycLevel(kycCase));
        kycCase.setSlaDeadline(kycCase.getSubmittedAt().plus(slaHours(kycCase), ChronoUnit.HOURS));
        caseRepository.save(kycCase);

        if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
            Member member = memberById(kycCase.getOwnerId());
            memberService.ChangeStatus(member.getMemberId(), new UpdateStatusRequest(MemberStatus.UNDER_REVIEW.name()));
        } else if (kycCase.getOwnerType() == DocumentOwnerType.CUSTOMER) {
            kycCase.setStatus(KycCaseStatus.UNDER_REVIEW);
            caseRepository.save(kycCase);
        }

        publishCaseEvent("KYC_CASE_SUBMITTED", kycCase, null);

        return toResponse(kycCase);
    }

    @Override
    public KycCaseResponse approve(String code, KycDecisionRequest request) {
        KycCase kycCase = serviceCase(code);
        CaseAssessment assessment = assess(kycCase);
        if (!assessment.complete()) {
            throw new BadRequestException("KYC case cannot be approved while mandatory documents are missing");
        }
        if (!assessment.allVerified()) {
            throw new BadRequestException("KYC case cannot be approved while documents are not verified");
        }
        validateCrossDocumentRules(kycCase);

        kycCase.setStatus(KycCaseStatus.APPROVED);
        kycCase.setReviewedBy(request.getReviewedBy());
        kycCase.setReviewedAt(Instant.now());
        kycCase.setCompletedAt(Instant.now());
        kycCase.setDecisionComment(trimToNull(request.getComment()));
        kycCase.setKycLevel(calculateKycLevel(kycCase));
        kycCase.setRiskLevel(kycCase.getRiskLevel() == null ? calculateRiskLevel(kycCase) : kycCase.getRiskLevel());
        kycCase.setSlaDeadline(null);
        caseRepository.save(kycCase);

        if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
            Member member = memberById(kycCase.getOwnerId());
            memberService.ChangeStatus(member.getMemberId(), new UpdateStatusRequest(MemberStatus.ACTIVE.name()));
            provisionDefaultWallet("MEMBER", member.getMemberId());
        } else if (kycCase.getOwnerType() == DocumentOwnerType.CUSTOMER) {
            Customer customer = customerService.getCustomerForService(kycCase.getOwnerId());
            customer.setStatus(com.sni.bokaticowork.features.client.customer.enums.CustomerStatus.ACTIVE);
        }

        publishCaseEvent("KYC_CASE_APPROVED", kycCase, request.getReviewedBy());

        return toResponse(kycCase);
    }

    @Override
    public KycCaseResponse reject(String code, KycDecisionRequest request) {
        if (!StringUtils.hasText(request.getComment())) {
            throw new BadRequestException("A rejection reason is required");
        }

        KycCase kycCase = serviceCase(code);
        kycCase.setStatus(KycCaseStatus.PENDING_CORRECTION);
        kycCase.setReviewedBy(request.getReviewedBy());
        kycCase.setReviewedAt(Instant.now());
        kycCase.setDecisionComment(request.getComment().trim());
        caseRepository.save(kycCase);

        documentRepository.findAllByKycCaseOrderByIdAsc(kycCase).stream()
                .filter(item -> item.getStatus() == KycDocumentVerificationStatus.REJECTED || item.getStatus() == KycDocumentVerificationStatus.PENDING)
                .forEach(item -> verificationRepository.save(KycVerification.builder()
                        .kycDocument(item)
                        .verificationStatus(item.getStatus())
                        .verifiedBy(request.getReviewedBy())
                        .verifiedAt(Instant.now())
                        .notes(request.getComment().trim())
                        .build()));

        if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
            Member member = memberById(kycCase.getOwnerId());
            memberService.ChangeStatus(member.getMemberId(), new UpdateStatusRequest(MemberStatus.PENDING_CORRECTION.name()));
        } else if (kycCase.getOwnerType() == DocumentOwnerType.CUSTOMER) {
            Customer customer = customerService.getCustomerForService(kycCase.getOwnerId());
            customer.setStatus(com.sni.bokaticowork.features.client.customer.enums.CustomerStatus.PENDING);
        }

        publishCaseEvent("KYC_CASE_CORRECTION_REQUESTED", kycCase, request.getReviewedBy());

        return toResponse(kycCase);
    }

    private KycCase serviceCase(String code) {
        return caseRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("KYC case not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycRequirementStatus> getMissingRequirements(String code) {
        KycCase kycCase = serviceCase(code);
        List<DocumentRequirement> allRequirements = filteredRequirementsForCase(kycCase);
        CaseAssessment assessment = assess(kycCase);
        return allRequirements.stream()
                .filter(req -> Boolean.TRUE.equals(req.getRequired()))
                .filter(req -> assessment.missingDocumentTypeCodes().stream()
                        .anyMatch(missing -> missing.equalsIgnoreCase(req.getDocumentTypeCode())))
                .map(req -> KycRequirementStatus.builder()
                        .documentTypeCode(req.getDocumentTypeCode())
                        .documentTypeName(req.getDocumentTypeName())
                        .required(true)
                        .status(null)
                        .documentCode(null)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KycDashboardResponse dashboard() {
        Instant now = Instant.now();
        List<KycCase> cases = caseRepository.findAll();
        Map<String, Long> summary = new java.util.LinkedHashMap<>();
        summary.put("totalCases", (long) cases.size());
        for (KycCaseStatus status : KycCaseStatus.values()) {
            summary.put(status.name(), cases.stream().filter(item -> item.getStatus() == status).count());
        }

        long reviewedWithin24h = cases.stream()
                .filter(item -> item.getSubmittedAt() != null && item.getReviewedAt() != null)
                .filter(item -> ChronoUnit.HOURS.between(item.getSubmittedAt(), item.getReviewedAt()) <= 24)
                .count();
        long exceeding48h = cases.stream()
                .filter(item -> item.getSubmittedAt() != null)
                .filter(item -> item.getReviewedAt() == null || ChronoUnit.HOURS.between(item.getSubmittedAt(), item.getReviewedAt()) > 48)
                .filter(item -> item.getStatus() == KycCaseStatus.SUBMITTED || item.getStatus() == KycCaseStatus.UNDER_REVIEW)
                .count();
        BigDecimal avgReview = averageReviewHours(cases);

        LocalDate today = LocalDate.now();
        List<KycDocument> expiring60 = documentRepository.findAllByExpiryDateBetween(today, today.plusDays(60));
        KycDashboardResponse.ExpiringSoonMetrics expiringSoon = KycDashboardResponse.ExpiringSoonMetrics.builder()
                .within7Days(expiring60.stream().filter(item -> !item.getExpiryDate().isAfter(today.plusDays(7))).count())
                .within30Days(expiring60.stream().filter(item -> !item.getExpiryDate().isAfter(today.plusDays(30))).count())
                .within60Days((long) expiring60.size())
                .build();

        List<KycDashboardResponse.OwnerTypeMetrics> byOwnerType = java.util.Arrays.stream(DocumentOwnerType.values())
                .map(type -> KycDashboardResponse.OwnerTypeMetrics.builder()
                        .ownerType(type)
                        .count(cases.stream().filter(item -> item.getOwnerType() == type).count())
                        .pendingReview(cases.stream().filter(item -> item.getOwnerType() == type)
                                .filter(item -> item.getStatus() == KycCaseStatus.SUBMITTED || item.getStatus() == KycCaseStatus.UNDER_REVIEW)
                                .count())
                        .build())
                .filter(item -> item.getCount() > 0)
                .toList();

        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        List<KycDashboardResponse.ActivityMetrics> recentActivity = List.of(
                KycDashboardResponse.ActivityMetrics.builder()
                        .action("APPROVED")
                        .period("LAST_7_DAYS")
                        .count(cases.stream().filter(item -> item.getStatus() == KycCaseStatus.APPROVED)
                                .filter(item -> item.getCompletedAt() != null && !item.getCompletedAt().isBefore(sevenDaysAgo)).count())
                        .build(),
                KycDashboardResponse.ActivityMetrics.builder()
                        .action("REJECTED")
                        .period("LAST_7_DAYS")
                        .count(cases.stream().filter(item -> item.getStatus() == KycCaseStatus.PENDING_CORRECTION || item.getStatus() == KycCaseStatus.REJECTED)
                                .filter(item -> item.getReviewedAt() != null && !item.getReviewedAt().isBefore(sevenDaysAgo)).count())
                        .build()
        );

        return KycDashboardResponse.builder()
                .generatedAt(now)
                .summary(summary)
                .sla(KycDashboardResponse.SlaMetrics.builder()
                        .casesReviewedWithin24h(reviewedWithin24h)
                        .casesExceeding48h(exceeding48h)
                        .avgReviewTimeHours(avgReview)
                        .build())
                .expiringSoon(expiringSoon)
                .byOwnerType(byOwnerType)
                .recentActivity(recentActivity)
                .build();
    }

    @Override
    public KycCaseResponse assign(String code, KycAssignRequest request) {
        Long assigneeId = request.getAssignedTo();
        if (assigneeId == null && StringUtils.hasText(request.getEmail())) {
            Users user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                    .orElseThrow(() -> new BadRequestException("No user found with email: " + request.getEmail()));
            assigneeId = user.getId();
        }
        if (assigneeId == null) {
            throw new BadRequestException("Either assignedTo (ID) or email is required");
        }

        KycCase kycCase = serviceCase(code);
        Instant now = Instant.now();
        kycCase.setAssignedTo(assigneeId);
        kycCase.setAssignedAt(now);
        if (kycCase.getSubmittedAt() == null) {
            kycCase.setSubmittedAt(now);
        }
        kycCase.setSlaDeadline(kycCase.getSubmittedAt().plus(slaHours(kycCase), ChronoUnit.HOURS));
        if (kycCase.getStatus() == KycCaseStatus.SUBMITTED) {
            kycCase.setStatus(KycCaseStatus.UNDER_REVIEW);
        }
        caseRepository.save(kycCase);
        publishCaseEvent("KYC_CASE_ASSIGNED", kycCase, kycCase.getAssignedTo());
        return toResponse(kycCase);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<KycCaseResponse> myQueue(Long userId, Pageable pageable) {
        Long resolvedUserId = userId != null ? userId : currentUserId();
        Page<KycCase> page = caseRepository.findAllByAssignedToOrderBySubmittedAtAsc(resolvedUserId, pageable);
        return new PaginatedResponse<>(page.map(this::toResponse));
    }

    @Override
    public KycCaseNoteResponse addNote(String code, KycCaseNoteRequest request) {
        KycCase kycCase = serviceCase(code);
        KycCaseNote note = KycCaseNote.builder()
                .kycCase(kycCase)
                .content(request.getContent().trim())
                .authorId(request.getAuthorId())
                .internal(request.getInternal() == null ? Boolean.TRUE : request.getInternal())
                .build();
        noteRepository.save(note);
        publishCaseEvent("KYC_CASE_NOTE_ADDED", kycCase, request.getAuthorId());
        return mapper.toNoteResponse(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycCaseNoteResponse> listNotes(String code) {
        return noteRepository.findAllByKycCaseOrderByCreatedAtDesc(serviceCase(code)).stream()
                .map(mapper::toNoteResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycTimelineEntryResponse> timeline(String code) {
        KycCase kycCase = serviceCase(code);
        List<KycTimelineEntryResponse> entries = new ArrayList<>();
        entries.add(KycTimelineEntryResponse.builder()
                .timestamp(kycCase.getStartedAt())
                .action("CASE_CREATED")
                .actor("SYSTEM")
                .description("KYC case created")
                .build());
        if (kycCase.getSubmittedAt() != null) {
            entries.add(KycTimelineEntryResponse.builder()
                    .timestamp(kycCase.getSubmittedAt())
                    .action("CASE_SUBMITTED")
                    .actor(ownerActor(kycCase))
                    .description("KYC case submitted")
                    .build());
        }
        List<KycDocument> documents = documentRepository.findAllByKycCaseOrderByIdAsc(kycCase);
        for (KycDocument document : documents) {
            entries.add(KycTimelineEntryResponse.builder()
                    .timestamp(document.getDocument() == null ? kycCase.getStartedAt() : document.getDocument().getUploadedAt())
                    .action("DOCUMENT_UPLOADED")
                    .actor(ownerActor(kycCase))
                    .description("Document " + document.getDocumentType() + " submitted")
                    .build());
        }
        verificationRepository.findAllByKycDocumentInOrderByVerifiedAtDesc(documents).forEach(verification ->
                entries.add(KycTimelineEntryResponse.builder()
                        .timestamp(verification.getVerifiedAt())
                        .action("DOCUMENT_" + verification.getVerificationStatus())
                        .actor(verification.getVerifiedBy() == null ? "SYSTEM" : "USR-" + verification.getVerifiedBy())
                        .description(emptyToDefault(verification.getNotes(), "Document review recorded"))
                        .build()));
        noteRepository.findAllByKycCaseOrderByCreatedAtDesc(kycCase).forEach(note ->
                entries.add(KycTimelineEntryResponse.builder()
                        .timestamp(note.getCreatedAt())
                        .action("NOTE_ADDED")
                        .actor("USR-" + note.getAuthorId())
                        .description(note.getContent())
                        .build()));
        outboxEventRepository.findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("KYC_CASE", code).forEach(event ->
                entries.add(KycTimelineEntryResponse.builder()
                        .timestamp(event.getCreatedAt())
                        .action(event.getEventType())
                        .actor("SYSTEM")
                        .description("Event " + event.getEventType() + " published")
                        .build()));
        entries.sort(Comparator.comparing(KycTimelineEntryResponse::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
        return entries;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<KycCaseResponse> expiringSoon(Integer days, Pageable pageable) {
        int horizon = days == null || days <= 0 ? 30 : days;
        LocalDate today = LocalDate.now();
        Set<Long> caseIds = documentRepository.findAllByExpiryDateBetween(today, today.plusDays(horizon)).stream()
                .map(KycDocument::getKycCase)
                .filter(Objects::nonNull)
                .map(KycCase::getId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        Specification<KycCase> spec = (root, query, cb) ->
                caseIds.isEmpty() ? cb.disjunction() : root.get("id").in(caseIds);
        Page<KycCase> page = caseRepository.findAll(spec, pageable);
        return new PaginatedResponse<>(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycExpiryDocumentStatus> expiryStatus(String code) {
        LocalDate today = LocalDate.now();
        return documentRepository.findAllByKycCaseOrderByIdAsc(serviceCase(code)).stream()
                .filter(item -> item.getExpiryDate() != null)
                .map(item -> KycExpiryDocumentStatus.builder()
                        .documentCode(item.getDocument() == null ? null : item.getDocument().getCode())
                        .documentType(item.getDocumentType())
                        .expiryDate(item.getExpiryDate())
                        .daysUntilExpiry(ChronoUnit.DAYS.between(today, item.getExpiryDate()))
                        .expired(item.getExpiryDate().isBefore(today))
                        .status(item.getStatus())
                        .build())
                .toList();
    }

    @Override
    public KycCaseResponse updateRiskLevel(String code, KycRiskLevelRequest request) {
        KycCase kycCase = serviceCase(code);
        kycCase.setRiskLevel(request.getRiskLevel());
        kycCase.setDecisionComment(trimToNull(request.getComment()));
        caseRepository.save(kycCase);
        publishCaseEvent("KYC_RISK_LEVEL_UPDATED", kycCase, request.getReviewedBy());
        return toResponse(kycCase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycDocumentResponse> reviewQueue() {
        return reviewDocuments(List.of(KycDocumentVerificationStatus.PENDING));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycDocumentResponse> reviewedDocuments(List<KycDocumentVerificationStatus> statuses) {
        List<KycDocumentVerificationStatus> effectiveStatuses = statuses == null || statuses.isEmpty()
                ? List.of(KycDocumentVerificationStatus.VERIFIED, KycDocumentVerificationStatus.REJECTED)
                : statuses;
        return reviewDocuments(effectiveStatuses);
    }

    @Override
    @Transactional(readOnly = true)
    public KycDocumentOcrResultResponse getOcrResult(String documentCode) {
        if (!hasUsableDocumentCode(documentCode)) {
            return KycDocumentOcrResultResponse.builder().build();
        }
        String normalizedCode = documentCode.trim();
        KycDocument document = documentRepository.findAllByDocument_Code(normalizedCode).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));
        return ocrResultRepository.findByKycDocument(document)
                .map(mapper::toOcrResultResponse)
                .orElseGet(() -> KycDocumentOcrResultResponse.builder()
                        .documentCode(normalizedCode)
                        .build());
    }

    private List<KycDocumentResponse> reviewDocuments(List<KycDocumentVerificationStatus> statuses) {
        return documentRepository.findAllReviewDocumentsByStatusIn(statuses).stream()
                .map(mapper::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KycBulkActionResponse bulkApprove(KycBulkApproveRequest request) {
        List<KycBulkItemResult> results = request.getDocumentIds().stream()
                .map(code -> bulkApproveOne(code, request.getReviewedBy()))
                .toList();
        return bulkResponse(results);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KycBulkActionResponse bulkReject(KycBulkRejectRequest request) {
        List<KycBulkItemResult> results = request.getItems().stream()
                .map(item -> bulkRejectOne(item.getDocumentCode(), request.getReviewedBy(), item.getReason()))
                .toList();
        return bulkResponse(results);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(String code, boolean includeInternalNotes) {
        KycCaseResponse kycCase = getByCode(code);
        Context context = new Context(appLocale);
        context.setVariable("case", kycCase);
        context.setVariable("timeline", timeline(code));
        context.setVariable("expiry", expiryStatus(code));
        context.setVariable("notes", includeInternalNotes ? listNotes(code) : List.of());
        context.setVariable("generatedAt", Instant.now());
        String html = templateEngine.process("kyc/kyc-case-report", context);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document xhtml = Jsoup.parse(html);
            xhtml.outputSettings()
                    .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.xhtml)
                    .charset(StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new W3CDom().fromJsoup(xhtml), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to export KYC case PDF", ex);
        }
    }

    private BigDecimal averageReviewHours(List<KycCase> cases) {
        List<Long> reviewDurations = cases.stream()
                .filter(item -> item.getSubmittedAt() != null && item.getReviewedAt() != null)
                .map(item -> ChronoUnit.MINUTES.between(item.getSubmittedAt(), item.getReviewedAt()))
                .toList();
        if (reviewDurations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalMinutes = BigDecimal.valueOf(reviewDurations.stream().mapToLong(Long::longValue).sum());
        return totalMinutes
                .divide(BigDecimal.valueOf(reviewDurations.size()), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private long slaHours(KycCase kycCase) {
        return switch (kycCase.getOwnerType()) {
            case BUSINESS -> 72L;
            case CUSTOMER -> 48L;
            default -> 24L;
        };
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        throw new BadRequestException("Current user could not be resolved");
    }

    private String ownerActor(KycCase kycCase) {
        KycCaseResponse response = toResponse(kycCase);
        return StringUtils.hasText(response.getOwnerCode()) ? response.getOwnerCode() : kycCase.getOwnerType() + "-" + kycCase.getOwnerId();
    }

    private String emptyToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private KycBulkItemResult bulkApproveOne(String documentCode, Long reviewedBy) {
        try {
            DocumentReviewDecisionRequest decision = new DocumentReviewDecisionRequest();
            decision.setReviewedBy(reviewedBy);
            decision.setComment("Bulk approved");
            documentService.approve(documentCode, decision);
            return KycBulkItemResult.builder().documentCode(documentCode).success(true).build();
        } catch (Exception ex) {
            return KycBulkItemResult.builder().documentCode(documentCode).success(false).error(ex.getMessage()).build();
        }
    }

    private KycBulkItemResult bulkRejectOne(String documentCode, Long reviewedBy, String reason) {
        try {
            DocumentReviewDecisionRequest decision = new DocumentReviewDecisionRequest();
            decision.setReviewedBy(reviewedBy);
            decision.setComment(reason);
            decision.setRejectionReasonDetail(reason);
            documentService.reject(documentCode, decision);
            return KycBulkItemResult.builder().documentCode(documentCode).success(true).build();
        } catch (Exception ex) {
            return KycBulkItemResult.builder().documentCode(documentCode).success(false).error(ex.getMessage()).build();
        }
    }

    private KycBulkActionResponse bulkResponse(List<KycBulkItemResult> results) {
        int succeeded = (int) results.stream().filter(item -> Boolean.TRUE.equals(item.getSuccess())).count();
        return KycBulkActionResponse.builder()
                .processed(results.size())
                .succeeded(succeeded)
                .failed(results.size() - succeeded)
                .results(results)
                .build();
    }

    private KycRiskLevel defaultRiskLevel(DocumentOwnerType ownerType) {
        return ownerType == DocumentOwnerType.BUSINESS ? KycRiskLevel.MEDIUM : KycRiskLevel.LOW;
    }

    private KycRiskLevel calculateRiskLevel(KycCase kycCase) {
        if (kycCase.getRiskLevel() == KycRiskLevel.HIGH || kycCase.getRiskLevel() == KycRiskLevel.VERY_HIGH) {
            return kycCase.getRiskLevel();
        }
        return defaultRiskLevel(kycCase.getOwnerType());
    }

    private Integer calculateKycLevel(KycCase kycCase) {
        List<KycDocument> documents = documentRepository.findAllByKycCaseOrderByIdAsc(kycCase);
        long verified = documents.stream().filter(item -> item.getStatus() == KycDocumentVerificationStatus.VERIFIED).count();
        boolean hasBusinessDocument = documents.stream().anyMatch(item -> item.getDocumentType() != null && item.getDocumentType().startsWith("BUSINESS_"));
        boolean hasSelfieOrAml = documents.stream().anyMatch(item -> item.getDocumentType() != null
                && (item.getDocumentType().contains("SELFIE") || item.getDocumentType().contains("LIVENESS") || item.getDocumentType().contains("AML")));
        if (hasSelfieOrAml || kycCase.getRiskLevel() == KycRiskLevel.HIGH || kycCase.getRiskLevel() == KycRiskLevel.VERY_HIGH) {
            return 4;
        }
        if (kycCase.getOwnerType() == DocumentOwnerType.BUSINESS || hasBusinessDocument || verified >= 2) {
            return 3;
        }
        if (verified >= 1) {
            return 2;
        }
        return 1;
    }

    private void validateCrossDocumentRules(KycCase kycCase) {
        List<KycCrossValidationRule> rules = crossValidationRuleRepository.findAllByActiveTrueOrderByIdAsc();
        if (rules.isEmpty()) {
            return;
        }
        List<KycDocument> documents = documentRepository.findAllByKycCaseOrderByIdAsc(kycCase);
        for (KycCrossValidationRule rule : rules) {
            KycDocument first = documents.stream()
                    .filter(item -> rule.getDocumentTypeCode1().equalsIgnoreCase(item.getDocumentType()))
                    .findFirst()
                    .orElse(null);
            KycDocument second = documents.stream()
                    .filter(item -> rule.getDocumentTypeCode2().equalsIgnoreCase(item.getDocumentType()))
                    .findFirst()
                    .orElse(null);
            if (first == null || second == null) {
                continue;
            }
            String firstValue = ocrField(first, rule.getFieldToCompare());
            String secondValue = ocrField(second, rule.getFieldToCompare());
            if (StringUtils.hasText(firstValue) && StringUtils.hasText(secondValue)
                    && !firstValue.equalsIgnoreCase(secondValue)
                    && Boolean.TRUE.equals(rule.getBlocking())) {
                throw new BadRequestException("KYC cross-validation failed for field " + rule.getFieldToCompare());
            }
        }
    }

    private String ocrField(KycDocument document, String field) {
        KycDocumentOcrResult result = ocrResultRepository.findByKycDocument(document).orElse(null);
        if (result == null || !StringUtils.hasText(field)) {
            return null;
        }
        return switch (field.trim().toLowerCase(Locale.ROOT)) {
            case "first_name", "firstname" -> result.getExtractedFirstName();
            case "last_name", "lastname", "name" -> result.getExtractedLastName();
            case "date_of_birth", "dob" -> result.getExtractedDateOfBirth() == null ? null : result.getExtractedDateOfBirth().toString();
            case "expiry_date" -> result.getExtractedExpiryDate() == null ? null : result.getExtractedExpiryDate().toString();
            case "document_number" -> result.getExtractedDocumentNumber();
            case "nationality" -> result.getExtractedNationality();
            default -> null;
        };
    }

    private KycCaseResponse toResponse(KycCase kycCase) {
        List<DocumentRequirement> allRequirements = filteredRequirementsForCase(kycCase);
        List<KycDocument> uploadedDocuments = documentRepository.findAllByKycCaseOrderByIdAsc(kycCase);

        CaseAssessment assessment = buildAssessment(allRequirements, uploadedDocuments);

        List<KycRequirementStatus> requirementStatuses = allRequirements.stream()
                .map(req -> {
                    KycDocument match = uploadedDocuments.stream()
                            .filter(doc -> doc.getDocumentType().equalsIgnoreCase(req.getDocumentTypeCode()))
                            .findFirst()
                            .orElse(null);
                    DocumentType docType = documentTypeRepository.findByCode(req.getDocumentTypeCode()).orElse(null);
                    Boolean requiresBack = docType != null ? docType.getRequiresBackSide() : Boolean.FALSE;
                    return KycRequirementStatus.builder()
                            .documentTypeCode(req.getDocumentTypeCode())
                            .documentTypeName(req.getDocumentTypeName())
                            .required(Boolean.TRUE.equals(req.getRequired()))
                            .requiresBackSide(requiresBack)
                            .status(match != null ? match.getStatus() : null)
                            .documentCode(match != null && match.getDocument() != null ? match.getDocument().getCode() : null)
                            .backDocumentCode(match != null && match.getBackDocument() != null ? match.getBackDocument().getCode() : null)
                            .build();
                })
                .toList();

        List<KycDocumentResponse> documents = uploadedDocuments.stream()
                .map(mapper::toDocumentResponse)
                .toList();

        KycCaseResponse response = mapper.toResponse(kycCase);
        response.setComplete(assessment.complete());
        response.setApproved(assessment.allVerified());
        response.setMissingDocumentTypeCodes(assessment.missingDocumentTypeCodes());
        response.setRequirements(requirementStatuses);
        response.setDocuments(documents);
        return response;
    }

    private CaseAssessment assess(KycCase kycCase) {
        return buildAssessment(
                filteredRequirementsForCase(kycCase),
                documentRepository.findAllByKycCaseOrderByIdAsc(kycCase)
        );
    }

    private CaseAssessment buildAssessment(List<DocumentRequirement> requirements, List<KycDocument> documents) {
        List<String> missing = new ArrayList<>();

        for (DocumentRequirement req : requirements) {
            if (!Boolean.TRUE.equals(req.getRequired())) continue;

            KycDocument match = documents.stream()
                    .filter(doc -> doc.getDocumentType().equalsIgnoreCase(req.getDocumentTypeCode()))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                missing.add(req.getDocumentTypeCode());
                continue;
            }

            DocumentType docType = documentTypeRepository.findByCode(req.getDocumentTypeCode()).orElse(null);
            if (docType != null && Boolean.TRUE.equals(docType.getRequiresBackSide()) && match.getBackDocument() == null) {
                missing.add(req.getDocumentTypeCode() + "_BACK");
            }
        }

        boolean allVerified = requirements.stream()
                .filter(req -> Boolean.TRUE.equals(req.getRequired()))
                .allMatch(req -> documents.stream().anyMatch(doc ->
                        doc.getDocumentType().equalsIgnoreCase(req.getDocumentTypeCode())
                                && doc.getStatus() == KycDocumentVerificationStatus.VERIFIED
                ));

        return new CaseAssessment(missing.isEmpty(), allVerified, missing);
    }

    private OwnerResolution resolveOwner(DocumentOwnerType ownerType, String ownerCode) {
        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            throw new BadRequestException("KYC owner is required");
        }

        return switch (ownerType) {
            case MEMBER -> {
                Member member = memberService.getByMemberIdForService(ownerCode.trim());
                yield new OwnerResolution(DocumentOwnerType.MEMBER, member.getId(), member.getMemberId());
            }
            case CUSTOMER -> {
                Customer customer = customerService.getCustomerForService(ownerCode.trim());
                yield new OwnerResolution(DocumentOwnerType.CUSTOMER, customer.getId(), customer.getCustomerId());
            }
            case BUSINESS -> {
                BusinessEntity business = businessService.serviceBusinessByCode(ownerCode.trim());
                yield new OwnerResolution(DocumentOwnerType.BUSINESS, business.getId(), business.getCode());
            }
            default -> throw new BadRequestException("KYC is only supported for member, customer or business");
        };
    }

    private void provisionDefaultWallet(String ownerType, String ownerCode) {
        try {
            walletService.getOrCreate(ownerType, ownerCode, "XAF");
        } catch (Exception ex) {
            // Non-blocking: wallet provisioning failure must not roll back KYC approval
            org.slf4j.LoggerFactory.getLogger(KycServiceImpl.class)
                    .warn("Failed to provision default wallet for {} {}: {}", ownerType, ownerCode, ex.getMessage());
        }
    }

    private Member memberById(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(member -> !Boolean.TRUE.equals(member.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean hasUsableDocumentCode(String documentCode) {
        if (!StringUtils.hasText(documentCode)) {
            return false;
        }
        String value = documentCode.trim();
        return !value.equals("-")
                && !value.equals("—")
                && !value.equals("–")
                && !value.equalsIgnoreCase("null")
                && !value.equalsIgnoreCase("undefined");
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycCaseRequirementResponse> listCaseRequirements(String caseCode) {
        KycCase kycCase = serviceCase(caseCode);
        return caseRequirementRepository.findAllByKycCaseOrderByDocumentTypeNameAsc(kycCase).stream()
                .map(this::toCaseRequirementResponse)
                .toList();
    }

    @Override
    public KycCaseRequirementResponse addCaseRequirement(String caseCode, KycCaseRequirementRequest request) {
        KycCase kycCase = serviceCase(caseCode);
        String normalizedCode = request.getDocumentTypeCode().trim().toUpperCase(Locale.ROOT);

        if (caseRequirementRepository.existsByKycCaseAndDocumentTypeCode(kycCase, normalizedCode)) {
            throw new BadRequestException("Requirement already exists for document type: " + normalizedCode);
        }

        String typeName = request.getDocumentTypeName();
        if (typeName == null || typeName.isBlank()) {
            typeName = documentTypeRepository.findByCode(normalizedCode)
                    .map(DocumentType::getName)
                    .orElse(normalizedCode);
        }

        KycCaseRequirement entity = KycCaseRequirement.builder()
                .kycCase(kycCase)
                .documentTypeCode(normalizedCode)
                .documentTypeName(typeName)
                .required(request.getRequired() != null ? request.getRequired() : Boolean.TRUE)
                .build();
        caseRequirementRepository.save(entity);
        publishCaseEvent("KYC_CASE_REQUIREMENT_ADDED", kycCase, null);
        return toCaseRequirementResponse(entity);
    }

    @Override
    public void removeCaseRequirement(String caseCode, String documentTypeCode) {
        KycCase kycCase = serviceCase(caseCode);
        String normalizedCode = documentTypeCode.trim().toUpperCase(Locale.ROOT);
        KycCaseRequirement requirement = caseRequirementRepository
                .findByKycCaseAndDocumentTypeCode(kycCase, normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Case requirement not found: " + normalizedCode));
        requirement.setActive(false);
        caseRequirementRepository.save(requirement);
        publishCaseEvent("KYC_CASE_REQUIREMENT_REMOVED", kycCase, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycDocumentResponse> caseReviewQueue(String caseCode) {
        KycCase kycCase = serviceCase(caseCode);
        return documentRepository.findAllReviewDocumentsByKycCaseAndStatusIn(
                        kycCase, List.of(KycDocumentVerificationStatus.PENDING))
                .stream()
                .map(mapper::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KycBulkActionResponse caseBulkApprove(String caseCode, KycCaseBulkApproveRequest request) {
        KycCase kycCase = serviceCase(caseCode);
        List<KycDocument> caseDocuments = documentRepository.findAllByKycCaseOrderByIdAsc(kycCase);
        List<KycBulkItemResult> results = request.getDocumentCodes().stream()
                .map(code -> {
                    boolean belongsToCase = caseDocuments.stream()
                            .anyMatch(d -> d.getDocument() != null && code.equals(d.getDocument().getCode()));
                    if (!belongsToCase) {
                        return KycBulkItemResult.builder()
                                .documentCode(code).success(false)
                                .error("Document does not belong to case " + caseCode).build();
                    }
                    return bulkApproveOne(code, request.getReviewedBy());
                })
                .toList();
        return bulkResponse(results);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KycBulkActionResponse caseBulkReject(String caseCode, KycCaseBulkRejectRequest request) {
        KycCase kycCase = serviceCase(caseCode);
        List<KycDocument> caseDocuments = documentRepository.findAllByKycCaseOrderByIdAsc(kycCase);
        List<KycBulkItemResult> results = request.getItems().stream()
                .map(item -> {
                    boolean belongsToCase = caseDocuments.stream()
                            .anyMatch(d -> d.getDocument() != null && item.getDocumentCode().equals(d.getDocument().getCode()));
                    if (!belongsToCase) {
                        return KycBulkItemResult.builder()
                                .documentCode(item.getDocumentCode()).success(false)
                                .error("Document does not belong to case " + caseCode).build();
                    }
                    return bulkRejectOne(item.getDocumentCode(), request.getReviewedBy(), item.getReason());
                })
                .toList();
        return bulkResponse(results);
    }

    @Override
    public KycDocumentResponse caseCorrectionRequest(String caseCode, KycCaseCorrectionRequest request) {
        KycCase kycCase = serviceCase(caseCode);
        KycDocument kycDoc = documentRepository.findAllByKycCaseOrderByIdAsc(kycCase).stream()
                .filter(d -> d.getDocument() != null && request.getDocumentCode().equals(d.getDocument().getCode()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in case " + caseCode));

        DocumentCorrectionRequest corrReq = new DocumentCorrectionRequest();
        corrReq.setReviewedBy(request.getReviewedBy());
        corrReq.setCorrectionNote(request.getCorrectionNote());
        corrReq.setDeadlineDays(request.getDeadlineDays());
        documentService.requestCorrection(kycDoc.getDocument().getCode(), corrReq);

        kycDoc.setStatus(KycDocumentVerificationStatus.REJECTED);
        documentRepository.save(kycDoc);

        publishCaseEvent("KYC_DOCUMENT_CORRECTION_REQUESTED", kycCase, request.getReviewedBy());
        return mapper.toDocumentResponse(kycDoc);
    }

    private KycCaseRequirementResponse toCaseRequirementResponse(KycCaseRequirement requirement) {
        DocumentType docType = documentTypeRepository.findByCode(requirement.getDocumentTypeCode()).orElse(null);
        return KycCaseRequirementResponse.builder()
                .id(requirement.getId())
                .kycCaseCode(requirement.getKycCase().getCode())
                .documentTypeCode(requirement.getDocumentTypeCode())
                .documentTypeName(requirement.getDocumentTypeName())
                .required(requirement.getRequired())
                .requiresBackSide(docType != null ? docType.getRequiresBackSide() : Boolean.FALSE)
                .active(requirement.getActive())
                .build();
    }

    private record OwnerResolution(DocumentOwnerType ownerType, Long ownerId, String ownerCode) {
    }

    private record CaseAssessment(boolean complete, boolean allVerified, List<String> missingDocumentTypeCodes) {
    }

    private List<DocumentRequirement> filteredRequirementsForCase(KycCase kycCase) {
        List<KycCaseRequirement> caseRequirements = caseRequirementRepository
                .findAllByKycCaseAndActiveTrueOrderByDocumentTypeNameAsc(kycCase);

        if (!caseRequirements.isEmpty()) {
            return caseRequirements.stream()
                    .map(cr -> {
                        DocumentRequirement dr = new DocumentRequirement();
                        dr.setOwnerType(kycCase.getOwnerType());
                        dr.setDocumentTypeCode(cr.getDocumentTypeCode());
                        dr.setDocumentTypeName(cr.getDocumentTypeName());
                        dr.setRequired(cr.getRequired());
                        dr.setActive(cr.getActive());
                        return dr;
                    })
                    .toList();
        }

        List<DocumentRequirement> all = requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(kycCase.getOwnerType());
        if (kycCase.getOwnerType() == DocumentOwnerType.BUSINESS) {
            String legalForm = resolveBusinessLegalForm(kycCase.getOwnerId());
            if (legalForm != null) {
                all = all.stream()
                        .filter(r -> r.getBusinessLegalForm() == null
                                || r.getBusinessLegalForm().isBlank()
                                || legalForm.equalsIgnoreCase(r.getBusinessLegalForm()))
                        .toList();
            }
        }
        return all;
    }

    private String resolveBusinessLegalForm(Long ownerId) {
        try {
            return businessService.serviceBusinessById(ownerId).getLegalForm();
        } catch (Exception e) {
            return null;
        }
    }

    private void publishCaseEvent(String eventType, KycCase kycCase, Long actorId) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("kycCaseCode", kycCase.getCode());
        payload.put("ownerType", kycCase.getOwnerType());
        payload.put("ownerId", kycCase.getOwnerId());
        payload.put("status", kycCase.getStatus());
        payload.put("actorId", actorId);
        outboxService.publish(eventType, "KYC_CASE", kycCase.getCode(), payload);
    }
}
