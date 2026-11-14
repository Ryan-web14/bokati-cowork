package com.sni.bokaticowork.features.billing.element.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.element.dto.ElementDtos;
import com.sni.bokaticowork.features.billing.element.model.ElementRef;
import com.sni.bokaticowork.features.billing.element.model.PaymentAllocationReversal;
import com.sni.bokaticowork.features.billing.element.repository.PaymentAllocationReversalRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassLifecycleOperator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Annulation d'une affectation de paiement (lot R4), toujours possible.
 *
 * <ul>
 *   <li>la somme revient en attente ; si l'affectation etait comptabilisee, D 411 / C 4719 ;</li>
 *   <li>la facture est rouverte (de nouveau due) ou neutralisee (annulee, par avoir si elle est
 *   certifiee), au choix de l'administrateur ;</li>
 *   <li>un element qui n'est plus regle est remis en attente : droits retires, contrat annule,
 *   reservation remise en attente de paiement avec ses creneaux liberes ; l'historique reste. Si
 *   l'element a deja ete consomme, l'annulation est refusee avec le motif, et rien n'est change.</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional
public class AllocationReversalService {

    public static final String REOPEN = "REOPEN";
    public static final String NEUTRALIZE = "NEUTRALIZE";
    private static final List<String> CONSUMED_BOOKINGS = List.of("IN_PROGRESS", "COMPLETED", "NO_SHOW");

    private final PaymentAllocationRepository allocations;
    private final PaymentAllocationReversalRepository reversals;
    private final BillingElementResolver resolver;
    private final ElementSettlementService settlements;
    private final JdbcTemplate jdbc;
    private final ObjectProvider<BillingDocumentService> documents;
    private final ObjectProvider<SubscriptionService> subscriptions;
    private final ObjectProvider<SubscriptionLifecycleOperator> subscriptionOperator;
    private final ObjectProvider<PassService> passes;
    private final ObjectProvider<PassLifecycleOperator> passOperator;
    private final ObjectProvider<BookingService> bookings;
    private final StaffProfileService staffProfiles;

    public AllocationReversalService(PaymentAllocationRepository allocations, PaymentAllocationReversalRepository reversals,
                                     BillingElementResolver resolver, ElementSettlementService settlements, JdbcTemplate jdbc,
                                     ObjectProvider<BillingDocumentService> documents, ObjectProvider<SubscriptionService> subscriptions,
                                     ObjectProvider<SubscriptionLifecycleOperator> subscriptionOperator, ObjectProvider<PassService> passes,
                                     ObjectProvider<PassLifecycleOperator> passOperator, ObjectProvider<BookingService> bookings,
                                     StaffProfileService staffProfiles) {
        this.allocations = allocations;
        this.reversals = reversals;
        this.resolver = resolver;
        this.settlements = settlements;
        this.jdbc = jdbc;
        this.documents = documents;
        this.subscriptions = subscriptions;
        this.subscriptionOperator = subscriptionOperator;
        this.passes = passes;
        this.passOperator = passOperator;
        this.bookings = bookings;
        this.staffProfiles = staffProfiles;
    }

    /** Demande d'annulation : sort de la facture ({@code REOPEN} par defaut ou {@code NEUTRALIZE}) et motif. */
    public record CancelRequest(String invoiceOutcome, String reason) {
    }

    public record Result(Long allocationId, String transactionNumber, String documentNumber, BigDecimal amount, String invoiceOutcome,
                         List<String> elementsReverted, boolean accountingRequired) {
    }

