package com.sni.bokaticowork.features.document.kyc.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
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
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.dto.request.CreateKycCaseRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentRequirementResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import com.sni.bokaticowork.features.document.kyc.mapper.interfaces.KycMapper;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycVerification;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycVerificationRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class KycServiceImpl implements KycService {

    private final KycCaseRepository caseRepository;
    private final KycDocumentRepository documentRepository;
    private final KycVerificationRepository verificationRepository;
    private final DocumentRequirementRepository requirementRepository;
    private final CustomerService customerService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final BusinessService businessService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;
    private final KycAutomationService kycAutomationService;
    private final KycMapper mapper;

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
                .build();
        caseRepository.save(entity);

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

        kycCase.setStatus(KycCaseStatus.APPROVED);
        kycCase.setReviewedBy(request.getReviewedBy());
        kycCase.setReviewedAt(Instant.now());
        kycCase.setCompletedAt(Instant.now());
        kycCase.setDecisionComment(trimToNull(request.getComment()));
        caseRepository.save(kycCase);

        if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
            Member member = memberById(kycCase.getOwnerId());
            memberService.ChangeStatus(member.getMemberId(), new UpdateStatusRequest(MemberStatus.ACTIVE.name()));
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

        publishCaseEvent("KYC_CASE_REJECTED", kycCase, request.getReviewedBy());
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
        List<DocumentRequirement> allRequirements = requirementRepository
                .findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(kycCase.getOwnerType());
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

    private KycCaseResponse toResponse(KycCase kycCase) {
        List<DocumentRequirement> allRequirements = requirementRepository
                .findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(kycCase.getOwnerType());
        List<KycDocument> uploadedDocuments = documentRepository.findAllByKycCaseOrderByIdAsc(kycCase);

        CaseAssessment assessment = buildAssessment(allRequirements, uploadedDocuments);

        List<KycRequirementStatus> requirementStatuses = allRequirements.stream()
                .map(req -> {
                    KycDocument match = uploadedDocuments.stream()
                            .filter(doc -> doc.getDocumentType().equalsIgnoreCase(req.getDocumentTypeCode()))
                            .findFirst()
                            .orElse(null);
                    return KycRequirementStatus.builder()
                            .documentTypeCode(req.getDocumentTypeCode())
                            .documentTypeName(req.getDocumentTypeName())
                            .required(Boolean.TRUE.equals(req.getRequired()))
                            .status(match != null ? match.getStatus() : null)
                            .documentCode(match != null && match.getDocument() != null ? match.getDocument().getCode() : null)
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
        resolveOwnerInfo(kycCase, response);
        return response;
    }

    private void resolveOwnerInfo(KycCase kycCase, KycCaseResponse response) {
        try {
            switch (kycCase.getOwnerType()) {
                case MEMBER -> {
                    Member member = memberRepository.findById(kycCase.getOwnerId()).orElse(null);
                    if (member != null) {
                        response.setOwnerName(member.getDisplayName());
                        response.setOwnerCode(member.getMemberId());
                    }
                }
                case CUSTOMER -> {
                    Customer customer = customerService.getCustomerForService(kycCase.getOwnerId());
                    String name = StringUtils.hasText(customer.getCompanyName())
                            ? customer.getCompanyName()
                            : ((customer.getFirstname() == null ? "" : customer.getFirstname()) + " " + (customer.getLastname() == null ? "" : customer.getLastname())).trim();
                    response.setOwnerName(name);
                    response.setOwnerCode(customer.getCustomerId());
                }
                case BUSINESS -> {
                    BusinessEntity business = businessService.serviceBusinessById(kycCase.getOwnerId());
                    response.setOwnerName(business.getName());
                    response.setOwnerCode(business.getCode());
                }
                default -> { }
            }
        } catch (Exception ignored) { }
    }

    private CaseAssessment assess(KycCase kycCase) {
        return buildAssessment(
                requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(kycCase.getOwnerType()),
                documentRepository.findAllByKycCaseOrderByIdAsc(kycCase)
        );
    }

    private CaseAssessment buildAssessment(List<DocumentRequirement> requirements, List<KycDocument> documents) {
        List<String> missing = requirements.stream()
                .filter(DocumentRequirement::getRequired)
                .map(DocumentRequirement::getDocumentTypeCode)
                .filter(typeCode -> documents.stream().noneMatch(doc -> doc.getDocumentType().equalsIgnoreCase(typeCode)))
                .toList();

        boolean allVerified = requirements.stream()
                .filter(DocumentRequirement::getRequired)
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

    private Member memberById(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(member -> !Boolean.TRUE.equals(member.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record OwnerResolution(DocumentOwnerType ownerType, Long ownerId, String ownerCode) {
    }

    private record CaseAssessment(boolean complete, boolean allVerified, List<String> missingDocumentTypeCodes) {
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
