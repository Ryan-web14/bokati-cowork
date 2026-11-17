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
import java.util.List;

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
    private com.sni.bokaticowork.features.document.kyc.service.support.KycRequirementResolver requirementResolver;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setRequirementResolver(com.sni.bokaticowork.features.document.kyc.service.support.KycRequirementResolver requirementResolver) {
        this.requirementResolver = requirementResolver;
    }

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
        metadata.setKycSide("BACK".equalsIgnoreCase(side) ? "BACK" : "FRONT");

        uploadValidator.validateForAdmin(metadata, docType);

        DocumentResponse docResponse = documentService.upload(metadata, file);
        Document document = documentRepository.findByCode(docResponse.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable après upload"));

        Long ownerId = document.getOwnerId();
        KycCase kycCase = kycCaseRepository
                .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(ownerType, ownerId)
                .orElseThrow(() -> new BadRequestException(
                        "Aucun dossier KYC actif pour ce propriétaire · créez-en un d'abord"));
        // Lot KG1 : seules les pieces demandees pour ce dossier se deposent (la transaction annule le depot).
        if (requirementResolver != null && !requirementResolver.accepts(kycCase, documentTypeCode)) {
            throw new BadRequestException("Cette pièce n'est pas demandée pour ce dossier : " + documentTypeCode.toUpperCase()
                    + " (ajoutez-la d'abord aux exigences du dossier)");
        }

        // Lot KG2 : documentService.upload a rattache le fichier a sa piece (recto remplace, verso
        // ajoute, anciens fichiers SUPERSEDED) ; la synchronisation part apres validation.
        KycDocument piece = pieceOf(document)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce KYC introuvable après le dépôt"));
        if (piece.getKycCase() == null) {
            piece.setKycCase(kycCase);
            kycDocumentRepository.save(piece);
        }
        return toResponse(piece, docType);
    }

    /** La piece dont ce document est le recto ou le verso. */
    private java.util.Optional<KycDocument> pieceOf(Document document) {
        return kycDocumentRepository.findByDocument(document)
                .or(() -> kycDocumentRepository.findAllByBackDocument(document).stream().findFirst());
    }

    @Transactional
    public KycDocumentResponse updateDetails(Long kycDocumentId,
                                              String documentNumber,
                                              LocalDate issueDate,
                                              LocalDate expiryDate) {
        // L'API expose le numero court de la piece, pas l'identifiant genere.
        KycDocument kycDoc = kycDocumentRepository.findByShortId(kycDocumentId)
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
                .id(kycDoc.getShortId())
                .documentCode(doc.getCode())
                .documentType(kycDoc.getDocumentType())
                .documentNumber(kycDoc.getDocumentNumber())
                .qualityStatus(kycDoc.getQualityStatus())
                .qualityWarnings(kycDoc.getQualityWarnings() == null || kycDoc.getQualityWarnings().isBlank() ? List.of() : List.of(kycDoc.getQualityWarnings().split("\n")))
                .backQualityStatus(kycDoc.getBackQualityStatus())
                .backQualityWarnings(kycDoc.getBackQualityWarnings() == null || kycDoc.getBackQualityWarnings().isBlank() ? List.of() : List.of(kycDoc.getBackQualityWarnings().split("\n")))
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
