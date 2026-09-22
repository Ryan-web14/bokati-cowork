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
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentVersionResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentCorrectionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTagResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.*;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentReview;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentSignature;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentMetadataRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentReviewRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentSignatureRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTagAssignmentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentVersionRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sni.bokaticowork.core.utils.constant.SystemActors;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final DocumentTagAssignmentRepository tagAssignmentRepository;
    private final DocumentMetadataRepository metadataRepository;
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
    private final DocumentRejectionCascadeService rejectionCascadeService;
    private final DocumentSignatureRepository signatureRepository;
    private final DocumentMapper mapper;

    @Value("${app.document.default-expiry-years:5}")
    private Integer defaultExpiryYears;

    @Override
    public DocumentResponse upload(DocumentUploadMetadataRequest metadata, MultipartFile file) {
        Long uploadedBy = currentUserId();
        DocumentType documentType = documentType(metadata.getDocumentTypeCode());
        OwnerResolution owner = resolveOwner(metadata.getOwnerType(), metadata.getOwnerCode());

        applyDefaultExpiryDate(metadata, documentType);
        validateDocumentMetadata(metadata, documentType, file.getSize());
        enforceMultipleAllowed(documentType, owner);
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
                .status(initialDocumentStatus(documentType))
                .issueDate(metadata.getIssueDate())
                .expiryDate(metadata.getExpiryDate())
                .uploadedBy(uploadedBy)
                .space(deriveSpace(owner.ownerType(), documentType.getCategory()))
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
        Long uploadedBy = currentUserId();
        DocumentType documentType = documentType(metadata.getDocumentTypeCode());
        OwnerResolution owner = resolveOwner(metadata.getOwnerType(), metadata.getOwnerCode());

        applyDefaultExpiryDate(metadata, documentType);
        validateDocumentMetadata(metadata, documentType, content.length);
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
                .status(initialDocumentStatus(documentType))
                .issueDate(metadata.getIssueDate())
                .expiryDate(metadata.getExpiryDate())
                .uploadedBy(uploadedBy)
                .space(deriveSpace(owner.ownerType(), documentType.getCategory()))
                .build();

        documentRepository.save(document);
        createVersionFromBytes(document, uploadedBy, originalFileName, content, inspection);
        attachKycDocumentIfNeeded(document, owner, metadata);
        kycAutomationService.syncFromDocumentUpload(document);
        publishDocumentEvent("DOCUMENT_GENERATED", document, uploadedBy);
        return getByCode(document.getCode());
    }

    @Override
    public DocumentResponse replace(String documentCode, MultipartFile file) {
        Long uploadedBy = currentUserId();
        Document document = serviceDocument(documentCode);
        List<String> allowedMimeTypes = parseAllowedMimeTypes(document.getDocumentType().getAllowedMimeTypes());
        DocumentSecurityService.SecurityInspection inspection = securityService.inspect(file, allowedMimeTypes);

        document.setStatus(document.getDocumentType().getRequiresReview() ? DocumentStatus.PENDING_REVIEW : DocumentStatus.APPROVED);
        createVersion(document, uploadedBy, document.getTitle(), null, file, inspection);

        kycDocumentRepository.findAllByDocument(document).forEach(item -> {
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
            page = documentRepository.findAllByOwnerTypeAndOwnerIdAndStatusNot(owner.ownerType(), owner.ownerId(), DocumentStatus.REJECTED, pageable);
        } else {
            page = documentRepository.findAllByStatusNot(DocumentStatus.REJECTED, pageable);
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
        assertReviewable(document, DocumentStatus.APPROVED);
        boolean liftsRejection = document.getStatus() == DocumentStatus.REJECTED;
        document.setStatus(DocumentStatus.APPROVED);
        documentRepository.save(document);
        saveReview(document, request, DocumentReviewStatus.APPROVED);
        syncKycDocumentStatus(document, KycDocumentVerificationStatus.VERIFIED);
        kycAutomationService.syncFromDocumentReview(document);
        publishDocumentEvent("DOCUMENT_APPROVED", document, request.getReviewedBy());
        if (liftsRejection) {
            // Le refus avait pu annuler contrat, abonnement ou pass · rien ne les retablit tout
            // seul, et personne ne doit le croire. L'evenement nomme ce qui reste a reprendre.
            log.warn("Piece {} approuvee apres refus · les annulations en cascade du refus ne sont pas revenues",
                    document.getCode());
            publishDocumentEvent("DOCUMENT_REJECTION_LIFTED", document, request.getReviewedBy());
        }
        if (Boolean.TRUE.equals(document.getDocumentType().getRequiresSignature())) {
            createPendingSignature(document);
            publishDocumentEvent("DOCUMENT_SIGNATURE_REQUIRED", document, request.getReviewedBy());
        }
        return getByCode(documentCode);
    }

    @Override
    public DocumentResponse reject(String documentCode, DocumentReviewDecisionRequest request) {
        Document document = serviceDocument(documentCode);
        assertReviewable(document, DocumentStatus.REJECTED);
        document.setStatus(DocumentStatus.REJECTED);
        documentRepository.save(document);
        saveReview(document, request, DocumentReviewStatus.REJECTED);
        syncKycDocumentStatus(document, KycDocumentVerificationStatus.REJECTED);
        kycAutomationService.syncFromDocumentReview(document);
        rejectionCascadeService.cascade(document, request.getReviewedBy(), rejectionReason(request));
        publishDocumentEvent("DOCUMENT_REJECTED", document, request.getReviewedBy());
        return getByCode(documentCode);
    }

    @Override
    public DocumentResponse requestCorrection(String documentCode, DocumentCorrectionRequest request) {
        Document document = serviceDocument(documentCode);
        assertReviewable(document, DocumentStatus.NEEDS_CORRECTION);

        Instant deadline = Instant.now().plus(
                java.time.Duration.ofDays(request.getDeadlineDays() == null ? 7 : request.getDeadlineDays()));

        document.setStatus(DocumentStatus.NEEDS_CORRECTION);
        document.setCorrectionNote(request.getCorrectionNote().trim());
        document.setCorrectionDeadline(deadline);
        document.setCorrectionCount(document.getCorrectionCount() == null ? 1 : document.getCorrectionCount() + 1);
        documentRepository.save(document);

        DocumentReview review = DocumentReview.builder()
                .document(document)
                .versionNumber(document.getCurrentVersionNumber())
                .reviewStatus(DocumentReviewStatus.NEEDS_CORRECTION)
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(Instant.now())
                .comment(trimToNull(request.getComment()))
                .correctionNote(request.getCorrectionNote().trim())
                .correctionDeadline(deadline)
                .build();
        documentReviewRepository.save(review);

        syncKycDocumentStatus(document, KycDocumentVerificationStatus.PENDING);
        publishDocumentEvent("DOCUMENT_CORRECTION_REQUESTED", document, request.getReviewedBy());
        return getByCode(documentCode);
    }

    @Override
    public DocumentResponse restoreVersion(String documentCode, Integer versionNumber) {
        Document document = serviceDocument(documentCode);
        DocumentVersion target = documentVersionRepository.findByDocumentAndVersionNumber(document, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for document " + documentCode));

        documentVersionRepository.findByDocumentAndCurrentTrue(document).ifPresent(current -> {
            current.setCurrent(false);
            documentVersionRepository.save(current);
        });

        int nextVersion = document.getCurrentVersionNumber() == null ? 1 : document.getCurrentVersionNumber() + 1;

        DocumentVersion restored = DocumentVersion.builder()
                .document(document)
                .versionNumber(nextVersion)
                .storageProvider(target.getStorageProvider())
                .storagePath(target.getStoragePath())
                .originalFileName(target.getOriginalFileName())
                .storedFileName(target.getStoredFileName())
                .mimeTypeDeclared(target.getMimeTypeDeclared())
                .mimeTypeDetected(target.getMimeTypeDetected())
                .fileExtension(target.getFileExtension())
                .checksumSha256(target.getChecksumSha256())
                .fileSizeBytes(target.getFileSizeBytes())
                .uploadStatus(target.getUploadStatus())
                .antivirusStatus(target.getAntivirusStatus())
                .uploadedBy(currentUserId())
                .current(true)
                .build();
        documentVersionRepository.save(restored);

        document.setCurrentVersionNumber(nextVersion);
        document.setFileName(restored.getOriginalFileName());
        document.setFileUrl(restored.getStoragePath());
        document.setFileSize(restored.getFileSizeBytes());
        document.setMimeType(restored.getMimeTypeDetected());
        document.setChecksumSha256(restored.getChecksumSha256());
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("documentCode", document.getCode());
        payload.put("restoredFromVersion", versionNumber);
        payload.put("newVersion", nextVersion);
        payload.put("actorId", currentUserId());
        outboxService.publish("DOCUMENT_VERSION_RESTORED", "DOCUMENT", document.getCode(), payload);

        return getByCode(documentCode);
    }

    @Override
    public DocumentResponse archive(String documentCode, String reason) {
        Document document = serviceDocument(documentCode);
        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new BadRequestException("Document is already archived");
        }
        document.setStatus(DocumentStatus.ARCHIVED);
        documentRepository.save(document);
        publishDocumentEvent("DOCUMENT_ARCHIVED", document, currentUserId());
        return getByCode(documentCode);
    }

    /**
     * Une piece peut-elle recevoir cette decision de revue ?
     *
     * <p>Un refus n'est pas definitif : il vient d'une personne, et une personne se trompe · un
     * agent doit pouvoir approuver une piece qu'un collegue a refusee a tort, sans obliger le
     * client a la redeposer. Ce qui a ete annule en cascade par le refus n'est pas ressuscite
     * pour autant · l'approbation le dit et l'evenement le porte.</p>
     *
     * <p>Restent hors de portee : une piece approuvee, signee, expiree, archivee ou remplacee.
     * Revenir sur celles-la n'est pas une correction de revue, c'est une autre decision.</p>
     */
    private void assertReviewable(Document document, DocumentStatus decision) {
        DocumentStatus status = document.getStatus();
        if (status == DocumentStatus.PENDING_REVIEW
                || status == DocumentStatus.UPLOADED
                || status == DocumentStatus.NEEDS_CORRECTION) {
            return;
        }
        if (status == DocumentStatus.REJECTED && decision != DocumentStatus.REJECTED) {
            return;
        }
        String allowed = status == DocumentStatus.REJECTED
                ? "Cette pièce est déjà refusée · elle peut être approuvée ou renvoyée en correction, pas refusée une seconde fois."
                : "Une pièce " + status + " ne se revoit plus · le client doit en déposer une nouvelle.";
        throw new BadRequestException("Document " + document.getCode() + " · " + allowed);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadFile(String documentCode) {
        return getFileWithMeta(documentCode).content();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentFileResult getFileWithMeta(String documentCode) {
        Document document = serviceDocument(documentCode);
        if (!StringUtils.hasText(document.getFileUrl())) {
            throw new BadRequestException("Document has no file stored");
        }
        DocumentVersion currentVersion = documentVersionRepository.findByDocumentAndCurrentTrue(document).orElse(null);
        String storageProvider = currentVersion == null ? "FILESYSTEM" : currentVersion.getStorageProvider();
        String storagePath = currentVersion == null ? document.getFileUrl() : currentVersion.getStoragePath();
        byte[] content = storageService.read(storageProvider, storagePath);
        String mimeType = StringUtils.hasText(document.getMimeType()) ? document.getMimeType() : "application/octet-stream";
        String fileName = StringUtils.hasText(document.getFileName()) ? document.getFileName() : documentCode + ".bin";
        return new DocumentFileResult(content, mimeType, fileName);
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
        kycDocumentRepository.findAllByDocument(document).forEach(item -> {
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

    private void createPendingSignature(Document document) {
        List<DocumentSignature> existing = signatureRepository.findAllByDocumentAndSignatureStatus(
                document, DocumentSignatureStatus.PENDING);
        if (!existing.isEmpty()) {
            return;
        }
        DocumentSignature signature = DocumentSignature.builder()
                .document(document)
                .signerType(document.getOwnerType().name())
                .signerId(document.getOwnerId())
                .signatureStatus(DocumentSignatureStatus.PENDING)
                .build();
        signatureRepository.save(signature);
    }

    private void enforceMultipleAllowed(DocumentType documentType, OwnerResolution owner) {
        if (Boolean.TRUE.equals(documentType.getMultipleAllowed())) {
            return;
        }
        List<DocumentStatus> excluded = List.of(
                DocumentStatus.REJECTED, DocumentStatus.ARCHIVED,
                DocumentStatus.SUPERSEDED, DocumentStatus.EXPIRED);
        long existing = documentRepository.countByOwnerTypeAndOwnerIdAndDocumentTypeAndStatusNotIn(
                owner.ownerType(), owner.ownerId(), documentType, excluded);
        if (existing > 0) {
            throw new BadRequestException(
                    "Only one document of type '" + documentType.getCode() + "' is allowed per owner");
        }
    }

    private void validateDocumentMetadata(DocumentUploadMetadataRequest metadata, DocumentType documentType, long fileSizeBytes) {
        if (documentType.getRequiresExpiryDate() && metadata.getExpiryDate() == null) {
            throw new BadRequestException("Expiry date is required for this document type");
        }
        if (documentType.getMaxFileSizeBytes() != null && documentType.getMaxFileSizeBytes() > 0
                && fileSizeBytes > documentType.getMaxFileSizeBytes()) {
            throw new BadRequestException(
                    "File size (" + (fileSizeBytes / 1024) + " KB) exceeds the maximum allowed size of "
                            + (documentType.getMaxFileSizeBytes() / 1024) + " KB for this document type");
        }
    }

    private void applyDefaultExpiryDate(DocumentUploadMetadataRequest metadata, DocumentType documentType) {
        if (!Boolean.TRUE.equals(documentType.getRequiresExpiryDate()) || metadata.getExpiryDate() != null) {
            return;
        }
        LocalDate baseDate = metadata.getIssueDate() == null ? LocalDate.now() : metadata.getIssueDate();
        metadata.setExpiryDate(baseDate.plusYears(resolvedDefaultExpiryYears()));
    }

    private int resolvedDefaultExpiryYears() {
        return defaultExpiryYears == null || defaultExpiryYears <= 0 ? 5 : defaultExpiryYears;
    }

    private DocumentStatus initialDocumentStatus(DocumentType documentType) {
        if (Boolean.TRUE.equals(documentType.getAutoApprove())) {
            return DocumentStatus.APPROVED;
        }
        return Boolean.TRUE.equals(documentType.getRequiresReview()) ? DocumentStatus.PENDING_REVIEW : DocumentStatus.APPROVED;
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

        List<DocumentTagResponse> tags = tagAssignmentRepository.findAllByDocument(document).stream()
                .map(a -> {
                    var t = a.getTag();
                    return DocumentTagResponse.builder()
                            .id(t.getId()).code(t.getCode()).label(t.getLabel())
                            .color(t.getColor()).space(t.getSpace())
                            .createdBy(t.getCreatedBy()).createdAt(t.getCreatedAt())
                            .build();
                })
                .toList();

        Map<String, String> metadata = new LinkedHashMap<>();
        metadataRepository.findAllByDocument(document).forEach(m -> metadata.put(m.getMetaKey(), m.getMetaValue()));

        DocumentResponse response = mapper.toResponse(document);
        response.setVersions(versions);
        response.setTags(tags);
        response.setMetadata(metadata);
        response.setSpace(document.getSpace());
        response.setSpaceReferenceCode(document.getSpaceReferenceCode());
        return response;
    }

    private DocumentSpace deriveSpace(DocumentOwnerType ownerType, DocumentCategory category) {
        if (category == DocumentCategory.KYC) return DocumentSpace.KYC_SPACE;
        if (ownerType == DocumentOwnerType.CONTRACT) return DocumentSpace.CONTRACT_SPACE;
        if (ownerType == DocumentOwnerType.INVOICE || ownerType == DocumentOwnerType.PAYMENT) return DocumentSpace.FINANCIAL_SPACE;
        if (ownerType == DocumentOwnerType.ASSET) return DocumentSpace.ASSET_SPACE;
        if (category == DocumentCategory.LEGAL || category == DocumentCategory.SYSTEM) return DocumentSpace.ADMINISTRATIVE;
        return DocumentSpace.GENERIC;
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

    private String rejectionReason(DocumentReviewDecisionRequest request) {
        if (StringUtils.hasText(request.getRejectionReasonDetail())) {
            return request.getRejectionReasonDetail();
        }
        if (StringUtils.hasText(request.getRejectionReasonCode())) {
            return request.getRejectionReasonCode();
        }
        return request.getComment();
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

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return SystemActors.SYSTEM_USER_ID;
    }
}
