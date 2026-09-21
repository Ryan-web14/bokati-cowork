package com.sni.bokaticowork.features.portal.subscription.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.subscription.change.dto.CreateSubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.dto.SubscriptionChangeResponse;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeEffectivePolicy;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;
import com.sni.bokaticowork.features.subscription.change.service.SubscriptionChangeRequestService;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionDebitMandate;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import com.sni.bokaticowork.features.subscription.lifecycle.service.*;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PauseSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Le portail en ecriture · changer, geler, resilier, prelever, accepter un devis, sans guichet.
 *
 * <p>Chaque action passe par les memes services que le guichet · memes politiques, memes bornes,
 * memes traces. Ce qui change : l'acteur est le membre, le canal est PORTAL, et ce que le membre
 * peut faire seul s'arrete la ou une decision humaine est requise (dispense de frais, plancher,
 * visa). Il ne voit que ce qui est a lui.</p>
 */
@Service
@RequiredArgsConstructor
public class ClientSubscriptionSelfService {

    private final SubscriptionService subscriptionService;
    private final SubscriptionChangeRequestService changeService;
    private final SubscriptionPlanResolver planResolver;
    private final PlanPriceRepository planPriceRepository;
    private final PlanChangeProrationService prorationService;
    private final SubscriptionFreezeService freezeService;
    private final SubscriptionPolicyService policyService;
    private final SubscriptionTerminationService terminationService;
    private final SubscriptionCommitmentService commitmentService;
    private final SubscriptionDirectDebitService directDebitService;
    private final SubscriptionQuoteService quoteService;

    // ---- Changer de plan ------------------------------------------------------------------------

    public record PlanChangePreview(String targetPlanCode, String targetPlanName, BigDecimal targetPeriodTotal, BigDecimal currentPeriodTotal,
                                    BigDecimal amount, boolean credit, String prorationPolicy, LocalDate effectiveDate, boolean upgrade) {
    }

    @Transactional(readOnly = true)
    public PlanChangePreview previewPlanChange(Member member, String subscriptionNumber, String targetPlanCode, boolean atNextPeriod) {
        Subscription subscription = own(member, subscriptionNumber);
        PlanVersion target = planResolver.resolvePlanVersion(targetPlanCode, null);
        if (target.getId().equals(subscription.getPlanVersion().getId())) {
            throw new BadRequestException("Vous êtes déjà sur ce plan");
        }
        LocalDate effective = atNextPeriod ? subscription.getNextBillingDate() : LocalDate.now();
        PlanChangeProrationService.Preview p = prorationService.preview(subscription, target, effective);
        boolean upgrade = p.targetPeriodTotal().compareTo(p.currentPeriodTotal()) > 0;
        return new PlanChangePreview(target.getPlan().getCode(), target.getName(), p.targetPeriodTotal(), p.currentPeriodTotal(),
                atNextPeriod ? BigDecimal.ZERO : p.amount(), !atNextPeriod && p.credit(), p.policy().name(), effective, upgrade);
    }

    /**
     * Demande et applique · tout de suite, ou a la prochaine echeance.
     *
     * <p>Un changement de catalogue a catalogue n'a pas besoin de visa : la confirmation du membre
     * vaut approbation. Immediat, il est applique ici ; planifie, le worker l'appliquera a la date.</p>
     */
    @Transactional
    public SubscriptionChangeResponse changePlan(Member member, String subscriptionNumber, String targetPlanCode, boolean atNextPeriod, String reason) {
        Subscription subscription = own(member, subscriptionNumber);
        if (!subscription.getStatus().entitled() || subscription.getStatus() == com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus.PENDING_TERMINATION) {
            throw new ConflictException("subscription", "un abonnement " + subscription.getStatus() + " ne change pas de plan");
        }
        PlanVersion target = planResolver.resolvePlanVersion(targetPlanCode, null);
        if (target.privateToSubscription()) {
            throw new BadRequestException("Ce plan n'est pas au catalogue");
        }
        PlanPrice price = planPriceRepository.findAllByPlanVersion(target.getId()).stream()
                .filter(p -> p.getBillingCycle() == subscription.getBillingCycle()).findFirst()
                .orElseThrow(() -> new ConflictException("subscription", "ce plan n'existe pas au rythme " + subscription.getBillingCycle()));
        boolean upgrade = price.getAmount().compareTo(subscription.getSubtotalAmount()) > 0;
        SubscriptionChangeResponse change = changeService.request(subscriptionNumber, new CreateSubscriptionChangeRequest(
                upgrade ? SubscriptionChangeType.UPGRADE : SubscriptionChangeType.DOWNGRADE, target.getId(),
                atNextPeriod ? SubscriptionChangeEffectivePolicy.NEXT_BILLING_PERIOD : SubscriptionChangeEffectivePolicy.IMMEDIATE,
                null, null, reason == null ? "Changement depuis l'espace client" : reason, member.getMemberId()));
        SubscriptionChangeResponse approved = changeService.approve(change.changeNumber(), member.getMemberId());
        return atNextPeriod ? approved : changeService.apply(change.changeNumber());
    }

    // ---- Geler -------------------------------------------------------------------------------------

    public record FreezeTerms(int freezesUsedThisYear, int freezesAllowedPerYear, int maxDays, BigDecimal feePercent, int noticeDays, boolean canFreezeNow) {
    }

    @Transactional(readOnly = true)
    public FreezeTerms freezeTerms(Member member, String subscriptionNumber) {
        Subscription subscription = own(member, subscriptionNumber);
        SubscriptionFreezeService.Allowance a = freezeService.allowance(subscription);
        SubscriptionPolicy policy = policyService.current();
        return new FreezeTerms(a.freezesUsedThisYear(), a.freezesAllowedPerYear(), a.maxDays(), a.feePercent(), policy.getFreezeNoticeDays(),
                a.canFreeze() && policy.getFreezeNoticeDays() == 0);
    }

