package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentVersionResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.*;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentReview;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentReviewRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentVersionRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final Long SYSTEM_UPLOADER_ID = 0L;

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final KycCaseRepository kycCaseRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final CustomerService customerService;
    private final MemberService memberService;
    private final BusinessService businessService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final DocumentStorageService storageService;
    private final DocumentSecurityService securityService;
    private final OutboxService outboxService;
    private final KycAutomationService kycAutomationService;
    private final DocumentMapper mapper;

    @Override
    public DocumentResponse upload(DocumentUploadMetadataRequest metadata, MultipartFile file) {
        Long uploadedBy = resolveUploadedBy(metadata.getUploadedBy());
        DocumentType documentType = documentType(metadata.getDocumentTypeCode());
        OwnerResolution owner = resolveOwner(metadata.getOwnerType(), metadata.getOwnerCode());

        validateDocumentMetadata(metadata, documentType);
        List<String> allowedMimeTypes = parseAllowedMimeTypes(documentType.getAllowedMimeTypes());
        DocumentSecurityService.SecurityInspection inspection = securityService.inspect(file, allowedMimeTypes);

        Document document = Document.builder()
                .code(sequenceGenerator.next("DOCUMENT", LocalDate.now()))
                .ownerType(owner.ownerType())
                .ownerId(owner.ownerId())
                .category(documentType.getCategory())
                .documentType(documentType)
                .title(metadata.getTitle().trim())
                .description(trimToNull(metadata.getDescription()))
                .status(documentType.getRequiresReview() ? DocumentStatus.PENDING_REVIEW : DocumentStatus.APPROVED)
                .issueDate(metadata.getIssueDate())
                .expiryDate(metadata.getExpiryDate())
                .uploadedBy(uploadedBy)
                .build();

        documentRepository.save(document);
        createVersion(document, uploadedBy, metadata.getTitle(), metadata.getDocumentNumber(), file, inspection);
        attachKycDocumentIfNeeded(document, owner, metadata);
        kycAutomationService.syncFromDocumentUpload(document);
        publishDocumentEvent("DOCUMENT_UPLOADED", document, uploadedBy);
        return getByCode(document.getCode());
    }

    @Override
    public DocumentResponse createGeneratedDocument(DocumentUploadMetadataRequest metadata, String originalFileName, String declaredMimeType, byte[] content) {
        Long uploadedBy = resolveUploadedBy(metadata.getUploadedBy());
        DocumentType documentType = documentType(metadata.getDocumentTypeCode());
        OwnerResolution owner = resolveOwner(metadata.getOwnerType(), metadata.getOwnerCode());

        validateDocumentMetadata(metadata, documentType);
        List<String> allowedMimeTypes = parseAllowedMimeTypes(documentType.getAllowedMimeTypes());
        DocumentSecurityService.SecurityInspection inspection = securityService.inspectBytes(
                originalFileName,
                declaredMimeType,
                content,
                allowedMimeTypes
        );

        Document document = Document.builder()
                .code(sequenceGenerator.next("DOCUMENT", LocalDate.now()))
                .ownerType(owner.ownerType())
                .ownerId(owner.ownerId())
                .category(documentType.getCategory())
                .documentType(documentType)
                .title(metadata.getTitle().trim())
                .description(trimToNull(metadata.getDescription()))
                .status(documentType.getRequiresReview() ? DocumentStatus.PENDING_REVIEW : DocumentStatus.APPROVED)
                .issueDate(metadata.getIssueDate())
                .expiryDate(metadata.getExpiryDate())
                .uploadedBy(uploadedBy)
                .build();

        documentRepository.save(document);
        createVersionFromBytes(document, uploadedBy, originalFileName, content, inspection);
        attachKycDocumentIfNeeded(document, owner, metadata);
        kycAutomationService.syncFromDocumentUpload(document);
        publishDocumentEvent("DOCUMENT_GENERATED", document, uploadedBy);
        return getByCode(document.getCode());
    }

    @Override
    public DocumentResponse replace(String documentCode, Long uploadedBy, MultipartFile file) {
        Document document = serviceDocument(documentCode);
        List<String> allowedMimeTypes = parseAllowedMimeTypes(document.getDocumentType().getAllowedMimeTypes());
        DocumentSecurityService.SecurityInspection inspection = securityService.inspect(file, allowedMimeTypes);

        document.setStatus(document.getDocumentType().getRequiresReview() ? DocumentStatus.PENDING_REVIEW : DocumentStatus.APPROVED);
        createVersion(document, uploadedBy, document.getTitle(), null, file, inspection);

        kycDocumentRepository.findByDocument(document).ifPresent(item -> {
            item.setStatus(KycDocumentVerificationStatus.PENDING);
            kycDocumentRepository.save(item);
        });
        kycAutomationService.syncFromDocumentUpload(document);

        publishDocumentEvent("DOCUMENT_REPLACED", document, uploadedBy);

        return getByCode(documentCode);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getByCode(String documentCode) {
        Document document = serviceDocument(documentCode);
        return toResponse(document, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentResponse> list(DocumentOwnerType ownerType, String ownerCode, Pageable pageable) {
        Page<Document> page;
        if (ownerType != null && StringUtils.hasText(ownerCode)) {
            OwnerResolution owner = resolveOwner(ownerType, ownerCode);
            page = documentRepository.findAllByOwnerTypeAndOwnerId(owner.ownerType(), owner.ownerId(), pageable);
        } else {
            page = documentRepository.findAll(pageable);
        }
        return new PaginatedResponse<>(page.map(document -> toResponse(document, false)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> listVersions(String documentCode) {
        return documentVersionRepository.findAllByDocumentOrderByVersionNumberDesc(serviceDocument(documentCode)).stream()
                .map(mapper::toVersionResponse)
                .toList();
    }

    @Override
    public DocumentResponse approve(String documentCode, DocumentReviewDecisionRequest request) {
        Document document = serviceDocument(documentCode);
        document.setStatus(DocumentStatus.APPROVED);
        documentRepository.save(document);
        saveReview(document, request, DocumentReviewStatus.APPROVED);
        syncKycDocumentStatus(document, KycDocumentVerificationStatus.VERIFIED);
        kycAutomationService.syncFromDocumentReview(document);
        publishDocumentEvent("DOCUMENT_APPROVED", document, request.getReviewedBy());
        return getByCode(documentCode);
    }

    @Override
    public DocumentResponse reject(String documentCode, DocumentReviewDecisionRequest request) {
        Document document = serviceDocument(documentCode);
        document.setStatus(DocumentStatus.REJECTED);
        documentRepository.save(document);
        saveReview(document, request, DocumentReviewStatus.REJECTED);
        syncKycDocumentStatus(document, KycDocumentVerificationStatus.REJECTED);
        kycAutomationService.syncFromDocumentReview(document);
        publishDocumentEvent("DOCUMENT_REJECTED", document, request.getReviewedBy());
        return getByCode(documentCode);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadFile(String documentCode) {
        Document document = serviceDocument(documentCode);
        if (!StringUtils.hasText(document.getFileUrl())) {
            throw new BadRequestException("Document has no file stored");
        }
        try {
            return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(document.getFileUrl()));
        } catch (java.io.IOException ex) {
            throw new BadRequestException("Unable to read document file", ex);
        }
    }

    private void createVersion(
            Document document,
            Long uploadedBy,
            String title,
            String documentNumber,
            MultipartFile file,
            DocumentSecurityService.SecurityInspection inspection
    ) {
        documentVersionRepository.findByDocumentAndCurrentTrue(document).ifPresent(existing -> {
            existing.setCurrent(false);
            existing.setUploadStatus(DocumentVersionUploadStatus.READY);
            documentVersionRepository.save(existing);
        });

        int nextVersion = document.getCurrentVersionNumber() == null ? 1 : document.getCurrentVersionNumber() + 1;
        String ownerFolder = document.getOwnerType().name().toLowerCase(Locale.ROOT) + "/" + document.getOwnerId();
        DocumentStorageService.StoredDocument stored = storageService.store(ownerFolder, document.getCode(), nextVersion, file);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(nextVersion)
                .storageProvider(stored.provider())
                .storagePath(stored.storagePath())
                .originalFileName(file.getOriginalFilename() == null ? title : file.getOriginalFilename())
                .storedFileName(stored.storedFileName())
                .mimeTypeDeclared(inspection.declaredMimeType())
                .mimeTypeDetected(inspection.detectedMimeType())
                .fileExtension(extractExtension(file.getOriginalFilename()))
                .checksumSha256(inspection.checksumSha256())
                .fileSizeBytes(inspection.fileSizeBytes())
                .uploadStatus(DocumentVersionUploadStatus.READY)
                .antivirusStatus(inspection.antivirusStatus())
                .uploadedBy(uploadedBy)
                .current(true)
                .build();
        documentVersionRepository.save(version);

        document.setCurrentVersionNumber(nextVersion);
        document.setFileName(version.getOriginalFileName());
        document.setFileUrl(version.getStoragePath());
        document.setFileSize(version.getFileSizeBytes());
        document.setMimeType(version.getMimeTypeDetected());
        document.setChecksumSha256(version.getChecksumSha256());
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
    }

    private void createVersionFromBytes(
            Document document,
            Long uploadedBy,
            String originalFileName,
            byte[] content,
            DocumentSecurityService.SecurityInspection inspection
    ) {
        documentVersionRepository.findByDocumentAndCurrentTrue(document).ifPresent(existing -> {
            existing.setCurrent(false);
            existing.setUploadStatus(DocumentVersionUploadStatus.READY);
            documentVersionRepository.save(existing);
        });

        int nextVersion = document.getCurrentVersionNumber() == null ? 1 : document.getCurrentVersionNumber() + 1;
        String ownerFolder = document.getOwnerType().name().toLowerCase(Locale.ROOT) + "/" + document.getOwnerId();
        DocumentStorageService.StoredDocument stored = storageService.storeBytes(ownerFolder, document.getCode(), nextVersion, originalFileName, content);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(nextVersion)
                .storageProvider(stored.provider())
                .storagePath(stored.storagePath())
                .originalFileName(originalFileName)
                .storedFileName(stored.storedFileName())
                .mimeTypeDeclared(inspection.declaredMimeType())
                .mimeTypeDetected(inspection.detectedMimeType())
                .fileExtension(securityService.normalizeExtension(originalFileName))
                .checksumSha256(inspection.checksumSha256())
                .fileSizeBytes(inspection.fileSizeBytes())
                .uploadStatus(DocumentVersionUploadStatus.READY)
                .antivirusStatus(inspection.antivirusStatus())
                .uploadedBy(uploadedBy)
                .current(true)
                .build();
        documentVersionRepository.save(version);

        document.setCurrentVersionNumber(nextVersion);
        document.setFileName(version.getOriginalFileName());
        document.setFileUrl(version.getStoragePath());
        document.setFileSize(version.getFileSizeBytes());
        document.setMimeType(version.getMimeTypeDetected());
        document.setChecksumSha256(version.getChecksumSha256());
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
    }

    private void attachKycDocumentIfNeeded(Document document, OwnerResolution owner, DocumentUploadMetadataRequest metadata) {
        if (document.getCategory() != DocumentCategory.KYC) {
            return;
        }
        KycCase kycCase = kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(owner.ownerType(), owner.ownerId())
                .orElse(null);

        KycDocument kycDocument = KycDocument.builder()
                .kycCase(kycCase)
                .ownerType(owner.ownerType())
                .ownerId(owner.ownerId())
                .document(document)
                .documentType(document.getDocumentType().getCode())
                .documentNumber(trimToNull(metadata.getDocumentNumber()))
                .issueDate(metadata.getIssueDate())
                .expiryDate(metadata.getExpiryDate())
                .status(KycDocumentVerificationStatus.PENDING)
                .build();
        kycDocumentRepository.save(kycDocument);
    }

    private void syncKycDocumentStatus(Document document, KycDocumentVerificationStatus status) {
        kycDocumentRepository.findByDocument(document).ifPresent(item -> {
            item.setStatus(status);
            kycDocumentRepository.save(item);
        });
    }

    private void saveReview(Document document, DocumentReviewDecisionRequest request, DocumentReviewStatus status) {
        DocumentReview review = DocumentReview.builder()
                .document(document)
                .versionNumber(document.getCurrentVersionNumber())
                .reviewStatus(status)
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(Instant.now())
                .comment(trimToNull(request.getComment()))
                .rejectionReasonCode(trimToNull(request.getRejectionReasonCode()))
                .rejectionReasonDetail(trimToNull(request.getRejectionReasonDetail()))
                .build();
        documentReviewRepository.save(review);
    }

    private void validateDocumentMetadata(DocumentUploadMetadataRequest metadata, DocumentType documentType) {
        if (documentType.getRequiresExpiryDate() && metadata.getExpiryDate() == null) {
            throw new BadRequestException("Expiry date is required for this document type");
        }
        if (documentType.getMaxFileSizeBytes() != null && documentType.getMaxFileSizeBytes() > 0) {
            // The multipart size is rechecked during inspection using actual bytes.
        }
    }

    private DocumentType documentType(String code) {
        return documentTypeRepository.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));
    }

    private Document serviceDocument(String code) {
        return documentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private DocumentResponse toResponse(Document document, boolean includeVersions) {
        List<DocumentVersionResponse> versions = includeVersions
                ? documentVersionRepository.findAllByDocumentOrderByVersionNumberDesc(document).stream()
                .map(mapper::toVersionResponse)
                .toList()
                : List.of();

        DocumentResponse response = mapper.toResponse(document);
        response.setVersions(versions);
        return response;
    }

    private OwnerResolution resolveOwner(DocumentOwnerType ownerType, String ownerCode) {
        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            throw new BadRequestException("Document owner is required");
        }

        return switch (ownerType) {
            case MEMBER -> {
                Member member = memberService.getByMemberIdForService(ownerCode.trim());
                yield new OwnerResolution(DocumentOwnerType.MEMBER, member.getId());
            }
            case CUSTOMER -> {
                log.debug(ownerCode);
                Customer customer = customerService.getCustomerForService(ownerCode.trim());
                yield new OwnerResolution(DocumentOwnerType.CUSTOMER, customer.getId());
            }
            case BUSINESS -> {
                BusinessEntity business = businessService.serviceBusinessByCode(ownerCode.trim());
                yield new OwnerResolution(DocumentOwnerType.BUSINESS, business.getId());
            }
            default -> {
                try {
                    yield new OwnerResolution(ownerType, Long.parseLong(ownerCode.trim()));
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Owner code must be numeric for owner type " + ownerType);
                }
            }
        };
    }

    private List<String> parseAllowedMimeTypes(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private record OwnerResolution(DocumentOwnerType ownerType, Long ownerId) {
    }

    private void publishDocumentEvent(String eventType, Document document, Long actorId) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("documentCode", document.getCode());
        payload.put("ownerType", document.getOwnerType());
        payload.put("ownerId", document.getOwnerId());
        payload.put("documentTypeCode", document.getDocumentType().getCode());
        payload.put("status", document.getStatus());
        payload.put("version", document.getCurrentVersionNumber());
        payload.put("actorId", actorId);
        outboxService.publish(eventType, "DOCUMENT", document.getCode(), payload);
    }

    private Long resolveUploadedBy(String uploadedBy) {
        if (!StringUtils.hasText(uploadedBy) || uploadedBy.trim().equalsIgnoreCase("undefined")) {
            return SYSTEM_UPLOADER_ID;
        }

        try {
            return Long.valueOf(uploadedBy.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("uploadedBy must be a valid numeric user id", ex);
        }
    }
}
