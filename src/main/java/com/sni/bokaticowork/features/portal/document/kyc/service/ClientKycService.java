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
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
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
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentUploadValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientKycService {

    private final KycService kycService;
    private final KycCaseRepository kycCaseRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final KycAutomationService kycAutomationService;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final DocumentRequirementRepository requirementRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentUploadValidator uploadValidator;

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
            DocumentType docType = documentTypeRepository.findByCode(r.getDocumentTypeCode()).orElse(null);
            return ClientKycRequirementResponse.builder()
                    .documentTypeCode(r.getDocumentTypeCode())
                    .documentTypeName(r.getDocumentTypeName())
                    .required(Boolean.TRUE.equals(r.getRequired()))
                    .requiresBackSide(docType != null ? docType.getRequiresBackSide() : Boolean.FALSE)
                    .status(match != null && match.getStatus() != null ? match.getStatus().name() : null)
                    .documentCode(match != null ? match.getDocumentCode() : null)
                    .backDocumentCode(match != null ? match.getBackDocumentCode() : null)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public ClientKycCompletionResponse getCompletion(Member member) {
        KycCase kycCase = resolveCase(member);
        KycCaseResponse caseResponse = kycService.getByCode(kycCase.getCode());
        List<ClientKycRequirementResponse> requirements = getRequirements(member);
        long totalRequired = requirements.stream().filter(ClientKycRequirementResponse::isRequired).count();
        boolean approved = kycCase.getStatus() == KycCaseStatus.APPROVED;
        long totalSubmitted = approved ? totalRequired : requirements.stream()
                .filter(r -> r.getStatus() != null && !r.getStatus().isEmpty()).count();
        long totalVerified = approved ? totalRequired : requirements.stream()
                .filter(r -> "VERIFIED".equals(r.getStatus())).count();
        int percent = approved ? 100 : (totalRequired == 0 ? 0 : (int) (totalVerified * 100 / totalRequired));
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
                                                     MultipartFile file,
                                                     String side) {
        DocumentType docType = documentTypeRepository.findByCode(documentType)
                .orElseThrow(() -> new BadRequestException("Type de document inconnu : " + documentType));

        DocumentUploadMetadataRequest tempMeta = new DocumentUploadMetadataRequest();
        tempMeta.setDocumentNumber(documentNumber);
        tempMeta.setIssueDate(issueDate);
        tempMeta.setExpiryDate(expiryDate);
        uploadValidator.validateForClient(tempMeta, docType, file);

        KycCase kycCase = resolveCase(member);
        assertCaseEditable(kycCase);

        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        metadata.setOwnerType(DocumentOwnerType.MEMBER);
        metadata.setOwnerCode(member.getMemberId());
        metadata.setDocumentTypeCode(documentType);
        String sideLabel = "BACK".equalsIgnoreCase(side) ? " (Arrière)" : "";
        metadata.setTitle(documentType.replace("_", " ") + sideLabel + " — " + member.getMemberId());
        metadata.setDocumentNumber(documentNumber);
        metadata.setIssueDate(issueDate);
        metadata.setExpiryDate(expiryDate);

        DocumentResponse docResponse = documentService.upload(metadata, file);
        Document document = documentRepository.findByCode(docResponse.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found after upload"));

        if ("BACK".equalsIgnoreCase(side)) {
            KycDocument existing = kycDocumentRepository.findByKycCaseAndDocumentType(kycCase, documentType)
                    .orElseThrow(() -> new BadRequestException(
                            "Front side must be uploaded before the back side for document type: " + documentType));
            existing.setBackDocument(document);
            kycDocumentRepository.save(existing);
            kycAutomationService.syncMemberKyc(member.getMemberId());
            return toDocumentResponse(existing);
        }

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
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est requis");
        }
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

    private ClientKycDocumentResponse toDocumentResponse(KycDocument kycDocument) {
        Document document = kycDocument.getDocument();
        DocumentType docType = documentTypeRepository.findByCode(kycDocument.getDocumentType()).orElse(null);
        ClientKycDocumentResponse.ClientKycDocumentResponseBuilder builder = ClientKycDocumentResponse.builder()
                .id(kycDocument.getId())
                .documentCode(document.getCode())
                .documentType(kycDocument.getDocumentType())
                .documentNumber(kycDocument.getDocumentNumber())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .status(kycDocument.getStatus() != null ? kycDocument.getStatus().name() : null)
                .requiresBackSide(docType != null ? docType.getRequiresBackSide() : Boolean.FALSE)
                .issueDate(kycDocument.getIssueDate())
                .expiryDate(kycDocument.getExpiryDate())
                .uploadedAt(document.getUploadedAt());

        Document backDoc = kycDocument.getBackDocument();
        if (backDoc != null) {
            builder.backDocumentCode(backDoc.getCode())
                    .backFileName(backDoc.getFileName())
                    .backFileSize(backDoc.getFileSize())
                    .backMimeType(backDoc.getMimeType());
        }

        return builder.build();
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
                        .requiresBackSide(r.getRequiresBackSide())
                        .status(r.getStatus() != null ? r.getStatus().name() : null)
                        .documentCode(r.getDocumentCode())
                        .backDocumentCode(r.getBackDocumentCode())
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
