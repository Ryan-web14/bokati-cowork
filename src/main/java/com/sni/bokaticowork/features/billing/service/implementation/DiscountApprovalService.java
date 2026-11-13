package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingDiscountGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lot C3 : derogation de remise approuvee. A l'emission (et a la revision d'un devis), une ligne
 * sous le plancher, sous la marge minimale ou au-dessus de la remise maximale ne passe plus par
 * le seul droit de derogation : une demande part au N+1 par le centre de la facturation (circuit
 * {@code BILLING_DISCOUNT_OVERRIDE}), et l'emission attend son accord.
 *
 * <ul>
 *   <li>Le titulaire du droit {@code BILLING:DISCOUNT_OVERRIDE} passe outre directement, avec un motif.</li>
 *   <li>L'accord vaut pour l'offre approuvee : une empreinte des lignes le lie ; une ligne modifiee
 *       le fait tomber et une nouvelle demande part (l'ancienne est annulee).</li>
 *   <li>Refus : la piece reste en brouillon avec le motif du refus ; un nouveau motif relance une demande.</li>
 *   <li>Parametre {@value #MODE_KEY} : {@code APPROVAL} (defaut) ou {@code PERMISSION} (ancien comportement).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountApprovalService {

    public static final String CENTER = "BILLING";
    public static final String TYPE = "DISCOUNT_OVERRIDE";
    static final String MODE_KEY = "billing.discount.override-mode";
    static final String PENDING = "PENDING";
    static final String APPROVED = "APPROVED";
    static final String REJECTED = "REJECTED";

    private final BillingDiscountGuard guard;
    private final ObjectProvider<ApprovalEngine> approvals;
    private final BillingDocumentRepository documents;
    private final ObjectMapper objectMapper;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Demande en attente : la piece reste en brouillon ; la transaction est gardee (demande enregistree). */
    public static class PendingException extends ConflictException {
        public PendingException(String message) {
            super(message);
            withReason("DEROGATION_EN_ATTENTE");
        }
    }

    /**
     * Applique les limites de remise a une piece.
     *
     * @return le motif de derogation retenu (direct ou approuve), ou null s'il n'y a pas de depassement bloquant
     * @throws PendingException si une demande d'approbation est partie ou attend encore
     */
    public String enforce(BillingDocument document, List<BillingDocumentLine> lines,
                          List<BillingDiscountGuard.DiscountViolation> violations, String reason) {
        List<BillingDiscountGuard.DiscountViolation> blocking = violations.stream()
                .filter(violation -> "BLOCK".equals(violation.severity()))
                .toList();
        if (blocking.isEmpty()) {
            return null;
        }
        ApprovalEngine engine = approvals.getIfAvailable();
        if (!"APPROVAL".equals(mode()) || engine == null
                || (guard.hasOverridePermission() && StringUtils.hasText(reason))) {
            return guard.enforce(violations, reason);
        }
        String summary = blocking.stream().map(BillingDiscountGuard.DiscountViolation::message).reduce((a, b) -> a + " · " + b).orElse("");
        String fingerprint = fingerprint(document, lines);
        boolean sameOffer = fingerprint.equals(document.getDiscountApprovalFingerprint());
        if (sameOffer && APPROVED.equals(document.getDiscountApprovalStatus())) {
            return document.getDiscountApprovalReason();
        }
        if (sameOffer && PENDING.equals(document.getDiscountApprovalStatus())) {
            throw new PendingException("Remise hors limites en attente d'approbation (demande " + document.getDiscountApprovalNumber()
                    + ") : l'émission sera possible après l'accord de votre responsable.");
        }
        if (!StringUtils.hasText(reason)) {
            throw BadRequestException.coded("DEROGATION_MOTIF_REQUIS", "Remise hors limites : " + summary
                    + ". Indiquez un motif : la demande partira à votre responsable pour approbation.");
        }
        if (sameOffer && REJECTED.equals(document.getDiscountApprovalStatus())
                && reason.trim().equals(document.getDiscountApprovalReason())) {
            throw BadRequestException.coded("DEROGATION_REFUSEE", "La dérogation a été refusée"
                    + (StringUtils.hasText(document.getDiscountApprovalComment()) ? " : " + document.getDiscountApprovalComment() : "")
                    + ". Modifiez l'offre, ou donnez un nouveau motif pour une nouvelle demande.");
        }
        // Une demande precedente (autre offre, ou renvoyee) est annulee : une seule demande ouverte par piece.
        engine.cancelSubject(CENTER, TYPE, document.getDocumentNumber(), "Offre modifiée, nouvelle demande");
        ApprovalRequest request = engine.submit(new ApprovalDtos.Submission(CENTER, TYPE, "BILLING_DOCUMENT", document.getDocumentNumber(),
                "Dérogation de remise sur " + document.getDocumentNumber() + " : " + reason.trim(), document.getTotalAmount(),
                document.getCurrency(), null, details(document, blocking, reason), null, null));
        document.setDiscountApprovalNumber(request.getNumber());
        document.setDiscountApprovalFingerprint(fingerprint);
        document.setDiscountApprovalReason(reason.trim());
        document.setDiscountApprovalComment(null);
        if (request.getStatus() == ApprovalStatus.APPROVED) {
            // Demandeur sans responsable au-dessus de lui : accord immediat par le circuit.
            document.setDiscountApprovalStatus(APPROVED);
            documents.save(document);
            return reason.trim();
        }
        document.setDiscountApprovalStatus(PENDING);
        documents.save(document);
        throw new PendingException("Remise hors limites : " + summary + ". Demande d'approbation " + request.getNumber()
                + " envoyée à votre responsable ; l'émission sera possible après son accord.");
    }

    /** Decision prise sur la demande (routes communes /approvals). */
    public void onDecision(ApprovalRequest request, ApprovalDtos.Outcome outcome, String status) {
        documents.findByDocumentNumber(request.getSubjectRef()).ifPresent(document -> {
            if (!request.getNumber().equals(document.getDiscountApprovalNumber())) {
                return;
            }
            if (APPROVED.equals(status) && !outcome.finalApproval()) {
                return;
            }
            document.setDiscountApprovalStatus(status);
            document.setDiscountApprovalComment(outcome.comment());
            if (APPROVED.equals(status)) {
                document.setDiscountApprovedBy(outcome.actor());
                document.setDiscountOverrideBy(outcome.actor());
                document.setDiscountOverrideAt(Instant.now());
            }
            documents.save(document);
        });
    }

    String mode() {
        String value = platformSettings == null ? "APPROVAL" : platformSettings.text(MODE_KEY, "APPROVAL");
        return value == null ? "APPROVAL" : value.trim().toUpperCase(Locale.ROOT);
    }

    /** Empreinte des lignes et des remises : ce que le responsable a vu et approuve. */
    static String fingerprint(BillingDocument document, List<BillingDocumentLine> lines) {
        StringBuilder text = new StringBuilder();
        for (BillingDocumentLine line : lines) {
            text.append(line.getItemCode()).append('|').append(plain(line.getQuantity())).append('|').append(plain(line.getUnitPrice()))
                    .append('|').append(plain(line.getDiscountRate())).append('|').append(plain(line.getDiscountAmount()))
                    .append('|').append(line.getOptional()).append(';');
        }
        text.append("doc|").append(plain(document.getDiscountAmount())).append('|').append(plain(document.getTotalAmount()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String details(BillingDocument document, List<BillingDiscountGuard.DiscountViolation> blocking, String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("documentNumber", document.getDocumentNumber());
        details.put("documentType", document.getDocumentType() == null ? null : document.getDocumentType().name());
        details.put("customer", document.getCustomerName());
        details.put("reason", reason.trim());
        details.put("violations", blocking.stream().map(violation -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", violation.type());
            item.put("itemCode", violation.itemCode());
            item.put("message", violation.message());
            return item;
        }).toList());
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
