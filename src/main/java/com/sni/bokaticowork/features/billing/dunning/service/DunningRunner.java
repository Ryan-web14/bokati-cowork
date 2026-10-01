package com.sni.bokaticowork.features.billing.dunning.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.utils.mail.Recipients;
import com.sni.bokaticowork.features.billing.dunning.model.DunningNotice;
import com.sni.bokaticowork.features.billing.dunning.model.DunningPolicy;
import com.sni.bokaticowork.features.billing.dunning.model.DunningStep;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningNoticeRepository;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningPolicyRepository;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La relance qui s'execute · chaque jour, facture par facture, palier par palier.
 *
 * <p>Pour chaque facture echue avec un solde, la politique du segment du client est lue, et tout
 * palier dont le jour est atteint et qui n'a pas encore ete execute pour cette facture l'est ·
 * une fois, jamais deux (contrainte unique). Un palier n'attend pas le precedent : une facture
 * decouverte tard recoit le rappel et la mise en demeure le meme jour, dans l'ordre. La boucle est
 * dans le worker · chaque palier s'execute ici dans sa propre transaction.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DunningRunner {

    private final DunningPolicyRepository policyRepository;
    private final DunningNoticeRepository noticeRepository;
    private final BillingDocumentRepository documentRepository;
    private final BillableItemRepository billableItemRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OutboxService outboxService;
    private final MemberInAppNotifier inAppNotifier;
    private final SubscriptionGraceService graceService;
    private final @Lazy SubscriptionLifecycleOperator lifecycleOperator;

    @Value("${bokati.billing.dunning.handover-email:}")
    private String handoverEmail;

    @Value("${bokati.billing.dunning.batch-size:200}")
    private int batchSize;

    /** Les factures a parcourir · le worker boucle dessus et appelle {@link #execute} palier par palier. */
    @Transactional(readOnly = true)
    public java.util.List<BillingDocument> overdueDocuments() {
        if (!policyRepository.existsByActiveTrue()) {
            return java.util.List.of();
        }
        return documentRepository.findOverdueWithBalance(batchSize);
    }

    /** Les paliers atteints et pas encore executes pour cette facture, dans l'ordre. */
    @Transactional(readOnly = true)
    public java.util.List<DunningStep> dueSteps(BillingDocument document) {
        int daysOverdue = daysOverdue(document);
        return policyFor(document.getCustomerType()).map(policy -> policy.getSteps().stream()
                .filter(step -> step.getDaysAfterDue() <= daysOverdue)
                .filter(step -> !noticeRepository.existsByDocumentNumberAndStep_Id(document.getDocumentNumber(), step.getId()))
                .toList()).orElse(java.util.List.of());
    }

    /** Execute un palier pour une facture · dans sa propre transaction, l'echec d'une facture n'arrete pas les autres. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DunningNotice execute(BillingDocument document, DunningStep step) {
        Subscription subscription = subscriptionBehind(document).orElse(null);
        DunningNotice.Outcome outcome = DunningNotice.Outcome.SENT;
        String detail = null;
        Map<String, String> vars = variables(document, step, subscription);
        try {
            switch (step.getAction()) {
                case REMINDER, FORMAL_NOTICE -> {
                    boolean delivered = notify(document, step, vars, subscription);
                    if (!delivered) {
                        outcome = DunningNotice.Outcome.SKIPPED;
                        detail = "aucune adresse de contact";
                    }
                }
                case GRACE_PERIOD -> {
                    if (subscription == null) {
                        outcome = DunningNotice.Outcome.SKIPPED;
                        detail = "facture sans abonnement";
                    } else if (!graceService.enter(subscription, "Relance · facture " + document.getDocumentNumber() + " échue")) {
                        outcome = DunningNotice.Outcome.SKIPPED;
                        detail = "abonnement " + subscription.getStatus();
                    }
                    notify(document, step, vars, subscription);
                }
                case SUSPEND -> {
                    if (subscription == null) {
                        outcome = DunningNotice.Outcome.SKIPPED;
                        detail = "facture sans abonnement";
                    } else if (subscription.getStatus() == SubscriptionStatus.ACTIVE || subscription.getStatus() == SubscriptionStatus.PAST_DUE
                            || subscription.getStatus() == SubscriptionStatus.GRACE_PERIOD) {
                        lifecycleOperator.suspend(subscription, new SubscriptionStatusChangeRequest(
                                "Suspension pour impayé · facture " + document.getDocumentNumber(), "SYSTEM:DUNNING", Boolean.FALSE));
                        notify(document, step, vars, subscription);
                    } else {
                        outcome = DunningNotice.Outcome.SKIPPED;
                        detail = "abonnement " + subscription.getStatus();
                    }
                }
                case HANDOVER -> {
                    List<String> recipients = Recipients.split(handoverEmail);
                    if (recipients.isEmpty()) {
                        outcome = DunningNotice.Outcome.SKIPPED;
                        detail = "bokati.billing.dunning.handover-email non renseigné";
                    } else {
                        // Une adresse ou plusieurs · un courriel par personne qui reprend le dossier
                        for (String recipient : recipients) {
                            Map<String, Object> payload = new HashMap<>(vars);
                            payload.put("adminEmail", recipient);
                            payload.put("subject", render(step.getSubjectTemplate(), vars));
                            payload.put("message", render(step.getMessageTemplate(), vars));
                            payload.put("templateCode", "BILLING_DUNNING_HANDOVER");
                            outboxService.publish("BILLING_DUNNING_HANDOVER", "BILLING_DOCUMENT", document.getDocumentNumber() + ":" + recipient, payload);
                        }
                    }
                }
            }
        } catch (RuntimeException ex) {
            outcome = DunningNotice.Outcome.FAILED;
            detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        }
        return noticeRepository.save(DunningNotice.builder()
                .documentNumber(document.getDocumentNumber()).step(step).policyCode(step.getPolicy().getPolicyCode()).action(step.getAction())
                .customerType(document.getCustomerType()).customerCode(document.getCustomerCode())
                .subscriptionNumber(subscription == null ? null : subscription.getSubscriptionNumber())
                .daysOverdue(daysOverdue(document)).balanceDue(document.getBalanceDue()).outcome(outcome).detail(detail).build());
    }

    /** La politique du segment, sinon celle par defaut. */
    @Transactional(readOnly = true)
    public Optional<DunningPolicy> policyFor(String customerType) {
        DunningPolicy.Segment segment;
        try {
            segment = DunningPolicy.Segment.valueOf(customerType == null ? "DEFAULT" : customerType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            segment = DunningPolicy.Segment.DEFAULT;
        }
        return policyRepository.findFirstBySegmentAndActiveTrue(segment)
                .or(() -> policyRepository.findFirstBySegmentAndActiveTrue(DunningPolicy.Segment.DEFAULT));
    }

    static int daysOverdue(BillingDocument document) {
        return document.getDueDate() == null ? 0 : (int) Math.max(0, ChronoUnit.DAYS.between(document.getDueDate(), LocalDate.now()));
    }

    /** L'abonnement derriere une facture, s'il y en a un · via les elements factures. */
    Optional<Subscription> subscriptionBehind(BillingDocument document) {
        if (document.getId() == null) {
            return Optional.empty();
        }
        return billableItemRepository.findByInvoiceId(document.getId()).stream()
                .filter(item -> item.getSourceType() != null && item.getSourceType().startsWith("SUBSCRIPTION"))
                .map(BillableItem::getSourceId).filter(StringUtils::hasText).findFirst()
                .flatMap(subscriptionRepository::findBySubscriptionNumber);
    }

    private boolean notify(BillingDocument document, DunningStep step, Map<String, String> vars, Subscription subscription) {
        String email = document.getCustomerEmail();
        if (!StringUtils.hasText(email) && subscription != null) {
            email = SubscriptionGraceService.recipientEmail(subscription);
        }
        String subject = render(step.getSubjectTemplate(), vars);
        String message = render(step.getMessageTemplate(), vars);
        boolean delivered = false;
        if (StringUtils.hasText(email) && step.getChannel() != DunningStep.Channel.IN_APP) {
            Map<String, Object> payload = new HashMap<>(vars);
            payload.put("recipientEmail", email);
            payload.put("subject", subject);
            payload.put("message", message);
            payload.put("templateCode", "billing_dunning_" + step.getAction().name().toLowerCase());
            payload.put("tone", step.getPolicy().getTone().name());
            outboxService.publish("BILLING_DUNNING_" + step.getAction().name(), "BILLING_DOCUMENT", document.getDocumentNumber(), payload);
            delivered = true;
        }
        if ("MEMBER".equalsIgnoreCase(document.getCustomerType()) && StringUtils.hasText(email)) {
            try {
                inAppNotifier.notify("BILLING_DUNNING_" + step.getAction().name(), "BILLING_DOCUMENT", document.getDocumentNumber(),
                        email, document.getCustomerName(), document.getCustomerCode(), subject, Map.copyOf(vars));
                delivered = true;
            } catch (RuntimeException ex) {
                log.debug("Notification in-app impossible pour {} · {}", document.getDocumentNumber(), ex.getMessage());
            }
        }
        return delivered;
    }

    private static Map<String, String> variables(BillingDocument document, DunningStep step, Subscription subscription) {
        Map<String, String> vars = new HashMap<>();
        vars.put("documentNumber", document.getDocumentNumber());
        vars.put("customerName", nz(document.getCustomerName()));
        vars.put("customerCode", nz(document.getCustomerCode()));
        vars.put("balanceDue", plain(document.getBalanceDue()));
        vars.put("totalAmount", plain(document.getTotalAmount()));
        vars.put("currency", nz(document.getCurrency()));
        vars.put("dueDate", document.getDueDate() == null ? "" : document.getDueDate().toString());
        vars.put("daysOverdue", String.valueOf(daysOverdue(document)));
        vars.put("stepOrder", String.valueOf(step.getStepOrder()));
        vars.put("subscriptionNumber", subscription == null ? "" : subscription.getSubscriptionNumber());
        return vars;
    }

    static String render(String template, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        String out = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