    public Result cancel(Long allocationId, CancelRequest request) {
        String outcome = request == null || !StringUtils.hasText(request.invoiceOutcome()) ? REOPEN
                : request.invoiceOutcome().trim().toUpperCase(Locale.ROOT);
        if (!List.of(REOPEN, NEUTRALIZE).contains(outcome)) {
            throw BadRequestException.coded("SORT_FACTURE_INCONNU", "Sort de la facture inconnu « " + outcome
                    + " » : REOPEN (rouverte, toujours due) ou NEUTRALIZE (annulée, par avoir si elle est certifiée)");
        }
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw BadRequestException.coded("MOTIF_REQUIS", "Indiquez le motif de l'annulation");
        }
        PaymentAllocation allocation = allocations.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Affectation introuvable : " + allocationId));
        String documentNumber = allocation.getBillingDocumentNumber();
        String transactionNumber = allocation.getPaymentTransaction().getTransactionNumber();
        BigDecimal amount = allocation.getAllocatedAmount();
        String mode = allocation.getAllocationMode() == null ? "PAYMENT" : allocation.getAllocationMode();
        String reason = request.reason().trim();
        // Comptabilisee : l'ecriture du paiement (ou de l'affectation tardive) existe deja.
        boolean accounted = Boolean.TRUE.equals(jdbc.queryForObject("LATE".equals(mode)
                        ? "SELECT EXISTS (SELECT 1 FROM acc_posting_event WHERE event_type = 'LATE_ALLOCATION' AND reference = ?)"
                        : "SELECT EXISTS (SELECT 1 FROM acc_posting_event WHERE event_type = 'PAYMENT_RECEIVED' AND reference = ?)",
                Boolean.class, "LATE".equals(mode) ? "ALLOC-" + allocationId : transactionNumber));

        List<ElementDtos.Settled> settled = resolver.elements(documentNumber);
        BillingDocumentService billing = documents.getObject();
        billing.reversePayment(documentNumber, amount);
        if (NEUTRALIZE.equals(outcome)) {
            billing.cancelAndArchive(documentNumber, "Affectation de " + transactionNumber + " annulée : " + reason);
        }

        List<String> reverted = new ArrayList<>();
        for (ElementDtos.Settled element : settled) {
            ElementRef ref = new ElementRef(element.type(), element.code());
            if (stillSettled(ref, element.activationMode(), documentNumber)) {
                continue;
            }
            String done = revert(ref, "Affectation de " + transactionNumber + " sur " + documentNumber + " annulée : " + reason);
            if (done != null) {
                reverted.add(ref.type() + " " + ref.code() + " : " + done);
            }
        }

