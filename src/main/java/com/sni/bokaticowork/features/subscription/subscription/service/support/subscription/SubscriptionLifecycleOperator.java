package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
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
    private final BillingDocumentRepository billingDocumentRepository;
    private final KycCaseRepository kycCaseRepository;
    private final OutboxService outboxService;
    private final @Lazy com.sni.bokaticowork.features.subscription.derivation.service.PlanDerivationService derivationService;
    private final @Lazy com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionFreezeService freezeService;
    private final @Lazy com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionDirectDebitService directDebitService;
    /** Soi-meme, pour que chaque abonnement d'un balayage obtienne sa propre transaction. */
    private final @Lazy SubscriptionLifecycleOperator self;


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

    private void releaseDepositHold(Subscription subscription) {
        List<WalletHold> activeHolds = walletHoldRepository.findAllByStatusAndSourceTypeAndSourceCode(
                "ACTIVE", "SUBSCRIPTION", subscription.getSubscriptionNumber());
        if (activeHolds.isEmpty()) {
            return;
        }
        boolean hasUnpaidInvoices = !billingDocumentRepository.findRecoverableDocuments(
                subscription.getSubscriberType().name(), subscription.getSubscriberCode()).isEmpty();
        for (WalletHold hold : activeHolds) {
            try {
                if (hasUnpaidInvoices) {
                    walletHoldService.capture(hold.getHoldNumber(), "SYSTEM");
                    eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_CANCELLED,
                            "Deposit hold " + hold.getHoldNumber() + " captured to cover outstanding invoices on cancellation");
                } else {
                    walletHoldService.release(hold.getHoldNumber(), "SYSTEM");
                }
            } catch (Exception ex) {
                log.warn("Failed to release/capture deposit hold {} for subscription {}", hold.getHoldNumber(), subscription.getSubscriptionNumber(), ex);
            }
        }
    }

    public void renew(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ConflictException("subscription", "only ACTIVE subscriptions can be renewed");
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
     */
    public int renewDueSubscriptions() {
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

    /** Un abonnement, sa propre transaction · relu dedans, pour ne pas ecrire depuis un detache. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renewOne(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) {
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
            eventWriter.writeEvent(subscription, SubscriptionEventType.ENTITLEMENTS_GRANTED, "{\"source\":\"INTEGRITY_WORKER\"}");
        });
        return subscriptions.size();
    }

    private void renewActive(Subscription subscription) {
        // Une derivation echue, ou qui a dit « retour au catalogue », rend l'abonnement au catalogue
        // ici · avant de chiffrer la periode, et sans que personne n'ait a s'en souvenir.
        derivationService.beforeRenewal(subscription);
        LocalDate previousPeriodStart = subscription.getCurrentPeriodStart();
        LocalDate previousPeriodEnd   = subscription.getCurrentPeriodEnd();

        LocalDate newStart = previousPeriodEnd == null ? LocalDate.now() : previousPeriodEnd.plusDays(1);
        LocalDate newEnd   = periodCalculator.periodEnd(newStart, subscription.getBillingCycle());
        LocalDate nextBilling = periodCalculator.nextBillingDate(newStart, subscription.getBillingCycle());

        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(newEnd);
        subscription.setNextBillingDate(nextBilling);
        java.math.BigDecimal recurringAmount = subscription.getSubtotalAmount().add(subscription.getTaxAmount());
        var invoice = billingSupport.createBillableItem(subscription, "SUBSCRIPTION_RENEWAL", "Subscription renewal", recurringAmount);
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.ACTIVE);
        if (invoice != null) {
            // Avec un mandat, l'echeance est prelevee une fois la facture validee en base · jamais avant
            directDebitService.collectAfterCommit(subscription.getId(), invoice.documentNumber());
        }
        subscriptionRepository.save(subscription);
        entitlementService.grantForSubscription(subscription);
        eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED, null);
        notifyInApp(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED);

        if (StringUtils.hasText(subscription.getContractCode())) {
            outboxService.publish(
                    "CONTRACT_RENEWAL_AMENDMENT_REQUESTED",
                    "SUBSCRIPTION",
                    subscription.getSubscriptionNumber(),
                    Map.of(
                            "contractCode", subscription.getContractCode(),
                            "subscriptionNumber", subscription.getSubscriptionNumber(),
                            "previousPeriodStart", previousPeriodStart != null ? previousPeriodStart.toString() : "",
                            "previousPeriodEnd",   previousPeriodEnd   != null ? previousPeriodEnd.toString()   : "",
                            "newPeriodStart",  newStart.toString(),
                            "newPeriodEnd",    newEnd.toString(),
                            "nextBillingDate", nextBilling.toString()
                    )
            );
        }
        // Notification after all business logic and outbox events
        try {
            emailNotifier.notify(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED);
        } catch (Exception ex) {
            log.warn("Failed to send renewal email for subscription {} · continuing",
                    subscription.getSubscriptionNumber(), ex);
        }
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
            if (status == ContractStatus.ACTIVE || status == ContractStatus.SUSPENDED) {
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

    private String resolveRecipientName(Subscription subscription) {
        if (subscription.getMember() != null) return subscription.getMember().getDisplayName();
        if (subscription.getCustomer() != null) return subscription.getCustomer().getFirstname();
        if (subscription.getBusinessEntity() != null) return subscription.getBusinessEntity().getName();
        return subscription.getSubscriberCode();
    }
}
