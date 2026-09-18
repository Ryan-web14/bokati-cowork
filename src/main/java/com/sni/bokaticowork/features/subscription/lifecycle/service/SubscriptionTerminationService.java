package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.MailItem;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemRepository;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionExitItem;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionTerminationRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * La resiliation · un preavis qui court, des frais eventuels, une liste de sortie.
 *
 * <p>On peut toujours resilier. Ce que le module garantit, c'est que la date d'effet respecte le
 * preavis, que la rupture d'engagement est chiffree avant d'etre acceptee, et que l'abonnement
 * ne se ferme pas tant que la liste de sortie a une ligne obligatoire ouverte : un domicilie qui
 * part en laissant du courrier et un badge n'est pas « parti ».</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionTerminationService {

    static final String FEE_SOURCE = "TERMINATION_FEE";
    static final String BRIDGING_SOURCE = "TERMINATION_BRIDGING";

    private final SubscriptionTerminationRepository terminationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCommitmentService commitmentService;
    private final SubscriptionPolicyService policyService;
    private final ProrationCalculator proration;
    private final SubscriptionStatusManager statusManager;
    private final SubscriptionEventWriter eventWriter;
    private final SubscriptionBillingSupport billingSupport;
    private final BillingDocumentRepository billingDocumentRepository;
    private final WalletHoldRepository walletHoldRepository;
    private final DomiciliationContractRepository domiciliationContractRepository;
    private final MailItemRepository mailItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;
    private final @Lazy SubscriptionLifecycleOperator lifecycleOperator;

    public record Request(SubscriptionTermination.ReasonCategory reasonCategory, String reason, SubscriptionTermination.Channel channel,
                          LocalDate requestedEffectiveDate, Integer noticePeriodDays) {
    }

    /** Ce que la demande impliquerait · avant de la poser. */
    public record Preview(String subscriptionNumber, int noticePeriodDays, LocalDate earliestEffectiveDate, LocalDate effectiveDate,
                          boolean earlyTermination, LocalDate commitmentEnd, int remainingCommitmentMonths, BigDecimal feeAmount,
                          BigDecimal bridgingAmount, String currency, List<String> exitChecklist) {
    }

    // ---- Demander -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Preview preview(String subscriptionNumber, Request request) {
        Subscription subscription = subscription(subscriptionNumber);
        return preview(subscription, request);
    }

    private Preview preview(Subscription subscription, Request request) {
        SubscriptionPolicy policy = policyService.current();
        int notice = request.noticePeriodDays() != null ? Math.max(0, request.noticePeriodDays()) : policy.getDefaultNoticeDays();
        LocalDate earliest = LocalDate.now().plusDays(notice);
        LocalDate effective = request.requestedEffectiveDate() == null || request.requestedEffectiveDate().isBefore(earliest)
                ? earliest : request.requestedEffectiveDate();
        SubscriptionCommitmentService.EarlyTermination early = commitmentService.earlyTerminationOn(subscription, effective);
        BigDecimal bridging = bridgingAmount(subscription, effective, policy);
        List<String> checklist = checklistFor(subscription, early.fee()).stream().map(SubscriptionExitItem::getLabel).toList();
        return new Preview(subscription.getSubscriptionNumber(), notice, earliest, effective, early.early(), early.commitmentEnd(),
                early.remainingMonths(), early.fee(), bridging, subscription.getCurrency(), checklist);
    }

    /**
     * Pose le preavis · l'abonnement passe PENDING_TERMINATION et vit jusqu'a la date d'effet.
     *
     * <p>La date d'effet ne peut pas etre avant la fin du preavis. Elle peut etre apres : c'est
     * l'abonne qui choisit de partir plus tard. Les frais de rupture sont chiffres ici, poses sur
     * la demande, et factures a l'acceptation · pas avant, parce qu'une demande peut se retracter.</p>
     */
    @Transactional
    public SubscriptionTermination request(String subscriptionNumber, Request request, String actor) {
        Subscription subscription = subscription(subscriptionNumber);
        if (request.reasonCategory() == null) {
            throw new BadRequestException("Une résiliation a un motif · déménagement, coût, qualité de service…");
        }
        if (!EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING, SubscriptionStatus.PAST_DUE,
                SubscriptionStatus.GRACE_PERIOD, SubscriptionStatus.PAUSED, SubscriptionStatus.SUSPENDED).contains(subscription.getStatus())) {
            throw new ConflictException("termination", "un abonnement " + subscription.getStatus() + " ne se résilie pas par préavis");
        }
        terminationRepository.findOpenBySubscription(subscription.getId()).ifPresent(open -> {
            throw new ConflictException("termination", "un préavis court déjà · " + open.getTerminationCode());
        });
        Preview preview = preview(subscription, request);

        SubscriptionTermination termination = SubscriptionTermination.builder()
                .terminationCode(sequenceGenerator.next("subscription_termination"))
                .subscription(subscription)
                .requestedAt(Instant.now())
                .requestedBy(actor)
                .channel(request.channel() == null ? SubscriptionTermination.Channel.STAFF : request.channel())
                .noticePeriodDays(preview.noticePeriodDays())
                .effectiveDate(preview.effectiveDate())
                .reason(trim(request.reason()))
                .reasonCategory(request.reasonCategory())
                .earlyTermination(preview.earlyTermination())
                .remainingCommitmentMonths(preview.remainingCommitmentMonths())
                .feeAmount(preview.feeAmount())
                .build();
        for (SubscriptionExitItem item : checklistFor(subscription, preview.feeAmount())) {
            item.setTermination(termination);
            termination.getExitItems().add(item);
        }
        SubscriptionTermination saved = terminationRepository.save(termination);

        statusManager.changeStatus(subscription, SubscriptionStatus.PENDING_TERMINATION,
                "Préavis de résiliation · effet le " + preview.effectiveDate(), actor);
        subscriptionRepository.save(subscription);
        eventWriter.writeEvent(subscription, SubscriptionEventType.TERMINATION_REQUESTED,
                "{\"code\":\"" + saved.getTerminationCode() + "\",\"effectiveDate\":\"" + saved.getEffectiveDate() + "\",\"fee\":" + saved.getFeeAmount().toPlainString() + "}");
        notify(saved, "SUBSCRIPTION_TERMINATION_REQUESTED", "Votre demande de résiliation " + saved.getTerminationCode());
        log.info("Abonnement {} · preavis {} pose, effet le {}, frais {}", subscription.getSubscriptionNumber(),
                saved.getTerminationCode(), saved.getEffectiveDate(), saved.getFeeAmount());
        return saved;
    }

    // ---- Accepter, retracter ------------------------------------------------------------------

    /**
     * Accepte · les frais de rupture et les jours au-dela de la periode payee sont factures ici.
     *
     * <p>Un abonnement en preavis ne se renouvelle pas. Si la date d'effet depasse la fin de la
     * periode en cours, les jours entre les deux sont factures au prorata de la politique, en une
     * fois · c'est ce qui permet de ne pas lancer un renouvellement pour trois semaines.</p>
     */
    @Transactional
    public SubscriptionTermination accept(String terminationCode, String actor) {
        SubscriptionTermination termination = get(terminationCode);
        if (termination.getStatus() != SubscriptionTermination.Status.REQUESTED) {
            throw new BadRequestException("Ce préavis n'attend pas d'acceptation · il est " + termination.getStatus());
        }
        Subscription subscription = termination.getSubscription();
        termination.setStatus(SubscriptionTermination.Status.ACCEPTED);
        termination.setAcceptedBy(actor);
        termination.setAcceptedAt(Instant.now());

        BigDecimal fee = termination.feeDue();
        if (fee.signum() > 0) {
            String billable = billingSupport.createStandaloneBillableItem(subscription, FEE_SOURCE,
                    "Frais de rupture d'engagement · " + termination.getRemainingCommitmentMonths() + " mois restant(s)", fee,
                    LocalDate.now(), termination.getEffectiveDate());
            termination.setFeeBillableNumber(billable);
        } else {
            termination.getExitItems().stream().filter(i -> SubscriptionExitItem.TERMINATION_FEE.equals(i.getItemCode()))
                    .forEach(i -> done(i, "SYSTEM", Boolean.TRUE.equals(termination.getFeeWaived()) ? "dispensé" : "sans objet"));
        }
        BigDecimal bridging = bridgingAmount(subscription, termination.getEffectiveDate(), policyService.current());
        if (bridging.signum() > 0) {
            String billable = billingSupport.createStandaloneBillableItem(subscription, BRIDGING_SOURCE,
                    "Jours jusqu'à la date d'effet de la résiliation", bridging,
                    subscription.getCurrentPeriodEnd().plusDays(1), termination.getEffectiveDate());
            termination.setBridgingBillableNumber(billable);
        }
        eventWriter.writeEvent(subscription, SubscriptionEventType.TERMINATION_ACCEPTED,
                "{\"code\":\"" + termination.getTerminationCode() + "\",\"fee\":" + fee.toPlainString() + ",\"bridging\":" + bridging.toPlainString() + "}");
        notify(termination, "SUBSCRIPTION_TERMINATION_ACCEPTED", "Résiliation acceptée · effet le " + termination.getEffectiveDate());
        return terminationRepository.save(termination);
    }

    /** L'abonne change d'avis · l'abonnement reprend son cours, le preavis reste ecrit. */
    @Transactional
    public SubscriptionTermination retract(String terminationCode, String reason, String actor) {
        SubscriptionTermination termination = get(terminationCode);
        if (!termination.getStatus().open()) {
            throw new BadRequestException("Ce préavis ne peut plus être retiré · il est " + termination.getStatus());
        }
        if (termination.getStatus() == SubscriptionTermination.Status.ACCEPTED
                && (termination.getFeeBillableNumber() != null || termination.getBridgingBillableNumber() != null)) {
            throw new ConflictException("termination", "des frais ont déjà été facturés · émettez un avoir avant de retirer le préavis");
        }
        Subscription subscription = termination.getSubscription();
        termination.setStatus(SubscriptionTermination.Status.RETRACTED);
        termination.setRetractedAt(Instant.now());
        termination.setRetractedBy(actor);
        termination.setNotes(join(termination.getNotes(), "Retiré · " + (StringUtils.hasText(reason) ? reason.trim() : "sans motif")));
        if (subscription.getStatus() == SubscriptionStatus.PENDING_TERMINATION) {
            statusManager.changeStatus(subscription, SubscriptionStatus.ACTIVE, "Préavis retiré", actor);
            subscriptionRepository.save(subscription);
        }
        eventWriter.writeEvent(subscription, SubscriptionEventType.TERMINATION_RETRACTED, "{\"code\":\"" + terminationCode + "\"}");
        return terminationRepository.save(termination);
    }

    /** Dispense des frais de rupture · motivee, signee, avant l'acceptation. */
    @Transactional
    public SubscriptionTermination waiveFee(String terminationCode, String reason, String actor) {
        SubscriptionTermination termination = get(terminationCode);
        if (termination.getStatus() != SubscriptionTermination.Status.REQUESTED) {
            throw new BadRequestException("La dispense se décide avant l'acceptation");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Une dispense se motive");
        }
        if (termination.getFeeAmount().signum() == 0) {
            throw new BadRequestException("Il n'y a pas de frais à dispenser");
        }
        termination.setFeeWaived(true);
        termination.setFeeWaivedBy(actor);
        termination.setFeeWaivedReason(reason.trim());
        return terminationRepository.save(termination);
    }

    // ---- La liste de sortie -----------------------------------------------------------------

    @Transactional
    public SubscriptionTermination tick(String terminationCode, String itemCode, String detail, String actor) {
        SubscriptionTermination termination = get(terminationCode);
        SubscriptionExitItem item = item(termination, itemCode);
        done(item, actor, detail);
        return terminationRepository.save(termination);
    }

    @Transactional
    public SubscriptionTermination waiveItem(String terminationCode, String itemCode, String reason, String actor) {
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Une ligne de sortie se dispense avec un motif");
        }
        SubscriptionTermination termination = get(terminationCode);
        SubscriptionExitItem item = item(termination, itemCode);
        item.setStatus(SubscriptionExitItem.Status.WAIVED);
        item.setDoneBy(actor);
        item.setDoneAt(Instant.now());
        item.setDetail(reason.trim());
        return terminationRepository.save(termination);
    }

    // ---- Achever ------------------------------------------------------------------------------

    /**
     * Acheve les preavis arrives a leur date d'effet · appele chaque jour.
     *
     * <p>Le solde et le courrier sont verifies automatiquement : sans facture ouverte, la ligne
     * « solde » se coche seule ; sans courrier en attente, la ligne « courrier » aussi. Ce qui reste
     * ouvert bloque, et se voit dans la liste des sorties bloquees.</p>
     */
    @Transactional
    public int completeDue() {
        int completed = 0;
        for (SubscriptionTermination termination : terminationRepository.findDue(LocalDate.now())) {
            try {
                if (complete(termination)) {
                    completed++;
                }
            } catch (RuntimeException ex) {
                log.warn("Preavis {} · achevement impossible : {}", termination.getTerminationCode(), ex.getMessage());
            }
        }
        return completed;
    }

    @Transactional
    public boolean complete(SubscriptionTermination termination) {
        if (termination.getStatus() != SubscriptionTermination.Status.ACCEPTED) {
            throw new BadRequestException("Seul un préavis accepté s'achève");
        }
        Subscription subscription = termination.getSubscription();
        autoTick(termination, subscription);
        if (!termination.exitChecklistClear()) {
            List<String> open = termination.getExitItems().stream()
                    .filter(i -> Boolean.TRUE.equals(i.getMandatory()) && i.getStatus() == SubscriptionExitItem.Status.PENDING)
                    .map(SubscriptionExitItem::getLabel).toList();
            log.info("Preavis {} · sortie bloquee : {}", termination.getTerminationCode(), open);
            terminationRepository.save(termination);
            return false;
        }
        lifecycleOperator.cancel(subscription, new SubscriptionStatusChangeRequest(
                "Résiliation " + termination.getTerminationCode() + " · " + termination.getReasonCategory(), "SYSTEM", Boolean.FALSE));
        termination.setStatus(SubscriptionTermination.Status.COMPLETED);
        termination.setCompletedAt(Instant.now());
        terminationRepository.save(termination);
        notify(termination, "SUBSCRIPTION_TERMINATION_COMPLETED", "Votre abonnement " + subscription.getSubscriptionNumber() + " est clos");
        return true;
    }

    /** Force l'achevement avant la date d'effet · le client est deja parti, la liste est vide. */
    @Transactional
    public SubscriptionTermination completeNow(String terminationCode, String actor) {
        SubscriptionTermination termination = get(terminationCode);
        if (!complete(termination)) {
            throw new ConflictException("termination", "la liste de sortie a encore des lignes obligatoires ouvertes");
        }
        termination.setNotes(join(termination.getNotes(), "Achevé par " + actor + " avant la date d'effet"));
        return terminationRepository.save(termination);
    }

    // ---- Lire -----------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SubscriptionTermination get(String terminationCode) {
        return terminationRepository.findByTerminationCode(terminationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Préavis introuvable"));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionTermination> ofSubscription(String subscriptionNumber) {
        return terminationRepository.findBySubscription_IdOrderByRequestedAtDesc(subscription(subscriptionNumber).getId());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionTermination> byStatus(SubscriptionTermination.Status status) {
        return terminationRepository.findByStatusOrderByEffectiveDateAsc(status);
    }

    /** Les sorties bloquees · date d'effet passee, liste encore ouverte. */
    @Transactional(readOnly = true)
    public List<SubscriptionTermination> blocked() {
        return terminationRepository.findDue(LocalDate.now());
    }

    // ---- Interne --------------------------------------------------------------------------------

    private BigDecimal bridgingAmount(Subscription subscription, LocalDate effectiveDate, SubscriptionPolicy policy) {
        if (subscription.getCurrentPeriodEnd() == null || !effectiveDate.isAfter(subscription.getCurrentPeriodEnd())
                || subscription.getCurrentPeriodStart() == null) {
            return BigDecimal.ZERO;
        }
        // Les jours au-dela de la periode payee, au prix de la periode, selon la politique · en une fois
        LocalDate from = subscription.getCurrentPeriodEnd().plusDays(1);
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd()) + 1;
        LocalDate virtualEnd = from.plusDays(periodDays - 1);
        BigDecimal total = BigDecimal.ZERO;
        LocalDate cursor = from;
        while (!cursor.isAfter(effectiveDate)) {
            LocalDate end = virtualEnd.isAfter(effectiveDate) ? effectiveDate : virtualEnd;
            total = total.add(proration.share(subscription.getTotalAmount(), from, virtualEnd, cursor, end, policy.getProrationPolicy()));
            cursor = virtualEnd.plusDays(1);
            from = cursor;
            virtualEnd = from.plusDays(periodDays - 1);
        }
        return total;
    }

    private List<SubscriptionExitItem> checklistFor(Subscription subscription, BigDecimal fee) {
        List<SubscriptionExitItem> items = new ArrayList<>();
        items.add(SubscriptionExitItem.builder().itemCode(SubscriptionExitItem.BADGE_RETURN).label("Badge d'accès restitué").mandatory(true).build());
        items.add(SubscriptionExitItem.builder().itemCode(SubscriptionExitItem.KEYS_RETURN).label("Clés et matériel confié restitués").mandatory(false).build());
        items.add(SubscriptionExitItem.builder().itemCode(SubscriptionExitItem.BALANCE_DUE).label("Solde réglé · aucune facture ouverte").mandatory(true).build());
        boolean domiciled = domiciliationContractRepository.findFirstBySubscription_IdAndStatusNotInOrderByCreatedAtDesc(subscription.getId(),
                EnumSet.of(DomiciliationContract.Status.TERMINATED, DomiciliationContract.Status.EXPIRED)).isPresent();
        if (domiciled) {
            items.add(SubscriptionExitItem.builder().itemCode(SubscriptionExitItem.MAIL_PENDING).label("Courrier en attente retiré ou réexpédié").mandatory(true).build());
            items.add(SubscriptionExitItem.builder().itemCode(SubscriptionExitItem.DOMICILIATION_END).label("Contrat de domiciliation résilié").mandatory(true).build());
        }
        if (!walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode("ACTIVE", "SUBSCRIPTION", subscription.getSubscriptionNumber()).isEmpty()) {
            items.add(SubscriptionExitItem.builder().itemCode(SubscriptionExitItem.DEPOSIT_SETTLEMENT).label("Caution libérée ou imputée").mandatory(false).build());
        }
        if (fee != null && fee.signum() > 0) {
            items.add(SubscriptionExitItem.builder().itemCode(SubscriptionExitItem.TERMINATION_FEE).label("Frais de rupture d'engagement réglés").mandatory(true).build());
        }
        return items;
    }

    /** Ce qui se verifie tout seul se coche tout seul. */
    private void autoTick(SubscriptionTermination termination, Subscription subscription) {
        for (SubscriptionExitItem item : termination.getExitItems()) {
            if (item.getStatus() != SubscriptionExitItem.Status.PENDING) continue;
            switch (item.getItemCode()) {
                case SubscriptionExitItem.BALANCE_DUE, SubscriptionExitItem.TERMINATION_FEE -> {
                    if (billingDocumentRepository.findRecoverableDocuments(subscription.getSubscriberType().name(), subscription.getSubscriberCode()).isEmpty()) {
                        done(item, "SYSTEM", "aucune facture ouverte");
                    }
                }
                case SubscriptionExitItem.MAIL_PENDING -> domiciliationContractRepository.findBySubscription_Id(subscription.getId()).stream()
                        .filter(c -> mailItemRepository.countByContract_IdAndStatusIn(c.getId(),
                                EnumSet.of(MailItem.Status.RECEIVED, MailItem.Status.NOTIFIED, MailItem.Status.SCANNED)) > 0)
                        .findAny().ifPresentOrElse(c -> { }, () -> done(item, "SYSTEM", "aucun courrier en attente"));
                case SubscriptionExitItem.DOMICILIATION_END -> {
                    if (domiciliationContractRepository.findFirstBySubscription_IdAndStatusNotInOrderByCreatedAtDesc(subscription.getId(),
                            EnumSet.of(DomiciliationContract.Status.TERMINATED, DomiciliationContract.Status.EXPIRED)).isEmpty()) {
                        done(item, "SYSTEM", "contrat de domiciliation clos");
                    }
                }
                default -> { }
            }
        }
    }

    private static void done(SubscriptionExitItem item, String actor, String detail) {
        item.setStatus(SubscriptionExitItem.Status.DONE);
        item.setDoneBy(actor);
        item.setDoneAt(Instant.now());
        item.setDetail(StringUtils.hasText(detail) ? detail.trim() : null);
    }

    private static SubscriptionExitItem item(SubscriptionTermination termination, String itemCode) {
        return termination.getExitItems().stream().filter(i -> i.getItemCode().equalsIgnoreCase(itemCode)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de sortie introuvable · " + itemCode));
    }

    private void notify(SubscriptionTermination termination, String eventType, String subject) {
        Subscription subscription = termination.getSubscription();
        String email = recipientEmail(subscription);
        if (!StringUtils.hasText(email)) {
            return;
        }
        outboxService.publish(eventType, "SUBSCRIPTION", subscription.getSubscriptionNumber(), Map.of(
                "recipientEmail", email,
                "subject", subject,
                "templateCode", eventType.toLowerCase(),
                "subscriptionNumber", subscription.getSubscriptionNumber(),
                "terminationCode", termination.getTerminationCode(),
                "effectiveDate", termination.getEffectiveDate().toString(),
                "feeAmount", termination.feeDue().toPlainString()
        ));
    }

    private static String recipientEmail(Subscription subscription) {
        if (subscription.getMember() != null) return subscription.getMember().getEmail();
        if (subscription.getCustomer() != null) {
            return StringUtils.hasText(subscription.getCustomer().getBillingEmail())
                    ? subscription.getCustomer().getBillingEmail() : subscription.getCustomer().getEmail();
        }
        if (subscription.getBusinessEntity() != null) return subscription.getBusinessEntity().getEmail();
        return null;
    }

    private Subscription subscription(String number) {
        return subscriptionRepository.findBySubscriptionNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String join(String existing, String more) {
        return StringUtils.hasText(existing) ? existing + " · " + more : more;
    }
}
