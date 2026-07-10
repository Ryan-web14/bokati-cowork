package com.sni.bokaticowork.features.document.kyc.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentUploadValidator;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycDocumentUploadService {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final KycCaseRepository kycCaseRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final KycAutomationService kycAutomationService;
    private final DocumentUploadValidator uploadValidator;

    @Transactional
    public KycDocumentResponse adminUpload(DocumentOwnerType ownerType,
                                            String ownerCode,
                                            String documentTypeCode,
                                            String side,
                                            String documentNumber,
                                            LocalDate issueDate,
                                            LocalDate expiryDate,
                                            MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est requis");
        }

        DocumentType docType = documentTypeRepository.findByCode(documentTypeCode.toUpperCase())
                .orElseThrow(() -> new BadRequestException("Type de document inconnu : " + documentTypeCode));

        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        metadata.setOwnerType(ownerType);
        metadata.setOwnerCode(ownerCode);
        metadata.setDocumentTypeCode(documentTypeCode);
        String sideLabel = "BACK".equalsIgnoreCase(side) ? " (Arrière)" : "";
        metadata.setTitle(docType.getName() + sideLabel + " · " + ownerCode);
        metadata.setDocumentNumber(documentNumber);
        metadata.setIssueDate(issueDate);
        metadata.setExpiryDate(expiryDate);

        uploadValidator.validateForAdmin(metadata, docType);

        DocumentResponse docResponse = documentService.upload(metadata, file);
        Document document = documentRepository.findByCode(docResponse.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable après upload"));

        Long ownerId = document.getOwnerId();
        KycCase kycCase = kycCaseRepository
                .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(ownerType, ownerId)
                .orElseThrow(() -> new BadRequestException(
                        "Aucun dossier KYC actif pour ce propriétaire · créez-en un d'abord"));

        if ("BACK".equalsIgnoreCase(side)) {
            KycDocument frontDoc = kycDocumentRepository
                    .findByKycCaseAndDocumentType(kycCase, documentTypeCode.toUpperCase())
                    .orElseThrow(() -> new BadRequestException(
                            "Le recto doit être uploadé avant le verso pour le type : " + documentTypeCode));

            kycDocumentRepository.findByDocument(document)
                    .ifPresent(kycDocumentRepository::delete);

            frontDoc.setBackDocument(document);
            kycDocumentRepository.save(frontDoc);
            kycAutomationService.syncFromDocumentUpload(document);
            return toResponse(frontDoc, docType);
        }

        // FRONT · check for existing KycDocument of this type in the case
        return kycDocumentRepository.findByKycCaseAndDocumentType(kycCase, documentTypeCode.toUpperCase())
                .map(existing -> {
                    existing.setDocument(document);
                    if (StringUtils.hasText(documentNumber)) existing.setDocumentNumber(documentNumber);
                    if (issueDate != null) existing.setIssueDate(issueDate);
                    if (expiryDate != null) existing.setExpiryDate(expiryDate);
                    existing.setStatus(KycDocumentVerificationStatus.PENDING);
                    kycDocumentRepository.save(existing);
                    // Remove the orphan auto-created duplicate if present
                    kycDocumentRepository.findByDocument(document)
                            .filter(d -> !d.getId().equals(existing.getId()))
                            .ifPresent(kycDocumentRepository::delete);
                    kycAutomationService.syncFromDocumentUpload(document);
                    return toResponse(existing, docType);
                })
                .orElseGet(() -> {
                    KycDocument autoCreated = kycDocumentRepository.findByDocument(document).orElseGet(() -> {
                        KycDocument nd = KycDocument.builder()
                                .kycCase(kycCase)
                                .ownerType(ownerType)
                                .ownerId(ownerId)
                                .document(document)
                                .documentType(documentTypeCode.toUpperCase())
                                .documentNumber(documentNumber)
                                .issueDate(issueDate)
                                .expiryDate(expiryDate)
                                .status(KycDocumentVerificationStatus.PENDING)
                                .build();
                        return kycDocumentRepository.save(nd);
                    });
                    kycAutomationService.syncFromDocumentUpload(document);
                    return toResponse(autoCreated, docType);
                });
    }

    @Transactional
    public KycDocumentResponse updateDetails(Long kycDocumentId,
                                              String documentNumber,
                                              LocalDate issueDate,
                                              LocalDate expiryDate) {
        KycDocument kycDoc = kycDocumentRepository.findById(kycDocumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document KYC introuvable"));

        if (StringUtils.hasText(documentNumber)) kycDoc.setDocumentNumber(documentNumber.trim());
        if (issueDate != null) kycDoc.setIssueDate(issueDate);
        if (expiryDate != null) kycDoc.setExpiryDate(expiryDate);
        kycDocumentRepository.save(kycDoc);

        DocumentType docType = documentTypeRepository.findByCode(kycDoc.getDocumentType()).orElse(null);
        return toResponse(kycDoc, docType);
    }

    private KycDocumentResponse toResponse(KycDocument kycDoc, DocumentType docType) {
        Document doc = kycDoc.getDocument();
        KycDocumentResponse.KycDocumentResponseBuilder builder = KycDocumentResponse.builder()
                .id(kycDoc.getId())
                .documentCode(doc.getCode())
                .documentType(kycDoc.getDocumentType())
                .documentNumber(kycDoc.getDocumentNumber())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .requiresBackSide(docType != null ? docType.getRequiresBackSide() : Boolean.FALSE)
                .issueDate(kycDoc.getIssueDate())
                .expiryDate(kycDoc.getExpiryDate())
                .status(kycDoc.getStatus())
                .uploadedBy(doc.getUploadedBy())
                .uploadedAt(doc.getUploadedAt());

        Document backDoc = kycDoc.getBackDocument();
        if (backDoc != null) {
            builder.backDocumentCode(backDoc.getCode())
                    .backFileName(backDoc.getFileName())
                    .backFileSize(backDoc.getFileSize())
                    .backMimeType(backDoc.getMimeType());
        }
        return builder.build();
    }
}
