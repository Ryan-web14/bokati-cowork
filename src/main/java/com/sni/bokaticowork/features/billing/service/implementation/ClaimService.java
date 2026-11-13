package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.ClaimDtos;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dunning.service.DunningHold;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingClaim;
import com.sni.bokaticowork.features.billing.model.BillingClaimReason;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingClaimReasonRepository;
import com.sni.bokaticowork.features.billing.repository.BillingClaimRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lot C11 : reclamations sur facture. Ouverte par l'equipe ou depuis l'espace client, une
 * reclamation suspend les relances de la facture ; elle se regle par un avoir (total ou partiel,
 * applique sur le reste du quand il y en a) ou par un refus motive. Classer la facture en creance
 * litigieuse reste une decision comptable distincte (lot F19).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ClaimService implements DunningHold {

    static final String OPEN = "OPEN";
    static final String STAFF = "STAFF";
    static final String PORTAL = "PORTAL";
    private static final Set<BillingDocumentType> CLAIMABLE = EnumSet.of(BillingDocumentType.INVOICE, BillingDocumentType.CORRECTIVE_INVOICE,
            BillingDocumentType.DEBIT_NOTE);

    private final BillingClaimRepository claims;
    private final BillingClaimReasonRepository reasons;
    private final BillingDocumentRepository documents;
    private final SequenceGeneratorFacade sequences;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<BillingDocumentServiceImpl> billingServices;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> holdReason(String documentNumber) {
        return documentNumber != null && claims.existsByDocumentNumberAndStatus(documentNumber, OPEN)
                ? Optional.of("Réclamation en cours : relance suspendue jusqu'à sa réponse") : Optional.empty();
    }

    /** Nouvelle reclamation ; une seule ouverte par facture. {@code channel} : STAFF ou PORTAL. */
    public ClaimDtos.ClaimView open(String documentNumber, ClaimDtos.OpenRequest request, String channel, String actor) {
        BillingDocument invoice = documents.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + documentNumber));
        if (!CLAIMABLE.contains(invoice.getDocumentType()) || invoice.getStatus() == BillingDocumentStatus.DRAFT
                || invoice.getStatus() == BillingDocumentStatus.CANCELLED) {
            throw BadRequestException.coded("RECLAMATION_IMPOSSIBLE", "Une réclamation porte sur une facture émise.");
        }
        if (claims.existsByDocumentNumberAndStatus(invoice.getDocumentNumber(), OPEN)) {
            throw ConflictException.coded("RECLAMATION_EN_COURS", "Une réclamation est déjà en cours sur " + invoice.getDocumentNumber() + ".");
        }
        BillingClaimReason reason = reasons.findByCode(request.reasonCode().trim().toUpperCase(Locale.ROOT))
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> BadRequestException.coded("REFERENCE_INCONNUE", "Motif de réclamation inconnu : " + request.reasonCode()));
        if (request.requestedAmount() != null && invoice.getTotalAmount() != null && request.requestedAmount().compareTo(invoice.getTotalAmount()) > 0) {
            throw BadRequestException.coded("RECLAMATION_MONTANT", "Le montant demandé dépasse le total de la facture.");
        }
        BillingClaim claim = claims.save(BillingClaim.builder().claimNumber(sequences.next("billing_claim"))
                .documentNumber(invoice.getDocumentNumber()).customerType(invoice.getCustomerType()).customerCode(invoice.getCustomerCode())
                .reasonCode(reason.getCode()).description(StringUtils.hasText(request.description()) ? request.description().trim() : null)
                .lineOrders(request.lineOrders() == null || request.lineOrders().isEmpty() ? null
                        : request.lineOrders().stream().map(String::valueOf).collect(Collectors.joining(",")))
                .requestedAmount(request.requestedAmount()).status(OPEN).channel(channel).openedBy(actor).build());
        return view(claim);
    }

    public ClaimDtos.ClaimView openByStaff(String documentNumber, ClaimDtos.OpenRequest request) {
        return open(documentNumber, request, STAFF, staffProfiles.currentAuthor().login());
    }

    /**
     * Acceptation : un avoir du montant accorde (par defaut le montant demande) ; applique sur le
     * reste du de la facture quand il le couvre, sinon a rembourser ou a imputer par les circuits
     * habituels des avoirs.
     */
    public ClaimDtos.ClaimView accept(String claimNumber, ClaimDtos.AcceptRequest request) {
        BillingClaim claim = openClaim(claimNumber);
        BigDecimal granted = request != null && request.grantedAmount() != null ? request.grantedAmount() : claim.getRequestedAmount();
        if (granted == null || granted.signum() <= 0) {
            throw BadRequestException.coded("RECLAMATION_MONTANT", "Indiquez le montant accordé.");
        }
        BillingDocument invoice = documents.findByDocumentNumber(claim.getDocumentNumber()).orElseThrow();
        if (invoice.getTotalAmount() != null && granted.compareTo(invoice.getTotalAmount()) > 0) {
            throw BadRequestException.coded("RECLAMATION_MONTANT", "Le montant accordé dépasse le total de la facture.");
        }
        boolean apply = invoice.getBalanceDue() != null && granted.compareTo(invoice.getBalanceDue()) <= 0;
        String creditNote = billingServices.getObject().createCreditNote(claim.getDocumentNumber(), new CreateCreditNoteRequest(granted,
                "Réclamation " + claim.getClaimNumber() + (request != null && StringUtils.hasText(request.comment()) ? " : " + request.comment().trim() : ""),
                apply, true, null)).documentNumber();
        boolean partial = claim.getRequestedAmount() != null && granted.compareTo(claim.getRequestedAmount()) < 0;
        return close(claim, partial ? "PARTIALLY_ACCEPTED" : "ACCEPTED", granted, creditNote, request == null ? null : request.comment());
    }

    /** Refus motive : la facture reste due, les relances reprennent. */
    public ClaimDtos.ClaimView reject(String claimNumber, String comment) {
        return close(openClaim(claimNumber), "REJECTED", null, null, comment);
    }

    /** Retrait par le client ou l'equipe : les relances reprennent. */
    public ClaimDtos.ClaimView withdraw(String claimNumber) {
        return close(openClaim(claimNumber), "WITHDRAWN", null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ClaimDtos.ClaimView> list(String documentNumber, String customerType, String customerCode, boolean openOnly) {
        List<BillingClaim> found = StringUtils.hasText(documentNumber) ? claims.findByDocumentNumberOrderByCreatedAtDesc(documentNumber.trim())
                : StringUtils.hasText(customerType) && StringUtils.hasText(customerCode)
                ? claims.findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(customerType, customerCode)
                : claims.findByStatusOrderByCreatedAtAsc(OPEN);
        return found.stream().filter(claim -> !openOnly || OPEN.equals(claim.getStatus())).map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public ClaimDtos.ClaimView get(String claimNumber) {
        return view(claim(claimNumber));
    }

    // ------------------------------------------------------------------ motifs

    @Transactional(readOnly = true)
    public List<ClaimDtos.ReasonView> reasons(boolean includeInactive) {
        return reasons.findAllOrdered().stream().filter(reason -> includeInactive || Boolean.TRUE.equals(reason.getActive()))
                .map(reason -> new ClaimDtos.ReasonView(reason.getCode(), reason.getLabel(), reason.getDisplayOrder(), Boolean.TRUE.equals(reason.getActive())))
                .toList();
    }

    public ClaimDtos.ReasonView saveReason(ClaimDtos.ReasonRequest request, Boolean active) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        BillingClaimReason reason = reasons.findByCode(code).orElseGet(() -> BillingClaimReason.builder().code(code).build());
        reason.setLabel(request.label().trim());
        if (request.displayOrder() != null) {
            reason.setDisplayOrder(request.displayOrder());
        }
        if (active != null) {
            reason.setActive(active);
        }
        BillingClaimReason saved = reasons.save(reason);
        return new ClaimDtos.ReasonView(saved.getCode(), saved.getLabel(), saved.getDisplayOrder(), Boolean.TRUE.equals(saved.getActive()));
    }

    // ------------------------------------------------------------------ outils

    private ClaimDtos.ClaimView close(BillingClaim claim, String status, BigDecimal granted, String creditNote, String comment) {
        if ("REJECTED".equals(status) && !StringUtils.hasText(comment)) {
            throw BadRequestException.coded("MOTIF_REQUIS", "Un refus est motivé : le motif est communiqué au client.");
        }
        claim.setStatus(status);
        claim.setGrantedAmount(granted);
        claim.setCreditNoteNumber(creditNote);
        claim.setDecisionComment(StringUtils.hasText(comment) ? comment.trim() : null);
        claim.setDecidedBy(staffProfiles.currentAuthor().login());
        claim.setDecidedAt(Instant.now());
        return view(claims.save(claim));
    }

    private BillingClaim openClaim(String claimNumber) {
        BillingClaim claim = claim(claimNumber);
        if (!OPEN.equals(claim.getStatus())) {
            throw ConflictException.coded("RECLAMATION_CLOSE", "La réclamation " + claimNumber + " est déjà réglée.");
        }
        return claim;
    }

    private BillingClaim claim(String claimNumber) {
        return claims.findByClaimNumber(claimNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Réclamation introuvable : " + claimNumber));
    }

    ClaimDtos.ClaimView view(BillingClaim claim) {
        String label = reasons.findByCode(claim.getReasonCode()).map(BillingClaimReason::getLabel).orElse(claim.getReasonCode());
        List<Integer> orders = claim.getLineOrders() == null ? List.of()
                : Arrays.stream(claim.getLineOrders().split(",")).map(String::trim).filter(StringUtils::hasText).map(Integer::valueOf).toList();
        return new ClaimDtos.ClaimView(claim.getClaimNumber(), claim.getDocumentNumber(), claim.getCustomerType(), claim.getCustomerCode(),
                claim.getReasonCode(), label, claim.getDescription(), orders, claim.getRequestedAmount(), claim.getStatus(), claim.getChannel(),
                claim.getOpenedBy(), claim.getGrantedAmount(), claim.getCreditNoteNumber(), claim.getDecisionComment(), claim.getDecidedBy(),
                claim.getDecidedAt(), claim.getCreatedAt());
    }
}
