package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.features.billing.dto.request.RequestInvoiceChangeRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.InvoiceChangeResponse;
import com.sni.bokaticowork.features.billing.enums.BillingChangeRequestStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentChangeRequest;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentChangeRequestRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modification d'une facture non payee, par versions et apres approbation (lots B14 et B15).
 *
 * <p>Tant qu'aucun encaissement n'est impute, une facture se modifie. Un brouillon change aussitot ;
 * une facture emise attend l'accord du centre d'approbation de la facturation et garde entre-temps sa
 * version en vigueur. A l'accord : nouvelle version de la meme piece si elle n'est ni scellee ni
 * comptabilisee ; sinon, avoir total et facture rectificative, presentee comme sa nouvelle version
 * (decision D8). Rien n'est supprime.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceChangeService {

    public static final String CENTER = "BILLING";
    public static final String TYPE = "INVOICE_CHANGE";
    static final String NEW_VERSION = "NEW_VERSION";
    static final String CORRECTIVE_INVOICE = "CORRECTIVE_INVOICE";

    private static final Set<BillingDocumentStatus> CLOSED = EnumSet.of(BillingDocumentStatus.CANCELLED, BillingDocumentStatus.REFUNDED,
            BillingDocumentStatus.WRITTEN_OFF, BillingDocumentStatus.PAID, BillingDocumentStatus.PARTIALLY_PAID);
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final BillingDocumentRepository documents;
    private final BillingDocumentChangeRequestRepository changes;
    private final BillingDocumentServiceImpl billing;
    private final ApprovalEngine approvals;
    private final StaffProfileService staffProfiles;

    /** Demande de modification ; un brouillon est modifie aussitot, une facture emise attend l'approbation. */
    public InvoiceChangeResponse request(String invoiceNumber, RequestInvoiceChangeRequest request) {
        BillingDocument invoice = modifiable(invoiceNumber);
        StaffProfileService.Author author = staffProfiles.currentAuthor();
        BigDecimal after = billing.projectedTotal(invoice, request.changes());
        BillingDocumentChangeRequest change = BillingDocumentChangeRequest.builder()
                .documentId(invoice.getId()).documentNumber(invoice.getDocumentNumber())
                .status(BillingChangeRequestStatus.PENDING).reason(request.reason().trim()).changesJson(write(request.changes()))
                .discountOverride(StringUtils.hasText(request.discountOverrideReason()) ? request.discountOverrideReason().trim() : null)
                .requestedBy(author.login()).requestedByName(author.name()).requestedAt(Instant.now())
                .totalBefore(invoice.getTotalAmount()).totalAfter(after).build();
        if (invoice.getStatus() == BillingDocumentStatus.DRAFT) {
            // Un brouillon n'engage personne : il change aussitot, sa version precedente reste archivee.
            billing.update(invoice.getDocumentNumber(), request.changes());
            change.setStatus(BillingChangeRequestStatus.APPLIED);
            change.setResult(NEW_VERSION);
            change.setResultDocumentNumber(invoice.getDocumentNumber());
            change.setDecidedAt(Instant.now());
            change.setDecidedBy(author.login());
            return view(changes.save(change));
        }
        for (BillingDocumentChangeRequest pending : changes.findByDocumentNumberAndStatus(invoice.getDocumentNumber(),
                BillingChangeRequestStatus.PENDING)) {
            pending.setStatus(BillingChangeRequestStatus.CANCELLED);
            pending.setDecisionComment("Remplacée par une nouvelle demande");
            changes.save(pending);
        }
        change = changes.save(change);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("invoiceNumber", invoice.getDocumentNumber());
        details.put("totalBefore", invoice.getTotalAmount());
        details.put("totalAfter", after);
        details.put("reason", change.getReason());
        details.put("sealed", Boolean.TRUE.equals(invoice.getLocked()));
        ApprovalRequest approval = approvals.submit(new ApprovalDtos.Submission(CENTER, TYPE, "BILLING_DOCUMENT", invoice.getDocumentNumber(),
                "Modification de la facture " + invoice.getDocumentNumber() + " : " + change.getReason(), after, invoice.getCurrency(),
                null, write(details), null, null));
        change.setApprovalNumber(approval.getNumber());
        change = changes.save(change);
        if (approval.getStatus() == ApprovalStatus.APPROVED) {
            apply(change, author.login());
        }
        return view(change);
    }

    @Transactional(readOnly = true)
    public List<InvoiceChangeResponse> list(String invoiceNumber) {
        return changes.findByDocumentNumberOrderByRequestedAtDesc(invoiceNumber).stream().map(InvoiceChangeService::view).toList();
    }

    /** Retrait par le demandeur d'une demande encore en attente. */
    public InvoiceChangeResponse cancel(Long id) {
        BillingDocumentChangeRequest change = changes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de modification introuvable : " + id));
        if (change.getStatus() != BillingChangeRequestStatus.PENDING) {
            throw new BadRequestException("Cette demande n'est plus en attente");
        }
        String actor = staffProfiles.currentAuthor().login();
        if (!actor.equalsIgnoreCase(change.getRequestedBy())) {
            throw new ForbiddenException("Seul le demandeur retire sa demande");
        }
        approvals.cancelSubject(CENTER, TYPE, change.getDocumentNumber(), "Demande retirée par le demandeur");
        change.setStatus(BillingChangeRequestStatus.CANCELLED);
        change.setDecidedAt(Instant.now());
        change.setDecidedBy(actor);
        return view(changes.save(change));
    }

    // ------------------------------------------------------------------ decisions du centre

    void onApproved(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        if (!outcome.finalApproval()) {
            return;
        }
        BillingDocumentChangeRequest change = byApproval(request.getNumber());
        change.setDecisionComment(outcome.comment());
        apply(change, outcome.actor());
    }

    void onClosed(ApprovalRequest request, ApprovalDtos.Outcome outcome, BillingChangeRequestStatus status) {
        BillingDocumentChangeRequest change = byApproval(request.getNumber());
        change.setStatus(status);
        change.setDecidedAt(Instant.now());
        change.setDecidedBy(outcome.actor());
        change.setDecisionComment(outcome.comment());
        changes.save(change);
    }

    /** Application de la modification approuvee : nouvelle version, ou rectificative (D8). */
    void apply(BillingDocumentChangeRequest change, String decidedBy) {
        BillingDocument invoice = billing.serviceByNumber(change.getDocumentNumber());
        if (invoice.getPaidAmount() != null && invoice.getPaidAmount().signum() > 0) {
            throw new ConflictException("invoice_change", "La facture " + invoice.getDocumentNumber()
                    + " a reçu un paiement depuis la demande : la modification passe désormais par un avoir ou une rectificative");
        }
        UpdateBillingDocumentRequest proposed = read(change.getChangesJson());
        StaffProfileService.Author author = staffProfiles.authorOf(change.getRequestedBy());
        if (Boolean.TRUE.equals(invoice.getLocked()) || documents.salePosted(invoice.getDocumentNumber())) {
            BillingDocument corrective = billing.replaceWithCorrective(invoice, proposed, change.getReason(), change.getDiscountOverride(), author);
            change.setResult(CORRECTIVE_INVOICE);
            change.setResultDocumentNumber(corrective.getDocumentNumber());
        } else {
            BillingDocument revised = billing.applyInvoiceVersion(invoice, proposed, change.getReason(), change.getDiscountOverride(), author);
            change.setResult(NEW_VERSION);
            change.setResultDocumentNumber(revised.getDocumentNumber());
            change.setVersionNumber(billing.currentRevision(revised));
        }
        change.setStatus(BillingChangeRequestStatus.APPLIED);
        change.setDecidedAt(Instant.now());
        change.setDecidedBy(decidedBy);
        changes.save(change);
    }

    // ------------------------------------------------------------------ outils

    private BillingDocument modifiable(String invoiceNumber) {
        BillingDocument invoice = billing.serviceByNumber(invoiceNumber);
        if (invoice.getDocumentType() != BillingDocumentType.INVOICE) {
            throw new BadRequestException("Seule une facture se modifie par demande ; un devis se révise, une proforma se modifie directement");
        }
        if (StringUtils.hasText(invoice.getReplacedByDocumentNumber())) {
            throw new BadRequestException("Cette facture est remplacée par " + invoice.getReplacedByDocumentNumber() + " : modifiez celle-ci");
        }
        boolean paid = invoice.getPaidAmount() != null && invoice.getPaidAmount().signum() > 0;
        if (paid || CLOSED.contains(invoice.getStatus())) {
            throw new BadRequestException(paid
                    ? "Une facture qui a reçu un paiement, même partiel, se corrige par un avoir ou une rectificative"
                    : "Une facture au statut " + invoice.getStatus() + " ne se modifie plus");
        }
        return invoice;
    }

    private BillingDocumentChangeRequest byApproval(String approvalNumber) {
        return changes.findFirstByApprovalNumber(approvalNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de modification introuvable pour " + approvalNumber));
    }

    static InvoiceChangeResponse view(BillingDocumentChangeRequest change) {
        return new InvoiceChangeResponse(change.getId(), change.getDocumentNumber(), change.getStatus().name(), change.getReason(),
                change.getRequestedBy(), change.getRequestedByName(), change.getRequestedAt(), change.getTotalBefore(), change.getTotalAfter(),
                change.getApprovalNumber(), change.getDecidedAt(), change.getDecidedBy(), change.getDecisionComment(), change.getResult(),
                change.getResultDocumentNumber(), change.getVersionNumber());
    }

    static String write(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BadRequestException("Modifications illisibles : " + exception.getMessage());
        }
    }

    static UpdateBillingDocumentRequest read(String json) {
        try {
            return JSON.readValue(json, UpdateBillingDocumentRequest.class);
        } catch (Exception exception) {
            throw new BadRequestException("Modifications enregistrées illisibles : " + exception.getMessage());
        }
    }
}
