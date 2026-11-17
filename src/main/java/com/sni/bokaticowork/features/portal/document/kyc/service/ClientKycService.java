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
    private com.sni.bokaticowork.features.document.kyc.service.support.KycRequirementResolver requirementResolver;

    /** Lot KG1 : pieces demandees pour ce dossier (globales et propres au dossier). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setRequirementResolver(com.sni.bokaticowork.features.document.kyc.service.support.KycRequirementResolver requirementResolver) {
        this.requirementResolver = requirementResolver;
    }

    @Transactional(readOnly = true)
    public ClientKycStatusResponse getMyCase(Member member) {
        KycCase kycCase = resolveCase(member);
        KycCaseResponse caseResponse = kycService.getByCode(kycCase.getCode());
        return toStatusResponse(caseResponse, kycCase);
    }

    @Transactional(readOnly = true)
    public List<ClientKycRequirementResponse> getRequirements(Member member) {
        KycCase kycCase = resolveCase(member);
        // Dossier valide ou en cours d'examen · plus rien a fournir tant qu'il n'est pas rouvert.
        if (closedForUploads(kycCase)) {
            return List.of();
        }
        // Lot KG1 : les pieces de ce dossier (exigences globales et propres au dossier), chacune avec
        // l'etat et les codes de la piece deposee (avant : exigences globales seules, sans etat).
        List<DocumentRequirement> reqs = requirementResolver != null
                ? requirementResolver.resolve(kycCase)
                : requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(DocumentOwnerType.MEMBER);
        // Once the case is approved the dossier is complete: nothing is required or missing anymore.
        boolean approved = kycCase.getStatus() == KycCaseStatus.APPROVED;
        List<KycDocument> pieces = kycDocumentRepository.findAllByKycCaseOrderByIdAsc(kycCase);
        return reqs.stream().map(r -> {
            KycDocument match = pieces.stream()
                    .filter(piece -> r.getDocumentTypeCode().equalsIgnoreCase(piece.getDocumentType()))
                    .reduce((first, second) -> second)
                    .orElse(null);
            DocumentType docType = documentTypeRepository.findByCode(r.getDocumentTypeCode()).orElse(null);
            return ClientKycRequirementResponse.builder()
                    .documentTypeCode(r.getDocumentTypeCode())
                    .documentTypeName(r.getDocumentTypeName())
                    .required(!approved && Boolean.TRUE.equals(r.getRequired()))
                    .requiresBackSide(docType != null ? docType.getRequiresBackSide() : Boolean.FALSE)
                    .status(match != null && match.getStatus() != null ? match.getStatus().name() : null)
                    .documentCode(match != null && match.getDocument() != null ? match.getDocument().getCode() : null)
                    .backDocumentCode(match != null && match.getBackDocument() != null ? match.getBackDocument().getCode() : null)
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
                .missingDocumentTypeCodes(approved ? List.of() : caseResponse.getMissingDocumentTypeCodes())
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
        // Lot KG1 : seules les pieces demandees pour ce dossier se deposent.
        if (requirementResolver != null && !requirementResolver.accepts(kycCase, documentType)) {
            throw new BadRequestException("Cette pièce n'est pas demandée pour votre dossier : " + docType.getName());
        }

        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        metadata.setOwnerType(DocumentOwnerType.MEMBER);
        metadata.setOwnerCode(member.getMemberId());
        metadata.setDocumentTypeCode(documentType);
        String sideLabel = "BACK".equalsIgnoreCase(side) ? " (Arrière)" : "";
        metadata.setTitle(documentType.replace("_", " ") + sideLabel + " · " + member.getMemberId());
        metadata.setDocumentNumber(documentNumber);
        metadata.setIssueDate(issueDate);
        metadata.setExpiryDate(expiryDate);
        metadata.setKycSide("BACK".equalsIgnoreCase(side) ? "BACK" : "FRONT");

        DocumentResponse docResponse = documentService.upload(metadata, file);
        Document document = documentRepository.findByCode(docResponse.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable après le dépôt"));

        // Lot KG2 : documentService.upload a rattache le fichier a sa piece (recto remplace, verso
        // ajoute a son recto, anciens fichiers SUPERSEDED, jamais de piece pour un verso).
        KycDocument kycDocument = kycDocumentRepository.findByDocument(document)
                .or(() -> kycDocumentRepository.findAllByBackDocument(document).stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Pièce KYC introuvable après le dépôt"));
        if (kycDocument.getKycCase() == null) {
            kycDocument.setKycCase(kycCase);
            kycDocumentRepository.save(kycDocument);
        }
        kycAutomationService.recompute(kycCase);
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
            throw new BadRequestException("Seule une pièce en attente ou refusée se redépose");
        }
        documentService.replace(kycDocument.getDocument().getCode(), file);
        kycDocument.setStatus(KycDocumentVerificationStatus.PENDING);
        kycDocumentRepository.save(kycDocument);
        kycAutomationService.recompute(kycDocument.getKycCase());
        return toDocumentResponse(kycDocument);
    }

    @Transactional
    public void deleteDocument(Member member, Long documentId) {
        KycDocument kycDocument = resolveOwnedKycDocument(member, documentId);
        if (kycDocument.getStatus() != KycDocumentVerificationStatus.PENDING) {
            throw new BadRequestException("Seule une pièce en attente de vérification peut être retirée");
        }
        // Lot KG2 : les fichiers sont archives (ils restent consultables par l'equipe) et la piece
        // retiree ; un nouveau depot du meme type est ensuite possible.
        KycCase kycCase = kycDocument.getKycCase();
        archiveQuietly(kycDocument.getBackDocument());
        archiveQuietly(kycDocument.getDocument());
        kycDocumentRepository.delete(kycDocument);
        kycAutomationService.recompute(kycCase);
    }

    @Transactional
    public ClientKycStatusResponse submitCase(Member member) {
        KycCase kycCase = resolveCase(member);
        if (kycCase.getStatus() != KycCaseStatus.IN_PROGRESS
                && kycCase.getStatus() != KycCaseStatus.PENDING_CORRECTION) {
            throw new BadRequestException("Votre dossier KYC ne peut pas être soumis dans son état actuel : " + kycCase.getStatus());
        }
        KycCaseResponse response = kycService.submit(kycCase.getCode());
        kycCase = kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(
                DocumentOwnerType.MEMBER, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dossier KYC introuvable"));
        return toStatusResponse(response, kycCase);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void archiveQuietly(Document document) {
        if (document == null || document.getStatus() == com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus.ARCHIVED) {
            return;
        }
        documentService.archive(document.getCode(), "Pièce retirée par le titulaire depuis son espace");
    }

    private KycCase resolveCase(Member member) {
        return kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(
                        DocumentOwnerType.MEMBER, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun dossier KYC : contactez le support pour l'ouvrir."));
    }

    private KycDocument resolveOwnedKycDocument(Member member, Long documentId) {
        KycCase kycCase = resolveCase(member);
        // L'API expose le numero court de la piece, pas l'identifiant genere.
        KycDocument doc = kycDocumentRepository.findByShortId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce KYC introuvable"));
        if (!doc.getKycCase().getId().equals(kycCase.getId())) {
            throw new ResourceNotFoundException("Pièce KYC introuvable");
        }
        return doc;
    }

    /** La piece deja deposee pour ce type, hors la ligne que l'envoi en cours vient de creer. */
    private void assertCaseEditable(KycCase kycCase) {
        if (closedForUploads(kycCase)) {
            throw new BadRequestException("Le dossier KYC n'accepte pas de dépôt dans son état actuel : " + kycCase.getStatus());
        }
    }

    /**
     * Le dossier n'attend plus rien du membre · valide, ou depose et en cours d'examen.
     *
     * <p>Les pieces exigees y etaient encore listees : le portail continuait de les reclamer a un
     * membre dont le dossier etait complet. Elles reviennent des que le dossier est rouvert
     * (correction demandee, renouvellement).</p>
     */
    static boolean closedForUploads(KycCase kycCase) {
        return kycCase.getStatus() == KycCaseStatus.APPROVED
                || kycCase.getStatus() == KycCaseStatus.UNDER_REVIEW
                || kycCase.getStatus() == KycCaseStatus.SUBMITTED;
    }

    private static List<String> lines(String text) {
        return text == null || text.isBlank() ? List.of() : List.of(text.split("\n"));
    }

    private ClientKycDocumentResponse toDocumentResponse(KycDocument kycDocument) {
        Document document = kycDocument.getDocument();
        DocumentType docType = documentTypeRepository.findByCode(kycDocument.getDocumentType()).orElse(null);
        ClientKycDocumentResponse.ClientKycDocumentResponseBuilder builder = ClientKycDocumentResponse.builder()
                .id(kycDocument.getShortId())
                .documentCode(document.getCode())
                .documentType(kycDocument.getDocumentType())
                .documentNumber(kycDocument.getDocumentNumber())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .status(kycDocument.getStatus() != null ? kycDocument.getStatus().name() : null)
                .qualityStatus(kycDocument.getQualityStatus())
                .qualityWarnings(lines(kycDocument.getQualityWarnings()))
                .backQualityStatus(kycDocument.getBackQualityStatus())
                .backQualityWarnings(lines(kycDocument.getBackQualityWarnings()))
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
        // Once the case is approved the dossier is complete: nothing is required or missing anymore.
        boolean approved = kycCase.getStatus() == KycCaseStatus.APPROVED;
        List<ClientKycRequirementResponse> requirements = !closedForUploads(kycCase) && caseResponse.getRequirements() != null
                ? caseResponse.getRequirements().stream()
                .map(r -> ClientKycRequirementResponse.builder()
                        .documentTypeCode(r.getDocumentTypeCode())
                        .documentTypeName(r.getDocumentTypeName())
                        .required(!approved && r.isRequired())
                        .requiresBackSide(r.getRequiresBackSide())
                        .status(r.getStatus() != null ? r.getStatus().name() : null)
                        .documentCode(r.getDocumentCode())
                        .backDocumentCode(r.getBackDocumentCode())
                        .build())
                .toList()
                : List.of();
        int completionPercent = approved ? 100 : computeCompletionPercent(requirements);
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
                .missingDocumentTypeCodes(approved ? List.of() : caseResponse.getMissingDocumentTypeCodes())
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
