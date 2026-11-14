package com.sni.bokaticowork.features.billing.element.service;

import com.sni.bokaticowork.features.billing.element.dto.ElementDtos;
import com.sni.bokaticowork.features.billing.element.model.ElementRef;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Effets d'un paiement sur les elements qu'une facture regle (lot R2) : les memes que ceux d'un
 * paiement fait directement sur l'element.
 *
 * <ul>
 *   <li>abonnement : renouvellement paye, sortie de periode de grace, ou activation s'il attendait ;</li>
 *   <li>domiciliation : effets de son abonnement parent ; le contrat reste a confirmer par l'equipe ;</li>
 *   <li>pass : activation s'il attendait ;</li>
 *   <li>reservation : confirmation si elle attendait son paiement.</li>
 * </ul>
 *
 * <p>Un element n'est traite que s'il est regle selon son mode : au solde de toutes ses factures,
 * ou des le premier paiement quand l'administrateur l'a choisi au rattachement. Les effets sont
 * rejouables : un element deja actif ou confirme est laisse tel quel. Un element en echec n'empeche
 * pas les autres.</p>
 */
@Slf4j
@Service
public class ElementSettlementService {

    static final String ACTOR = "SYSTEM";

    private final BillingElementResolver resolver;
    private final ObjectProvider<SubscriptionService> subscriptions;
    private final ObjectProvider<SubscriptionGraceService> graces;
    private final ObjectProvider<PassService> passes;
    private final ObjectProvider<BookingService> bookings;

    public ElementSettlementService(BillingElementResolver resolver, ObjectProvider<SubscriptionService> subscriptions,
                                    ObjectProvider<SubscriptionGraceService> graces, ObjectProvider<PassService> passes,
                                    ObjectProvider<BookingService> bookings) {
        this.resolver = resolver;
        this.subscriptions = subscriptions;
        this.graces = graces;
        this.passes = passes;
        this.bookings = bookings;
    }

    /** Resultat par element : {@code APPLIED}, {@code WAITING} (pas encore regle) ou {@code FAILED}. */
    public record Outcome(String type, String code, String result, String detail) {
    }

    public List<Outcome> settle(String documentNumber, String reason) {
        Map<String, Object> document = resolver.document(documentNumber).orElse(null);
        if (document == null) {
            return List.of();
        }
        BigDecimal total = (BigDecimal) document.get("total_amount");
        BigDecimal balance = (BigDecimal) document.get("balance_due");
        boolean somethingPaid = total != null && balance != null && balance.compareTo(total) < 0;
        List<Outcome> outcomes = new ArrayList<>();
        for (ElementDtos.Settled settled : resolver.elements(documentNumber)) {
            ElementRef ref = new ElementRef(settled.type(), settled.code());
            try {
                boolean due = ElementRef.FIRST_PAYMENT.equals(settled.activationMode()) ? somethingPaid : fullySettled(ref);
                if (!due) {
                    outcomes.add(new Outcome(ref.type(), ref.code(), "WAITING", "Factures de l'élément pas encore soldées"));
                    continue;
                }
                outcomes.add(new Outcome(ref.type(), ref.code(), "APPLIED", apply(ref, reason)));
            } catch (RuntimeException ex) {
                log.warn("Effets du paiement de {} sur {} {} non appliques : {}", documentNumber, ref.type(), ref.code(), ex.getMessage());
                outcomes.add(new Outcome(ref.type(), ref.code(), "FAILED", ex.getMessage()));
            }
        }
        return outcomes;
    }

    /** Toutes les factures emises de l'element sont soldees, et il en a au moins une. */
    boolean fullySettled(ElementRef ref) {
        List<ElementDtos.Invoice> invoices = resolver.invoices(ref).stream()
                .filter(invoice -> List.of("INVOICE", "CORRECTIVE_INVOICE", "DEBIT_NOTE").contains(invoice.documentType()))
                .filter(invoice -> !List.of("DRAFT", "CANCELLED").contains(invoice.status()))
                .toList();
        return !invoices.isEmpty() && invoices.stream().allMatch(invoice -> invoice.balanceDue() == null || invoice.balanceDue().signum() <= 0);
    }

    String apply(ElementRef ref, String reason) {
        return switch (ref.type()) {
            case ElementRef.SUBSCRIPTION -> subscription(ref.code(), reason);
            case ElementRef.DOMICILIATION -> resolver.domiciliationSubscription(ref.code())
                    .map(subscription -> "Abonnement " + subscription + " : " + subscription(subscription, reason)
                            + " ; le contrat reste à confirmer par l'équipe")
                    .orElse("Domiciliation sans abonnement");
            case ElementRef.PASS -> pass(ref.code(), reason);
            default -> booking(ref.code(), reason);
        };
    }

    private String subscription(String number, String reason) {
        SubscriptionService service = subscriptions.getObject();
        if (service.applyPaidRenewal(number, reason)) {
            return "renouvellement appliqué";
        }
        Subscription subscription = service.getForService(number);
        if (subscription.getStatus() == SubscriptionStatus.GRACE_PERIOD) {
            graces.getObject().exit(subscription, reason);
            return "sortie de la période de grâce";
        }
        if (subscription.getStatus() == SubscriptionStatus.PENDING_ACTIVATION) {
            service.activate(number, new SubscriptionStatusChangeRequest("Activation après paiement : " + reason, ACTOR, Boolean.FALSE));
            return "activé";
        }
        return "déjà " + subscription.getStatus();
    }

    private String pass(String number, String reason) {
        PassService service = passes.getObject();
        PassStatus status = service.getForService(number).getStatus();
        if (status == PassStatus.PENDING_ACTIVATION) {
            service.activate(number, "Activation après paiement : " + reason);
            return "activé";
        }
        return "déjà " + status;
    }

    private String booking(String number, String reason) {
        BookingService service = bookings.getObject();
        BookingStatus status = service.get(number).status();
        if (status == BookingStatus.DRAFT || status == BookingStatus.PENDING_PAYMENT) {
            service.confirm(number, new BookingStatusChangeRequest("Confirmation après paiement : " + reason, ACTOR, Boolean.TRUE));
            return "confirmée";
        }
        return "déjà " + status;
    }
}