        reversals.save(PaymentAllocationReversal.builder().allocationId(allocationId).transactionNumber(transactionNumber)
                .documentNumber(documentNumber).amount(amount).allocationMode(mode).allocatedAt(allocation.getAllocatedAt())
                .allocatedBy(allocation.getAllocatedBy()).reversedBy(actor()).reason(reason).invoiceOutcome(outcome)
                .elementsJson(reverted.isEmpty() ? null : String.join("\n", reverted)).accountingRequired(accounted).build());
        allocations.delete(allocation);
        log.info("Affectation {} ({} sur {}, {}) annulee, facture {}, elements remis en attente : {}", allocationId, transactionNumber,
                documentNumber, amount, outcome, reverted);
        return new Result(allocationId, transactionNumber, documentNumber, amount, outcome, reverted, accounted);
    }

    /** L'element reste regle sans cette affectation : premier paiement encore present, ou toutes ses factures soldees. */
    private boolean stillSettled(ElementRef ref, String mode, String documentNumber) {
        if (ElementRef.FIRST_PAYMENT.equals(mode)) {
            return resolver.document(documentNumber).map(row -> {
                BigDecimal total = (BigDecimal) row.get("total_amount");
                BigDecimal balance = (BigDecimal) row.get("balance_due");
                return !"CANCELLED".equals(row.get("status")) && total != null && balance != null && balance.compareTo(total) < 0;
            }).orElse(false);
        }
        return settlements.fullySettled(ref);
    }

    /** Remet l'element en attente ; rend ce qui a ete fait, ou rien si l'element n'avait pas ete active. */
    private String revert(ElementRef ref, String reason) {
        return switch (ref.type()) {
            case ElementRef.SUBSCRIPTION -> revertSubscription(ref.code(), reason);
            case ElementRef.DOMICILIATION -> resolver.domiciliationSubscription(ref.code()).map(number -> revertSubscription(number, reason)).orElse(null);
            case ElementRef.PASS -> revertPass(ref.code(), reason);
            default -> revertBooking(ref.code(), reason);
        };
    }

    private String revertSubscription(String number, String reason) {
        Subscription subscription = subscriptions.getObject().getForService(number);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.GRACE_PERIOD) {
            return null;
        }
        if (subscription.getCurrentPeriodStart() != null && subscription.getStartDate() != null
                && subscription.getCurrentPeriodStart().isAfter(subscription.getStartDate())) {
            throw refused("ABONNEMENT_RENOUVELE", "L'abonnement " + number + " a déjà été renouvelé : son retour arrière se traite à la main");
        }
        Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM entitlement_grant g WHERE g.subscription_id = ? "
                + "AND g.quantity_remaining IS NOT NULL AND g.quantity_granted IS NOT NULL AND g.quantity_remaining < g.quantity_granted",
                Integer.class, subscription.getId());
        Integer bookingsUsed = jdbc.queryForObject("SELECT COUNT(*) FROM booking WHERE subscription_number = ? AND (status IN ('IN_PROGRESS', 'COMPLETED', 'NO_SHOW') "
                + "OR checked_in_at IS NOT NULL)", Integer.class, number);
        if ((used != null && used > 0) || (bookingsUsed != null && bookingsUsed > 0)) {
            throw refused("ELEMENT_CONSOMME", "L'abonnement " + number + " a déjà été utilisé (droits consommés ou réservations faites) : retour arrière refusé");
        }
        subscriptionOperator.getObject().revertActivation(subscription, reason);
        return "remis en attente d'activation, droits retirés, contrat annulé";
    }

    private String revertPass(String number, String reason) {
        Pass pass = passes.getObject().getForService(number);
        if (pass.getStatus() != PassStatus.ACTIVE && pass.getStatus() != PassStatus.PARTIALLY_USED) {
            return null;
        }
        Integer bookingsUsed = jdbc.queryForObject("SELECT COUNT(*) FROM booking WHERE pass_number = ? AND (status IN ('IN_PROGRESS', 'COMPLETED', 'NO_SHOW') "
                + "OR checked_in_at IS NOT NULL)", Integer.class, number);
        Integer usedCount = jdbc.queryForObject("SELECT COALESCE(used_count, 0) FROM subscription_pass WHERE pass_number = ?", Integer.class, number);
        if (pass.getStatus() == PassStatus.PARTIALLY_USED || (usedCount != null && usedCount > 0) || (bookingsUsed != null && bookingsUsed > 0)) {
            throw refused("ELEMENT_CONSOMME", "Le pass " + number + " a déjà été utilisé : retour arrière refusé");
        }
        passOperator.getObject().revertActivation(pass, reason);
        return "remis en attente d'activation, droits retirés";
    }

    private String revertBooking(String number, String reason) {
        BookingService service = bookings.getObject();
        BookingStatus status = service.get(number).status();
        if (CONSUMED_BOOKINGS.contains(status.name())) {
            throw refused("ELEMENT_CONSOMME", "La réservation " + number + " a déjà commencé (" + status + ") : retour arrière refusé");
        }
        if (status != BookingStatus.CONFIRMED) {
            return null;
        }
        service.revertConfirmation(number, reason);
        return "remise en attente de paiement, créneaux libérés";
    }

    @Transactional(readOnly = true)
    public List<PaymentAllocationReversal> ofTransaction(String transactionNumber) {
        return reversals.findByTransactionNumberOrderByReversedAtDesc(transactionNumber);
    }

    private static BadRequestException refused(String code, String message) {
        return BadRequestException.coded(code, message);
    }

    private String actor() {
        try {
            return staffProfiles.currentAuthor().login();
        } catch (RuntimeException ex) {
            return "SYSTEM";
        }
    }
}
