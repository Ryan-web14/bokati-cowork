package com.sni.bokaticowork.features.portal.document.kyc.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycService;
import com.sni.bokaticowork.features.portal.document.kyc.dto.response.ClientKycCompletionResponse;
import com.sni.bokaticowork.features.portal.document.kyc.dto.response.ClientKycDocumentResponse;
import com.sni.bokaticowork.features.portal.document.kyc.dto.response.ClientKycRequirementResponse;
import com.sni.bokaticowork.features.portal.document.kyc.dto.response.ClientKycStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientKycService {

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf", "image/jpeg", "image/png"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private final KycService kycService;
    private final KycCaseRepository kycCaseRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final KycAutomationService kycAutomationService;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final DocumentRequirementRepository requirementRepository;

    @Transactional(readOnly = true)
    public ClientKycStatusResponse getMyCase(Member member) {
        KycCase kycCase = resolveCase(member);
        KycCaseResponse caseResponse = kycService.getByCode(kycCase.getCode());
        return toStatusResponse(caseResponse, kycCase);
    }

    @Transactional(readOnly = true)
    public List<ClientKycRequirementResponse> getRequirements(Member member) {
        List<DocumentRequirement> reqs = requirementRepository
                .findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(DocumentOwnerType.MEMBER);
        KycCase kycCase = resolveCase(member);
        List<KycRequirementStatus> statuses = kycService.getMissingRequirements(kycCase.getCode());
        return reqs.stream().map(r -> {
            KycRequirementStatus match = statuses.stream()
                    .filter(s -> s.getDocumentTypeCode().equals(r.getDocumentTypeCode()))
                    .findFirst().orElse(null);
            return ClientKycRequirementResponse.builder()
                    .documentTypeCode(r.getDocumentTypeCode())
                    .documentTypeName(r.getDocumentTypeName())
                    .required(Boolean.TRUE.equals(r.getRequired()))
                    .status(match != null && match.getStatus() != null ? match.getStatus().name() : null)
                    .documentCode(match != null ? match.getDocumentCode() : null)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public ClientKycCompletionResponse getCompletion(Member member) {
        KycCase kycCase = resolveCase(member);
        KycCaseResponse caseResponse = kycService.getByCode(kycCase.getCode());
        List<ClientKycRequirementResponse> requirements = getRequirements(member);
        long totalRequired = requirements.stream().filter(ClientKycRequirementResponse::isRequired).count();
        long totalSubmitted = requirements.stream()
                .filter(r -> r.getStatus() != null && !r.getStatus().isEmpty()).count();
        long totalVerified = requirements.stream()
                .filter(r -> "VERIFIED".equals(r.getStatus())).count();
        int percent = totalRequired == 0 ? 0 : (int) (totalVerified * 100 / totalRequired);
        return ClientKycCompletionResponse.builder()
                .caseCode(kycCase.getCode())
                .caseStatus(kycCase.getStatus().name())
                .completionPercent(percent)
                .totalRequired((int) totalRequired)
                .totalSubmitted((int) totalSubmitted)
                .totalVerified((int) totalVerified)
                .missingDocumentTypeCodes(caseResponse.getMissingDocumentTypeCodes())
                .requirements(requirements)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ClientKycDocumentResponse> listDocuments(Member member) {
        KycCase kycCase = resolveCase(member);
        return kycDocumentRepository.findAllByKycCaseOrderByIdAsc(kycCase).stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientKycDocumentResponse getDocument(Member member, Long documentId) {
        KycDocument kycDocument = resolveOwnedKycDocument(member, documentId);
        return toDocumentResponse(kycDocument);
    }

    @Transactional(readOnly = true)
    public DocumentFileResult downloadDocument(Member member, Long documentId) {
        KycDocument kycDocument = resolveOwnedKycDocument(member, documentId);
        return documentService.getFileWithMeta(kycDocument.getDocument().getCode());
    }

    @Transactional
    public ClientKycDocumentResponse uploadDocument(Member member,
                                                     String documentType,
                                                     String documentNumber,
                                                     LocalDate issueDate,
                                                     LocalDate expiryDate,
                                                     MultipartFile file) {
        validateFile(file);
        KycCase kycCase = resolveCase(member);
        assertCaseEditable(kycCase);

        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        metadata.setOwnerType(DocumentOwnerType.MEMBER);
        metadata.setOwnerCode(member.getMemberId());
        metadata.setDocumentTypeCode(documentType);
        metadata.setTitle(documentType.replace("_", " ") + " — " + member.getMemberId());
        metadata.setDocumentNumber(documentNumber);
        metadata.setIssueDate(issueDate);
        metadata.setExpiryDate(expiryDate);

        DocumentResponse docResponse = documentService.upload(metadata, file);
        Document document = documentRepository.findByCode(docResponse.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found after upload"));

        KycDocument kycDocument = KycDocument.builder()
                .kycCase(kycCase)
                .ownerType(DocumentOwnerType.MEMBER)
                .ownerId(member.getId())
                .document(document)
                .documentType(documentType)
                .documentNumber(documentNumber)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .status(KycDocumentVerificationStatus.PENDING)
                .build();
        kycDocumentRepository.save(kycDocument);
        kycAutomationService.syncMemberKyc(member.getMemberId());
        return toDocumentResponse(kycDocument);
    }

    @Transactional
    public ClientKycDocumentResponse resubmitDocument(Member member, Long documentId, MultipartFile file) {
        validateFile(file);
        KycDocument kycDocument = resolveOwnedKycDocument(member, documentId);
        if (kycDocument.getStatus() != KycDocumentVerificationStatus.REJECTED
                && kycDocument.getStatus() != KycDocumentVerificationStatus.PENDING) {
            throw new BadRequestException("Only PENDING or REJECTED documents can be resubmitted");
        }
        documentService.replace(kycDocument.getDocument().getCode(), file);
        kycDocument.setStatus(KycDocumentVerificationStatus.PENDING);
        kycDocumentRepository.save(kycDocument);
        kycAutomationService.syncMemberKyc(member.getMemberId());
        return toDocumentResponse(kycDocument);
    }

    @Transactional
    public void deleteDocument(Member member, Long documentId) {
        KycDocument kycDocument = resolveOwnedKycDocument(member, documentId);
        if (kycDocument.getStatus() != KycDocumentVerificationStatus.PENDING) {
            throw new BadRequestException("Only PENDING documents can be deleted");
        }
        kycDocumentRepository.delete(kycDocument);
        kycAutomationService.syncMemberKyc(member.getMemberId());
    }

    @Transactional
    public ClientKycStatusResponse submitCase(Member member) {
        KycCase kycCase = resolveCase(member);
        if (kycCase.getStatus() != KycCaseStatus.IN_PROGRESS
                && kycCase.getStatus() != KycCaseStatus.PENDING_CORRECTION) {
            throw new BadRequestException("Your KYC case is not in a submittable state. Current status: " + kycCase.getStatus());
        }
        KycCaseResponse response = kycService.submit(kycCase.getCode());
        kycCase = kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(
                DocumentOwnerType.MEMBER, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("KYC case not found"));
        return toStatusResponse(response, kycCase);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private KycCase resolveCase(Member member) {
        return kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(
                        DocumentOwnerType.MEMBER, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No KYC case found. Please contact support to initialize your KYC dossier."));
    }

    private KycDocument resolveOwnedKycDocument(Member member, Long documentId) {
        KycCase kycCase = resolveCase(member);
        KycDocument doc = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));
        if (!doc.getKycCase().getId().equals(kycCase.getId())) {
            throw new ResourceNotFoundException("KYC document not found");
        }
        return doc;
    }

    private void assertCaseEditable(KycCase kycCase) {
        if (kycCase.getStatus() == KycCaseStatus.APPROVED
                || kycCase.getStatus() == KycCaseStatus.UNDER_REVIEW
                || kycCase.getStatus() == KycCaseStatus.SUBMITTED) {
            throw new BadRequestException("KYC case is not open for document uploads. Current status: " + kycCase.getStatus());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size must not exceed 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("File type not allowed. Accepted: PDF, JPEG, PNG");
        }
    }

    private ClientKycDocumentResponse toDocumentResponse(KycDocument kycDocument) {
        Document document = kycDocument.getDocument();
        return ClientKycDocumentResponse.builder()
                .id(kycDocument.getId())
                .documentCode(document.getCode())
                .documentType(kycDocument.getDocumentType())
                .documentNumber(kycDocument.getDocumentNumber())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .status(kycDocument.getStatus() != null ? kycDocument.getStatus().name() : null)
                .issueDate(kycDocument.getIssueDate())
                .expiryDate(kycDocument.getExpiryDate())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private ClientKycStatusResponse toStatusResponse(KycCaseResponse caseResponse, KycCase kycCase) {
        List<ClientKycDocumentResponse> documents = kycDocumentRepository
                .findAllByKycCaseOrderByIdAsc(kycCase).stream()
                .map(this::toDocumentResponse)
                .toList();
        List<ClientKycRequirementResponse> requirements = caseResponse.getRequirements() != null
                ? caseResponse.getRequirements().stream()
                .map(r -> ClientKycRequirementResponse.builder()
                        .documentTypeCode(r.getDocumentTypeCode())
                        .documentTypeName(r.getDocumentTypeName())
                        .required(r.isRequired())
                        .status(r.getStatus() != null ? r.getStatus().name() : null)
                        .documentCode(r.getDocumentCode())
                        .build())
                .toList()
                : List.of();
        int completionPercent = computeCompletionPercent(requirements);
        return ClientKycStatusResponse.builder()
                .caseCode(kycCase.getCode())
                .status(kycCase.getStatus().name())
                .riskLevel(kycCase.getRiskLevel() != null ? kycCase.getRiskLevel().name() : null)
                .kycLevel(kycCase.getKycLevel() != null ? kycCase.getKycLevel() : 1)
                .completionPercent(completionPercent)
                .complete(caseResponse.isComplete())
                .approved(caseResponse.isApproved())
                .startedAt(kycCase.getStartedAt())
                .submittedAt(kycCase.getSubmittedAt())
                .completedAt(kycCase.getCompletedAt())
                .decisionComment(kycCase.getDecisionComment())
                .missingDocumentTypeCodes(caseResponse.getMissingDocumentTypeCodes())
                .requirements(requirements)
                .documents(documents)
                .build();
    }

    private int computeCompletionPercent(List<ClientKycRequirementResponse> requirements) {
        long totalRequired = requirements.stream().filter(ClientKycRequirementResponse::isRequired).count();
        if (totalRequired == 0) return 100;
        long verified = requirements.stream()
                .filter(r -> r.isRequired() && "VERIFIED".equals(r.getStatus())).count();
        return (int) (verified * 100 / totalRequired);
    }
}
