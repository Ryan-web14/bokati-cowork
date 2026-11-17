package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;

/**
 * Lot KG2 : rattache un document KYC deposee a sa piece, en un seul endroit, quelle que soit la voie
 * (portail, equipe, route documentaire).
 *
 * <ul>
 *   <li><b>Verso</b> : il devient le verso de la piece du meme type ; l'ancien verso passe
 *       {@code SUPERSEDED}. Sans recto, le depot est refuse. Un verso ne cree jamais de piece.</li>
 *   <li><b>Recto</b> : il remplace le fichier de la piece du meme type ; l'ancien recto et son verso
 *       passent {@code SUPERSEDED} (une nouvelle carte demande un nouveau verso). Sinon une piece
 *       est creee.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class KycPieceLinker {

    /** Ce que le depot dit de la piece. */
    public record Details(String side, String documentNumber, LocalDate issueDate, LocalDate expiryDate,
                          KycImageQualityService.Report quality) {
        public Details(String side, String documentNumber, LocalDate issueDate, LocalDate expiryDate) {
            this(side, documentNumber, issueDate, expiryDate, null);
        }

        boolean back() {
            return "BACK".equalsIgnoreCase(side);
        }
    }

    /** Qualite d'image mesuree au depot, sur la face deposee. */
    private static void applyQuality(KycDocument piece, Details details, boolean back) {
        KycImageQualityService.Report report = details.quality();
        String status = report == null ? null : report.status().name();
        String warnings = report == null || report.warnings().isEmpty() ? null : String.join("\n", report.warnings());
        java.math.BigDecimal blur = report == null || report.blurScore() == null ? null : java.math.BigDecimal.valueOf(report.blurScore());
        if (back) {
            piece.setBackQualityStatus(status);
            piece.setBackQualityWarnings(warnings);
            piece.setBackQualityBlurScore(blur);
        } else {
            piece.setQualityStatus(status);
            piece.setQualityWarnings(warnings);
            piece.setQualityBlurScore(blur);
        }
    }

    private final KycCaseRepository kycCaseRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final DocumentRepository documentRepository;
    private final OutboxService outboxService;

    /** Rattache le document et renvoie sa piece. */
    public KycDocument link(Document document, DocumentOwnerType ownerType, Long ownerId, Details details) {
        KycCase kycCase = kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(ownerType, ownerId).orElse(null);
        String typeCode = document.getDocumentType().getCode();
        KycDocument existing = kycCase == null ? null
                : kycDocumentRepository.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, typeCode).stream()
                .reduce((first, second) -> second).orElse(null);

        if (details.back()) {
            if (existing == null) {
                throw new BadRequestException("Le recto doit être déposé avant le verso pour la pièce : "
                        + document.getDocumentType().getName());
            }
            supersede(existing.getBackDocument(), document);
            existing.setBackDocument(document);
            existing.setStatus(KycDocumentVerificationStatus.PENDING);
            applyQuality(existing, details, true);
            return kycDocumentRepository.save(existing);
        }

        if (existing != null) {
            supersede(existing.getDocument(), document);
            supersede(existing.getBackDocument(), document);
            existing.setBackDocument(null);
            applyQuality(existing, new Details(null, null, null, null, null), true);
            applyQuality(existing, details, false);
            existing.setDocument(document);
            existing.setDocumentNumber(trim(details.documentNumber()));
            existing.setIssueDate(details.issueDate());
            existing.setExpiryDate(details.expiryDate());
            existing.setStatus(KycDocumentVerificationStatus.PENDING);
            return kycDocumentRepository.save(existing);
        }

        KycDocument created = KycDocument.builder()
                .kycCase(kycCase)
                .ownerType(ownerType)
                .ownerId(ownerId)
                .document(document)
                .documentType(typeCode)
                .documentNumber(trim(details.documentNumber()))
                .issueDate(details.issueDate())
                .expiryDate(details.expiryDate())
                .status(KycDocumentVerificationStatus.PENDING)
                .build();
        applyQuality(created, details, false);
        return kycDocumentRepository.save(created);
    }

    /** L'ancien fichier d'une piece remplacee passe SUPERSEDED ; il reste consultable. */
    void supersede(Document previous, Document replacement) {
        if (previous == null || previous == replacement
                || (previous.getId() != null && previous.getId().equals(replacement.getId()))
                || previous.getStatus() == DocumentStatus.ARCHIVED || previous.getStatus() == DocumentStatus.SUPERSEDED) {
            return;
        }
        previous.setStatus(DocumentStatus.SUPERSEDED);
        documentRepository.save(previous);
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("documentCode", previous.getCode());
        payload.put("ownerType", previous.getOwnerType());
        payload.put("ownerId", previous.getOwnerId());
        payload.put("documentTypeCode", previous.getDocumentType() == null ? null : previous.getDocumentType().getCode());
        payload.put("status", previous.getStatus());
        payload.put("replacedBy", replacement.getCode());
        payload.put("actorId", replacement.getUploadedBy());
        outboxService.publish("DOCUMENT_SUPERSEDED", "DOCUMENT", previous.getCode(), payload);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
