package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PauseSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;

import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleOperator {

    private static final String PAYMENT_DUE_EVENT = "SUBSCRIPTION_RENEWAL_PAYMENT_DUE";
    /** Les statuts d'ou un renouvellement paye repart · un abonnement clos ne revit pas. */
    private static final java.util.Set<SubscriptionStatus> RENEWABLE_ON_PAYMENT = java.util.EnumSet.of(
            SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD, SubscriptionStatus.PAST_DUE, SubscriptionStatus.SUSPENDED);

    private final SubscriptionRepository subscriptionRepository;
    private final EntitlementGrantRepository entitlementGrantRepository;
    private final SubscriptionPeriodCalculator periodCalculator;
    private final SubscriptionBillingSupport billingSupport;
    private final SubscriptionStatusManager statusManager;
    private final SubscriptionEventWriter eventWriter;
    private final @Lazy EntitlementService entitlementService;
    private final @Lazy ContractService contractService;
    private final SubscriptionEmailNotifier emailNotifier;
    private final MemberInAppNotifier memberInAppNotifier;
    private final WalletHoldRepository walletHoldRepository;
    private final WalletHoldService walletHoldService;
    private final SubscriptionDepositSettlement depositSettlement;
    private final BillingDocumentRepository billingDocumentRepository;
    private final @Lazy com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService billingDocumentService;
    private final KycCaseRepository kycCaseRepository;
    private final OutboxService outboxService;
    private SubscriptionWhatsAppNotifier whatsAppNotifier;

    /** WhatsApp abonnement_actif en plus du courriel ; absent, seul le courriel part. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setWhatsAppNotifier(SubscriptionWhatsAppNotifier whatsAppNotifier) {
        this.whatsAppNotifier = whatsAppNotifier;
    }

    private void notifyWhatsApp(Subscription subscription, SubscriptionEventType eventType) {
        if (whatsAppNotifier == null) {
            return;
        }
        try {
            whatsAppNotifier.notify(subscription, eventType);
        } catch (Exception ex) {
            log.warn("WhatsApp {} de l'abonnement {} non envoye : {}", eventType, subscription.getSubscriptionNumber(), ex.getMessage());
        }
    }
    private final @Lazy com.sni.bokaticowork.features.subscription.derivation.service.PlanDerivationService derivationService;
    private final @Lazy com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionFreezeService freezeService;
    private final @Lazy com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionDirectDebitService directDebitService;
    private final @Lazy com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService graceService;
    /** Soi-meme, pour que chaque abonnement d'un balayage obtienne sa propre transaction. */
    private final @Lazy SubscriptionLifecycleOperator self;

    private com.sni.bokaticowork.features.billing.service.support.MemberBillingContact memberContact;

    /** Absent : les rappels de paiement partent a l'adresse du compte. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setMemberContact(com.sni.bokaticowork.features.billing.service.support.MemberBillingContact memberContact) {
        this.memberContact = memberContact;
    }


    public void activate(Subscription subscription, String reason, String actor) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }
        // Des pieces manquent · l'abonnement attend, il n'est pas active avec un avertissement
        if (!kycCompliant(subscription)) {
            if (subscription.getStatus() != SubscriptionStatus.PENDING_DOCUMENTS) {
                statusManager.changeStatus(subscription, SubscriptionStatus.PENDING_DOCUMENTS,
                        "Pièces KYC manquantes pour le niveau exigé par le plan", actor);
                subscriptionRepository.save(subscription);
                eventWriter.writeEvent(subscription, SubscriptionEventType.DOCUMENTS_PENDING, null);
            }
            return;
        }
        statusManager.changeStatus(subscription, SubscriptionStatus.ACTIVE, reason, actor);
        subscriptionRepository.save(subscription);
        // Use currently-valid ACTIVE grants only · expired/depleted/pass grants must not block re-activation
        if (entitlementGrantRepository.findCurrentlyActiveBySubscription(subscription.getId(), Instant.now()).isEmpty()) {
            entitlementService.grantForSubscription(subscription);
            eventWriter.writeEvent(subscription, SubscriptionEventType.ENTITLEMENTS_GRANTED, null);
        }
        eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_ACTIVATED, null);
        notifyInApp(subscription, SubscriptionEventType.SUBSCRIPTION_ACTIVATED);

        try {
            emailNotifier.notify(subscription, SubscriptionEventType.SUBSCRIPTION_ACTIVATED);
        } catch (Exception ex) {
            log.warn("Failed to send activation email for subscription {} · continuing",
                    subscription.getSubscriptionNumber(), ex);
        }
        notifyWhatsApp(subscription, SubscriptionEventType.SUBSCRIPTION_ACTIVATED);

        if (!StringUtils.hasText(subscription.getContractCode())) {
            outboxService.publish(
                    "CONTRACT_GENERATION_REQUESTED",
                    "SUBSCRIPTION",
                    subscription.getSubscriptionNumber(),
                    java.util.Map.of("sourceType", "SUBSCRIPTION", "sourceId", subscription.getId())
            );
        }
    }

    public Subscription suspend(Subscription subscription, SubscriptionStatusChangeRequest request) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.PAST_DUE
                && subscription.getStatus() != SubscriptionStatus.GRACE_PERIOD) {
            throw new ConflictException("subscription", "only ACTIVE, PAST_DUE or GRACE_PERIOD subscriptions can be suspended");
        }
        statusManager.changeStatus(subscription, SubscriptionStatus.SUSPENDED, reason(request, "Suspension"), actor(request));
        subscription.setSuspendedAt(Instant.now());
        subscription.setSuspensionReason(reason(request, null));
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.PAUSED);
        try {
            emailNotifier.notify(subscription, SubscriptionEventType.SUBSCRIPTION_SUSPENDED);
        } catch (Exception ex) {
            log.warn("Failed to send suspension email for subscription {} · continuing",
                    subscription.getSubscriptionNumber(), ex);
        }
        return subscriptionRepository.save(subscription);
    }

    public Subscription pause(Subscription subscription, PauseSubscriptionRequest request) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ConflictException("subscription", "only ACTIVE subscriptions can be paused");
        }
        LocalDate pauseUntil = resolvePauseUntil(request);
        // Le gel est encadre · quota annuel, duree maximale, part facturee · c'est la politique qui decide
        freezeService.start(subscription, pauseUntil, pauseReason(request, null), pauseActor(request));
        statusManager.changeStatus(subscription, SubscriptionStatus.PAUSED, pauseReason(request, "Subscription paused"), pauseActor(request));
        subscription.setPausedAt(Instant.now());
        subscription.setPauseUntil(pauseUntil);
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.PAUSED);
        return subscriptionRepository.save(subscription);
    }

    public Subscription resume(Subscription subscription, SubscriptionStatusChangeRequest request) {
        if (subscription.getStatus() != SubscriptionStatus.PAUSED) {
            throw new ConflictException("subscription", "only PAUSED subscriptions can be resumed");
        }
        long pausedDays = subscription.getPauseUntil() == null
                ? 0
                : Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), subscription.getPauseUntil()));
        if (pausedDays > 0) {
            if (subscription.getCurrentPeriodEnd() != null) {
                subscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd().plusDays(pausedDays));
            }
            if (subscription.getNextBillingDate() != null) {
                subscription.setNextBillingDate(subscription.getNextBillingDate().plusDays(pausedDays));
            }
        }
        subscription.setPauseUntil(null);
        if (pausedDays > 0) {
            followOnContract(subscription, "Reprise de l'abonnement après une pause");
        }
        freezeService.end(subscription, LocalDate.now());
        statusManager.changeStatus(subscription, SubscriptionStatus.ACTIVE, reason(request, "Subscription resumed"), actor(request));
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.ACTIVE);
        return subscriptionRepository.save(subscription);
    }

    public Subscription cancel(Subscription subscription, SubscriptionStatusChangeRequest request) {
        if (request != null && Boolean.TRUE.equals(request.cancelAtPeriodEnd())) {
            subscription.setCancelAtPeriodEnd(Boolean.TRUE);
            return subscriptionRepository.save(subscription);
        }
        String cancelReason = reason(request, "Subscription cancelled");
        statusManager.changeStatus(subscription, SubscriptionStatus.CANCELLED, cancelReason, actor(request));
        subscription.setCancelledAt(Instant.now());
        subscription.setCancellationReason(cancelReason);
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.CANCELLED);
        revokeActiveGrants(subscription);
        cancelAssociatedContract(subscription, cancelReason);
        dropPendingRenewal(subscription, cancelReason);
        releaseDepositHold(subscription);
        Subscription saved = subscriptionRepository.save(subscription);
        notifyInApp(saved, SubscriptionEventType.SUBSCRIPTION_CANCELLED);
        try {
            emailNotifier.notify(saved, SubscriptionEventType.SUBSCRIPTION_CANCELLED);
        } catch (Exception ex) {
            log.warn("Failed to send cancellation email for subscription {} · continuing",
                    saved.getSubscriptionNumber(), ex);
        }
        return saved;
    }

    /**
     * Le depot de garantie a l'annulation · rendu, puis employe a regler les impayes de l'abonne,
     * voir {@link SubscriptionDepositSettlement}. Un echec laisse le depot retenu, a traiter par
     * l'administration, sans faire echouer l'annulation.
     */
    private void releaseDepositHold(Subscription subscription) {
        if (walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode(
                "ACTIVE", "SUBSCRIPTION", subscription.getSubscriptionNumber()).isEmpty()) {
            return;
        }
        try {
            for (SubscriptionDepositSettlement.Outcome outcome : depositSettlement.settle(subscription.getSubscriptionNumber(),
                    subscription.getSubscriberType().name(), subscription.getSubscriberCode())) {
                eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_CANCELLED, outcome.toJson());
            }
        } catch (Exception ex) {
            log.error("Abonnement {} · depot de garantie non regle a l'annulation, il reste retenu : {}",
                    subscription.getSubscriptionNumber(), ex.getMessage(), ex);
        }
    }

    public void renew(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ConflictException("subscription", "only ACTIVE subscriptions can be renewed");
        }
        if (subscription.getPendingRenewalInvoice() != null) {
            // Un seul renouvellement a la fois · la facture emise reste celle a regler.
            return;
        }
        renewActive(subscription);
    }

    /**
     * Les echeances du jour · un abonnement qui echoue n'empeche plus les autres d'etre factures.
     *
     * <p>Le balayage s'executait dans une transaction unique et sans rattrapage par abonnement :
     * un seul abonnement en erreur annulait la passe entiere, y compris les renouvellements deja
     * ecrits. Le worker consignait l'echec et recommencait un quart d'heure plus tard sur la meme
     * liste, avec le meme abonnement en tete et le meme resultat · plus rien n'etait facture, pour
     * personne, indefiniment.</p>
     *
     * <p>Chaque abonnement a desormais sa propre transaction. Celui qui echoue est consigne avec
     * son numero · c'est par la qu'on le retrouve · et les suivants passent.</p>
     *
     * <p>Avant les echeances, les renouvellements dont la facture a ete soldee sont appliques : le
     * paiement les applique deja, ce passage rattrape celui qui aurait echoue.</p>
     */
    public int renewDueSubscriptions() {
        applySettledRenewals();
        List<Subscription> subscriptions = subscriptionRepository.findAllByStatusAndNextBillingDateLessThanEqualAndAutoRenewTrue(
                SubscriptionStatus.ACTIVE.name(),
                LocalDate.now()
        );
        int renewed = 0;
        int failed = 0;
        for (Subscription subscription : subscriptions) {
            try {
                self.renewOne(subscription.getId());
                renewed++;
            } catch (Exception ex) {
                failed++;
                log.error("Abonnement {} · renouvellement impossible, les suivants continuent",
                        subscription.getSubscriptionNumber(), ex);
            }
        }
        if (failed > 0) {
            log.warn("Echeances d'abonnement · {} facture(s), {} en echec a reprendre a la main", renewed, failed);
        }
        return renewed;
    }

    private void applySettledRenewals() {
        for (Subscription subscription : subscriptionRepository.findWithPendingRenewal()) {
            try {
                self.applySettledRenewal(subscription.getId());
            } catch (Exception ex) {
                log.error("Abonnement {} · renouvellement paye non applique, repris au prochain passage",
                        subscription.getSubscriptionNumber(), ex);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applySettledRenewal(Long subscriptionId) {
        subscriptionRepository.findById(subscriptionId)
                .ifPresent(subscription -> applyPaidRenewal(subscription, "Facture de renouvellement soldée"));
    }

    /**
     * La facture de renouvellement est soldee · la nouvelle periode prend effet, l'abonnement redevient
     * actif, ses droits s'ouvrent et son contrat est prolonge par avenant.
     *
     * <p>Paye a l'heure ou d'avance, la periode suit la precedente. Paye en retard, elle repart du jour
     * du reglement : l'abonne recoit la duree qu'il a payee.</p>
     *
     * @return le renouvellement a-t-il pris effet · non si rien n'attend ou si la facture reste due
     */
    @Transactional
    public boolean applyPaidRenewal(Subscription subscription, String reason) {
        String invoiceNumber = subscription.getPendingRenewalInvoice();
        if (invoiceNumber == null || !RENEWABLE_ON_PAYMENT.contains(subscription.getStatus())) {
            return false;
        }
        boolean settled = billingDocumentRepository.findByDocumentNumber(invoiceNumber)
                .filter(invoice -> invoice.getStatus() != BillingDocumentStatus.CANCELLED)
                .filter(invoice -> invoice.getBalanceDue() == null || invoice.getBalanceDue().signum() <= 0)
                .isPresent();
        if (!settled) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate start = today.isAfter(subscription.getPendingRenewalStart()) ? today : subscription.getPendingRenewalStart();
        startNewPeriod(subscription, start, reason);
        return true;
    }

    /**
     * Le renouvellement attend son reglement · l'abonne est prevenu qu'il doit payer, sauf si un
     * prelevement vient de lui dire, et une fois la periode finie l'abonnement n'est plus actif.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void awaitPayment(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null || subscription.getPendingRenewalInvoice() == null) {
            return;
        }
        if (applyPaidRenewal(subscription, "Facture de renouvellement soldée")) {
            return;
        }
        String invoiceNumber = subscription.getPendingRenewalInvoice();
        if (!directDebitService.attempted(subscription.getId(), invoiceNumber)) {
            notifyPaymentDue(subscription, invoiceNumber);
        }
        LocalDate periodEnd = subscription.getCurrentPeriodEnd();
        if (periodEnd == null || periodEnd.isBefore(LocalDate.now())) {
            graceService.enterQuietly(subscription, "Renouvellement en attente du paiement de la facture " + invoiceNumber);
        }
    }

    /**
     * L'abonnement prend fin sans que son renouvellement ait ete paye · la facture ne doit plus etre
     * reglee pour rien. Une facture deja en partie reglee reste, son sort se decide a la main.
     */
    public void dropPendingRenewal(Subscription subscription, String reason) {
        String invoiceNumber = subscription.getPendingRenewalInvoice();
        if (invoiceNumber == null) {
            return;
        }
        clearPendingRenewal(subscription);
        BillingDocument invoice = billingDocumentRepository.findByDocumentNumber(invoiceNumber).orElse(null);
        if (invoice == null) {
            return;
        }
        if (invoice.getPaidAmount() != null && invoice.getPaidAmount().signum() > 0) {
            log.warn("Abonnement {} · facture de renouvellement {} en partie reglee, laissee a traiter",
                    subscription.getSubscriptionNumber(), invoiceNumber);
            return;
        }
        billingDocumentService.cancelAndArchive(invoiceNumber, reason);
    }

    /**
     * Regle en retard · la periode repart du jour du paiement, pas de l'echeance manquee.
     *
     * <p>Le renouvellement avance la periode des qu'il facture : elle commence donc le lendemain
     * de la periode precedente, avant tout reglement. Quand le client paie dix jours plus tard, il
     * se retrouvait avec une periode commencee dix jours plus tot · il payait un mois et en
     * recevait vingt jours. Personne ne le lui avait annonce, et personne ne le voyait.</p>
     *
     * <p>La periode est donc reancree sur le jour du reglement. Le client recoit la duree qu'il a
     * payee, et l'echeance suivante se decale d'autant.</p>
     *
     * <p>Un reglement a l'heure ne change rien · seule une date de paiement posterieure au debut
     * de periode, c'est-a-dire a l'echeance, declenche le recalage.</p>
     */
    @Transactional
    public boolean realignOnLatePayment(Subscription subscription, LocalDate paidOn) {
        LocalDate periodStart = subscription.getCurrentPeriodStart();
        if (periodStart == null || paidOn == null || !paidOn.isAfter(periodStart)) {
            return false;
        }
        LocalDate newEnd = periodCalculator.periodEnd(paidOn, subscription.getBillingCycle());
        if (newEnd == null) {
            return false;
        }
        LocalDate previousEnd = subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(paidOn);
        subscription.setCurrentPeriodEnd(newEnd);
        subscription.setNextBillingDate(periodCalculator.nextBillingDate(paidOn, subscription.getBillingCycle()));
        // La periode repart · les avis de fin deja envoyes concernaient l'ancienne.
        subscription.setEndNoticeStage(null);
        subscriptionRepository.save(subscription);
        followOnContract(subscription, "Période recalée sur le paiement du " + paidOn);
        eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED,
                com.sni.bokaticowork.core.utils.json.JsonPayload.of("realignedOn", paidOn, "previousPeriodEnd", previousEnd, "newPeriodEnd", newEnd));
        log.info("Abonnement {} · regle le {}, periode recalee du {} au {}",
                subscription.getSubscriptionNumber(), paidOn, paidOn, newEnd);
        return true;
    }

    /** Un abonnement, sa propre transaction · relu dedans, pour ne pas ecrire depuis un detache. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renewOne(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return;
        }
        if (subscription.getPendingRenewalInvoice() != null) {
            // Renouvele par avance et toujours impaye : l'echeance est la, il n'est plus actif.
            awaitPayment(subscriptionId);
            return;
        }
        renewActive(subscription);
    }

    /** Meme regle que pour les echeances · un abonnement en erreur ne retient pas les autres. */
    public int cancelEndedSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAllByCancelAtPeriodEndTrueAndCurrentPeriodEndLessThanEqualAndStatus(
                LocalDate.now(),
                SubscriptionStatus.ACTIVE.name()
        );
        int cancelled = 0;
        for (Subscription subscription : subscriptions) {
            try {
                self.cancelAtPeriodEnd(subscription.getId());
                cancelled++;
            } catch (Exception ex) {
                log.error("Abonnement {} · cloture de fin de periode impossible, les suivants continuent",
                        subscription.getSubscriptionNumber(), ex);
            }
        }
        return cancelled;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelAtPeriodEnd(Long subscriptionId) {
        Subscription found = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (found == null || found.getStatus() != SubscriptionStatus.ACTIVE) {
            return;
        }
        List<Subscription> subscriptions = List.of(found);
        subscriptions.forEach(subscription -> {
            String reason = "Cancellation at period end";
            statusManager.changeStatus(subscription, SubscriptionStatus.CANCELLED, reason, "SYSTEM");
            subscription.setCancelledAt(Instant.now());
            subscription.setCancellationReason(reason);
            revokeActiveGrants(subscription);
            cancelAssociatedContract(subscription, reason);
            Subscription saved = subscriptionRepository.save(subscription);
            notifyInApp(saved, SubscriptionEventType.SUBSCRIPTION_CANCELLED);
            try {
                emailNotifier.notify(saved, SubscriptionEventType.SUBSCRIPTION_CANCELLED);
            } catch (Exception ex) {
                log.warn("Failed to send period-end cancellation email for subscription {} · continuing",
                        saved.getSubscriptionNumber(), ex);
            }
        });
    }

    public int repairActiveSubscriptionsWithoutGrants() {
        List<Subscription> subscriptions = subscriptionRepository.findActiveSubscriptionsWithoutGrants();
        subscriptions.forEach(subscription -> {
            entitlementService.grantForSubscription(subscription);
            eventWriter.writeEvent(subscription, SubscriptionEventType.ENTITLEMENTS_GRANTED, com.sni.bokaticowork.core.utils.json.JsonPayload.of("source", "INTEGRITY_WORKER"));
        });
        return subscriptions.size();
    }

    /**
     * Rattrape les contrats restes derriere leur abonnement · renouveles quand le contrat ne suivait
     * pas, ils ont expire alors que l'abonnement court. Un seul avenant du meme contrat couvre tout
     * l'ecart, jusqu'a la fin de la periode en cours. Aucun nouveau contrat n'est cree.
     */
    public int catchUpContractTerms() {
        reopenExpiredContracts();
        int caughtUp = 0;
        for (Long subscriptionId : subscriptionRepository.findIdsWithContractBehindPeriod()) {
            try {
                self.catchUpContractTerm(subscriptionId);
                caughtUp++;
            } catch (Exception ex) {
                log.error("Abonnement {} · contrat non rattrape, repris au prochain passage", subscriptionId, ex);
            }
        }
        return caughtUp;
    }

    /** Un contrat marque echu alors que l'abonnement court, y compris en attente d'un paiement. */
    private void reopenExpiredContracts() {
        for (String contractCode : subscriptionRepository.findExpiredContractCodesOfOpenSubscriptions()) {
            try {
                contractService.reopenExpired(contractCode, "Abonnement en cours");
            } catch (Exception ex) {
                log.error("Contrat {} · remise en vigueur impossible, reprise au prochain passage", contractCode, ex);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void catchUpContractTerm(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null) {
            return;
        }
        Contract contract = contractService.serviceByCode(subscription.getContractCode());
        LocalDate contractEnd = contract.getEndDate();
        if (contractEnd == null || !subscription.getCurrentPeriodEnd().isAfter(contractEnd)) {
            return;
        }
        requestRenewalAmendment(subscription, contract.getStartDate(), contractEnd, contractEnd.plusDays(1),
                subscription.getCurrentPeriodEnd(), subscription.getNextBillingDate());
    }

    private void requestRenewalAmendment(Subscription subscription, LocalDate previousStart, LocalDate previousEnd,
                                         LocalDate newStart, LocalDate newEnd, LocalDate nextBilling) {
        outboxService.publish(
                "CONTRACT_RENEWAL_AMENDMENT_REQUESTED",
                "SUBSCRIPTION",
                subscription.getSubscriptionNumber(),
                Map.of(
                        "contractCode", subscription.getContractCode(),
                        "subscriptionNumber", subscription.getSubscriptionNumber(),
                        "previousPeriodStart", previousStart != null ? previousStart.toString() : "",
                        "previousPeriodEnd",   previousEnd   != null ? previousEnd.toString()   : "",
                        "newPeriodStart",  newStart.toString(),
                        "newPeriodEnd",    newEnd.toString(),
                        "nextBillingDate", nextBilling != null ? nextBilling.toString() : ""
                )
        );
    }

    /**
     * Emet la facture de la periode a venir · rien d'autre ne bouge tant qu'elle n'est pas payee.
     *
     * <p>La periode, les droits et le contrat n'avancent qu'au reglement ({@link #applyPaidRenewal}).
     * Sans facture a regler, periode offerte ou avoir impute, le renouvellement prend effet aussitot.</p>
     */
    private void renewActive(Subscription subscription) {
        // Une derivation echue, ou qui a dit « retour au catalogue », rend l'abonnement au catalogue
        // ici · avant de chiffrer la periode, et sans que personne n'ait a s'en souvenir.
        derivationService.beforeRenewal(subscription);
        LocalDate previousPeriodEnd = subscription.getCurrentPeriodEnd();
        LocalDate newStart = previousPeriodEnd == null ? LocalDate.now() : previousPeriodEnd.plusDays(1);
        LocalDate newEnd   = periodCalculator.periodEnd(newStart, subscription.getBillingCycle());
        java.math.BigDecimal recurringAmount = subscription.getSubtotalAmount().add(subscription.getTaxAmount());
        var invoice = billingSupport.createBillableItem(subscription, "SUBSCRIPTION_RENEWAL", "Subscription renewal",
                recurringAmount, newStart, newEnd);
        if (invoice == null || invoice.balanceDue() == null || invoice.balanceDue().signum() <= 0) {
            startNewPeriod(subscription, newStart, "Renouvellement sans montant à régler");
            return;
        }
        subscription.setPendingRenewalInvoice(invoice.documentNumber());
        subscription.setPendingRenewalStart(newStart);
        subscription.setPendingRenewalEnd(newEnd);
        subscriptionRepository.save(subscription);
        // Avec un mandat, l'echeance est prelevee une fois la facture validee en base · jamais avant.
        // Le sort de l'abonnement se decide ensuite, selon ce que le prelevement a donne.
        directDebitService.collectAfterCommit(subscription.getId(), invoice.documentNumber());
        awaitPaymentAfterCommit(subscription.getId());
    }

    private void awaitPaymentAfterCommit(Long subscriptionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            self.awaitPayment(subscriptionId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    self.awaitPayment(subscriptionId);
                } catch (RuntimeException ex) {
                    log.error("Abonnement {} · attente du paiement non posee, reprise au prochain passage", subscriptionId, ex);
                }
            }
        });
    }

    /** La periode payee commence · statut, echeance, droits, contrat et messages suivent. */
    private void startNewPeriod(Subscription subscription, LocalDate start, String reason) {
        LocalDate previousPeriodStart = subscription.getCurrentPeriodStart();
        LocalDate previousPeriodEnd   = subscription.getCurrentPeriodEnd();
        LocalDate newEnd = periodCalculator.periodEnd(start, subscription.getBillingCycle());
        LocalDate nextBilling = periodCalculator.nextBillingDate(start, subscription.getBillingCycle());

        subscription.setCurrentPeriodStart(start);
        subscription.setCurrentPeriodEnd(newEnd);
        subscription.setNextBillingDate(nextBilling);
        // La periode repart · les avis de fin deja envoyes ne concernaient que l'ancienne.
        subscription.setEndNoticeStage(null);
        clearPendingRenewal(subscription);
        if (subscription.getStatus() == SubscriptionStatus.GRACE_PERIOD) {
            graceService.exit(subscription, reason);
        } else if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            statusManager.changeStatus(subscription, SubscriptionStatus.ACTIVE, reason, "SYSTEM");
            subscription.setSuspendedAt(null);
            subscription.setSuspensionReason(null);
        }
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.ACTIVE);
        subscriptionRepository.save(subscription);
        entitlementService.grantForSubscription(subscription);
        eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED, null);
        notifyInApp(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED);

        if (StringUtils.hasText(subscription.getContractCode())) {
            requestRenewalAmendment(subscription, previousPeriodStart, previousPeriodEnd, start, newEnd, nextBilling);
        }
        // Notification after all business logic and outbox events
        try {
            emailNotifier.notify(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED);
        } catch (Exception ex) {
            log.warn("Failed to send renewal email for subscription {} · continuing",
                    subscription.getSubscriptionNumber(), ex);
        }
        notifyWhatsApp(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED);
    }

    private static void clearPendingRenewal(Subscription subscription) {
        subscription.setPendingRenewalInvoice(null);
        subscription.setPendingRenewalStart(null);
        subscription.setPendingRenewalEnd(null);
    }

    /** La periode s'etend en cours de route · le contrat s'etend avec elle, sans avenant. */
    private void followOnContract(Subscription subscription, String reason) {
        if (StringUtils.hasText(subscription.getContractCode()) && subscription.getCurrentPeriodEnd() != null) {
            contractService.extendEndDate(subscription.getContractCode(), subscription.getCurrentPeriodEnd(), reason);
        }
    }

    private void notifyPaymentDue(Subscription subscription, String invoiceNumber) {
        String subject = "Réglez votre abonnement " + subscription.getSubscriptionNumber() + " pour le renouveler";
        Map<String, Object> details = new HashMap<>();
        details.put("subscriptionNumber", subscription.getSubscriptionNumber());
        details.put("planName", subscription.getPlanVersion() != null ? subscription.getPlanVersion().getName() : "");
        details.put("invoiceNumber", invoiceNumber);
        details.put("periodStart", String.valueOf(subscription.getPendingRenewalStart()));
        details.put("periodEnd", String.valueOf(subscription.getPendingRenewalEnd()));
        billingDocumentRepository.findByDocumentNumber(invoiceNumber).ifPresent(invoice -> {
            details.put("amount", String.valueOf(invoice.getBalanceDue().stripTrailingZeros().toPlainString()));
            details.put("currency", String.valueOf(invoice.getCurrency()));
        });
        String email = resolveRecipientEmail(subscription);
        memberInAppNotifier.notify(PAYMENT_DUE_EVENT, "SUBSCRIPTION", subscription.getSubscriptionNumber(), email,
                resolveRecipientName(subscription), subscription.getSubscriberCode(), subject, details);
        if (!StringUtils.hasText(email)) {
            return;
        }
        Map<String, Object> payload = new HashMap<>(details);
        // Rappel de paiement · ne se coupe jamais, mais part a l'adresse de facturation du membre.
        // La notification dans l'application garde l'adresse du compte, qui identifie le membre.
        payload.put("recipientEmail", billingRecipient(subscription, email));
        payload.put("subject", subject);
        payload.put("templateCode", PAYMENT_DUE_EVENT.toLowerCase(java.util.Locale.ROOT));
        outboxService.publish(PAYMENT_DUE_EVENT, "SUBSCRIPTION", subscription.getSubscriptionNumber(), payload);
    }

    public String reason(SubscriptionStatusChangeRequest request, String defaultReason) {
        return request != null && StringUtils.hasText(request.reason()) ? request.reason().trim() : defaultReason;
    }

    public String actor(SubscriptionStatusChangeRequest request) {
        return request != null && StringUtils.hasText(request.changedBy()) ? request.changedBy().trim() : "SYSTEM";
    }

    private LocalDate resolvePauseUntil(PauseSubscriptionRequest request) {
        if (request != null && request.resumeDate() != null) {
            return request.resumeDate();
        }
        int days = request == null || request.days() == null ? 1 : request.days();
        return LocalDate.now().plusDays(Math.max(1, days));
    }

    private String pauseReason(PauseSubscriptionRequest request, String defaultReason) {
        return request != null && StringUtils.hasText(request.reason()) ? request.reason().trim() : defaultReason;
    }

    private String pauseActor(PauseSubscriptionRequest request) {
        return request != null && StringUtils.hasText(request.changedBy()) ? request.changedBy().trim() : "SYSTEM";
    }

    /**
     * Retour arriere d'une activation obtenue par un paiement dont l'affectation est annulee (lot R4) :
     * droits retires, contrat annule (ou resilie s'il etait actif), abonnement remis en attente
     * d'activation. L'historique garde la trace. Les controles (droits deja utilises, renouvellement)
     * sont faits par l'appelant.
     */
    public void revertActivation(Subscription subscription, String reason) {
        revokeActiveGrants(subscription);
        cancelAssociatedContract(subscription, reason);
        statusManager.changeStatus(subscription, SubscriptionStatus.PENDING_ACTIVATION, reason, "SYSTEM");
        subscriptionRepository.save(subscription);
    }

    private void cancelAssociatedContract(Subscription subscription, String reason) {
        if (!StringUtils.hasText(subscription.getContractCode())) {
            return;
        }
        try {
            Contract contract = contractService.serviceByCode(subscription.getContractCode());
            ContractStatus status = contract.getStatus();
            if (status == ContractStatus.CANCELLED || status == ContractStatus.TERMINATED || status == ContractStatus.EXPIRED) {
                return;
            }
            String contractReason = StringUtils.hasText(reason) ? reason : "Subscription cancelled";
            if (status == ContractStatus.ACTIVE || status == ContractStatus.SUSPENDED || status == ContractStatus.AMENDED) {
                contractService.terminate(subscription.getContractCode(), contractReason);
                log.info("Contract {} terminated following subscription {} cancellation",
                        subscription.getContractCode(), subscription.getSubscriptionNumber());
            } else {
                contractService.cancel(subscription.getContractCode(), contractReason);
                log.info("Contract {} cancelled following subscription {} cancellation",
                        subscription.getContractCode(), subscription.getSubscriptionNumber());
            }
        } catch (Exception ex) {
            log.warn("Could not cancel contract {} for subscription {}: {}",
                    subscription.getContractCode(), subscription.getSubscriptionNumber(), ex.getMessage());
        }
    }

    /** Le niveau KYC exige par le plan est-il atteint · le drapeau est pose, l'avertissement publie. */
    private boolean kycCompliant(Subscription subscription) {
        try {
            if (subscription.getPlanVersion() == null || subscription.getPlanVersion().getPlan() == null) return true;
            SubscriptionPlan plan = subscription.getPlanVersion().getPlan();
            Integer requiredLevel = plan.getRequiredKycLevel();
            if (requiredLevel == null || requiredLevel <= 1) return true;

            DocumentOwnerType ownerType = mapSubscriberType(subscription.getSubscriberType());
            Long ownerId = resolveOwnerId(subscription);
            if (ownerType == null || ownerId == null) return true;

            int currentLevel = kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(ownerType, ownerId)
                    .filter(kycCase -> kycCase.getStatus() == KycCaseStatus.APPROVED)
                    .map(KycCase::getKycLevel)
                    .orElse(1);

            boolean compliant = currentLevel >= requiredLevel;
            subscription.setKycCompliant(compliant);
            subscriptionRepository.save(subscription);

            if (!compliant) {
                log.warn("Subscription {} waits for documents (has KYC level {}, required {})",
                        subscription.getSubscriptionNumber(), currentLevel, requiredLevel);
                Map<String, Object> payload = new HashMap<>();
                payload.put("subscriptionNumber", subscription.getSubscriptionNumber());
                payload.put("subscriberType", subscription.getSubscriberType().name());
                payload.put("subscriberCode", subscription.getSubscriberCode());
                payload.put("currentKycLevel", currentLevel);
                payload.put("requiredKycLevel", requiredLevel);
                outboxService.publish("KYC_COMPLIANCE_WARNING", "NOTIFICATION",
                        subscription.getSubscriptionNumber(), payload);
            }
            return compliant;
        } catch (Exception ex) {
            log.warn("KYC compliance check failed for subscription {}: {}",
                    subscription.getSubscriptionNumber(), ex.getMessage());
            return true;
        }
    }

    private DocumentOwnerType mapSubscriberType(SubscriberType type) {
        return switch (type) {
            case MEMBER -> DocumentOwnerType.MEMBER;
            case CUSTOMER -> DocumentOwnerType.CUSTOMER;
            case BUSINESS_ENTITY -> DocumentOwnerType.BUSINESS;
        };
    }

    private Long resolveOwnerId(Subscription subscription) {
        if (subscription.getMember() != null) return subscription.getMember().getId();
        if (subscription.getCustomer() != null) return subscription.getCustomer().getId();
        if (subscription.getBusinessEntity() != null) return subscription.getBusinessEntity().getId();
        return null;
    }

    private void revokeActiveGrants(Subscription subscription) {
        if (subscription.getId() == null) {
            return;
        }
        try {
            List<EntitlementGrant> toRevoke = entitlementGrantRepository
                    .findAllBySubscription(subscription.getId())
                    .stream()
                    .filter(g -> g.getStatus() == EntitlementGrantStatus.ACTIVE)
                    .toList();
            if (toRevoke.isEmpty()) {
                return;
            }
            Instant now = Instant.now();
            toRevoke.forEach(g -> {
                g.setStatus(EntitlementGrantStatus.CANCELLED);
                g.setValidUntil(now);
            });
            entitlementGrantRepository.saveAll(toRevoke);
            log.info("Revoked {} entitlement grant(s) for subscription {}",
                    toRevoke.size(), subscription.getSubscriptionNumber());
        } catch (Exception ex) {
            log.warn("Could not revoke entitlement grants for subscription {}: {}",
                    subscription.getSubscriptionNumber(), ex.getMessage());
        }
    }

    private void notifyInApp(Subscription subscription, SubscriptionEventType eventType) {
        String email = resolveRecipientEmail(subscription);
        String name = resolveRecipientName(subscription);
        String subject = switch (eventType) {
            case SUBSCRIPTION_ACTIVATED -> "Abonnement active " + subscription.getSubscriptionNumber();
            case SUBSCRIPTION_CANCELLED -> "Abonnement annule " + subscription.getSubscriptionNumber();
            case SUBSCRIPTION_RENEWED -> "Abonnement renouvele " + subscription.getSubscriptionNumber();
            case SUBSCRIPTION_EXPIRED -> "Abonnement expire " + subscription.getSubscriptionNumber();
            default -> "Abonnement mis a jour " + subscription.getSubscriptionNumber();
        };
        memberInAppNotifier.notify(
                eventType.name(), "SUBSCRIPTION", subscription.getSubscriptionNumber(),
                email, name, subscription.getSubscriberCode(), subject,
                Map.of(
                        "subscriptionNumber", subscription.getSubscriptionNumber(),
                        "status", subscription.getStatus() != null ? subscription.getStatus().name() : "",
                        "planName", subscription.getPlanVersion() != null ? subscription.getPlanVersion().getName() : ""
                )
        );
    }

    private String resolveRecipientEmail(Subscription subscription) {
        if (subscription.getMember() != null) return subscription.getMember().getEmail();
        if (subscription.getCustomer() != null) {
            return StringUtils.hasText(subscription.getCustomer().getBillingEmail())
                    ? subscription.getCustomer().getBillingEmail() : subscription.getCustomer().getEmail();
        }
        if (subscription.getBusinessEntity() != null) return subscription.getBusinessEntity().getEmail();
        return null;
    }

    private String billingRecipient(Subscription subscription, String email) {
        if (memberContact == null) {
            return email;
        }
        if (subscription.getMember() != null) {
            return memberContact.recipient(subscription.getMember(), email);
        }
        if (subscription.getCustomer() != null) {
            return memberContact.memberOf(subscription.getCustomer())
                    .map(member -> memberContact.recipient(member, email))
                    .orElse(email);
        }
        return email;
    }

    private String resolveRecipientName(Subscription subscription) {
        if (subscription.getMember() != null) return subscription.getMember().getDisplayName();
        if (subscription.getCustomer() != null) return subscription.getCustomer().getFirstname();
        if (subscription.getBusinessEntity() != null) return subscription.getBusinessEntity().getName();
        return subscription.getSubscriberCode();
    }
}