    /** Le gel depuis le portail est immediat · quand la politique impose un preavis, il passe par le support. */
    @Transactional
    public SubscriptionResponse freeze(Member member, String subscriptionNumber, LocalDate until, String reason) {
        own(member, subscriptionNumber);
        int notice = policyService.current().getFreezeNoticeDays();
        if (notice > 0) {
            throw new ConflictException("freeze", "un gel se demande " + notice + " jour(s) à l'avance · contactez le support");
        }
        return subscriptionService.pause(subscriptionNumber, new PauseSubscriptionRequest(null, until, reason, member.getMemberId()));
    }

    @Transactional
    public SubscriptionResponse resume(Member member, String subscriptionNumber) {
        own(member, subscriptionNumber);
        return subscriptionService.resume(subscriptionNumber, new SubscriptionStatusChangeRequest("Reprise depuis l'espace client", member.getMemberId(), false));
    }

    // ---- Resilier ---------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SubscriptionTerminationService.Preview previewTermination(Member member, String subscriptionNumber,
                                                                     SubscriptionTermination.ReasonCategory category, LocalDate wanted) {
        own(member, subscriptionNumber);
        return terminationService.preview(subscriptionNumber, new SubscriptionTerminationService.Request(
                category == null ? SubscriptionTermination.ReasonCategory.OTHER : category, null, SubscriptionTermination.Channel.PORTAL, wanted, null));
    }

    @Transactional
    public SubscriptionTermination requestTermination(Member member, String subscriptionNumber, SubscriptionTermination.ReasonCategory category,
                                                      String reason, LocalDate wanted) {
        own(member, subscriptionNumber);
        return terminationService.request(subscriptionNumber, new SubscriptionTerminationService.Request(
                category == null ? SubscriptionTermination.ReasonCategory.OTHER : category, reason, SubscriptionTermination.Channel.PORTAL, wanted, null),
                member.getMemberId());
    }

    @Transactional
    public SubscriptionTermination retractTermination(Member member, String terminationCode, String reason) {
        SubscriptionTermination termination = terminationService.get(terminationCode);
        own(member, termination.getSubscription().getSubscriptionNumber());
        return terminationService.retract(terminationCode, reason, member.getMemberId());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionTermination> terminations(Member member, String subscriptionNumber) {
        own(member, subscriptionNumber);
        return terminationService.ofSubscription(subscriptionNumber);
    }

    @Transactional(readOnly = true)
    public SubscriptionCommitmentService.EarlyTermination earlyTermination(Member member, String subscriptionNumber, LocalDate on) {
        Subscription subscription = own(member, subscriptionNumber);
        return commitmentService.earlyTerminationOn(subscription, on == null ? LocalDate.now() : on);
    }

    // ---- Prelevement -----------------------------------------------------------------------------

    @Transactional
    public SubscriptionDebitMandate giveMandate(Member member, String subscriptionNumber, String walletNumber, BigDecimal maxAmountPerDebit) {
        own(member, subscriptionNumber);
        return directDebitService.give(subscriptionNumber,
                new SubscriptionDirectDebitService.Consent(walletNumber, SubscriptionDebitMandate.Channel.PORTAL, "portal:" + member.getMemberId(), maxAmountPerDebit),
                member.getMemberId());
    }

    @Transactional
    public SubscriptionDebitMandate revokeMandate(Member member, String subscriptionNumber) {
        Subscription subscription = own(member, subscriptionNumber);
        SubscriptionDebitMandate current = directDebitService.currentOf(subscription)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun mandat de prélèvement sur cet abonnement"));
        return directDebitService.revoke(current.getMandateCode(), "Révoqué depuis l'espace client", member.getMemberId());
    }

    @Transactional(readOnly = true)
    public SubscriptionDebitMandate mandate(Member member, String subscriptionNumber) {
        Subscription subscription = own(member, subscriptionNumber);
        return directDebitService.currentOf(subscription)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun mandat de prélèvement sur cet abonnement"));
    }

    // ---- Devis ------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SubscriptionQuote> quotes(Member member) {
        return quoteService.ofSubscriber(SubscriberType.MEMBER, member.getMemberId());
    }

    @Transactional(readOnly = true)
    public SubscriptionQuote quote(Member member, String quoteNumber) {
        SubscriptionQuote quote = quoteService.get(quoteNumber);
        if (quote.getSubscriberType() != SubscriberType.MEMBER || !member.getMemberId().equals(quote.getSubscriberCode())) {
            throw new ResourceNotFoundException("Devis introuvable");
        }
        return quote;
    }

    @Transactional
    public SubscriptionQuote acceptQuote(Member member, String quoteNumber) {
        quote(member, quoteNumber);
        return quoteService.accept(quoteNumber, member.getMemberId());
    }

    @Transactional
    public SubscriptionQuote rejectQuote(Member member, String quoteNumber, String reason) {
        quote(member, quoteNumber);
        return quoteService.reject(quoteNumber, reason);
    }

    // ---- Interne ----------------------------------------------------------------------------------

    /** L'abonnement s'il est au membre · sinon il n'existe pas. */
    Subscription own(Member member, String subscriptionNumber) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        if (subscription.getSubscriberType() != SubscriberType.MEMBER || !member.getMemberId().equals(subscription.getSubscriberCode())) {
            throw new ResourceNotFoundException("Subscription not found");
        }
        return subscription;
    }
}
