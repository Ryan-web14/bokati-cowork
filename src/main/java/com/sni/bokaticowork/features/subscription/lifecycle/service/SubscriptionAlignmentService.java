package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * L'alignement des echeances · une entreprise, une date, une facture.
 *
 * <p>Les abonnements d'un meme souscripteur convergent vers une meme fin de periode. On n'aligne
 * que vers l'avant : la periode de raccordement (entre la fin actuelle et la date commune) est
 * facturee au prorata, en une fois. Aligner vers l'arriere demanderait de rendre de l'argent · si
 * une date plus tot est demandee, les abonnements qu'elle raccourcirait sont laisses tels quels et
 * dits.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionAlignmentService {

    static final String BRIDGING_SOURCE = "ALIGNMENT_BRIDGING";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPolicyService policyService;
    private final ProrationCalculator proration;
    private final SubscriptionBillingSupport billingSupport;
    private final SubscriptionEventWriter eventWriter;

    public record Line(String subscriptionNumber, LocalDate currentPeriodEnd, LocalDate newPeriodEnd, int bridgingDays,
                       BigDecimal bridgingAmount, boolean aligned, String message) {
    }

    public record Outcome(SubscriberType subscriberType, String subscriberCode, LocalDate targetPeriodEnd, int total, int aligned,
                          int skipped, BigDecimal totalBridging, List<Line> lines) {
    }

    /**
     * @param targetPeriodEnd la fin de periode commune · nulle pour prendre la plus tardive des fins actuelles
     */
    @Transactional
    public Outcome align(SubscriberType subscriberType, String subscriberCode, LocalDate targetPeriodEnd, boolean simulateOnly, String actor) {
        List<Subscription> subscriptions = subscriptionRepository.findOpenBySubscriber(subscriberType.name(), subscriberCode).stream()
                .filter(s -> s.getCurrentPeriodEnd() != null && s.getCurrentPeriodStart() != null)
                .filter(s -> s.getBillingCycle() != BillingCycle.ONE_TIME && s.getBillingCycle() != BillingCycle.DAILY)
                .toList();
        if (subscriptions.size() < 2) {
            throw new BadRequestException("Il faut au moins deux abonnements ouverts pour aligner leurs échéances");
        }
        LocalDate target = targetPeriodEnd != null ? targetPeriodEnd
                : subscriptions.stream().map(Subscription::getCurrentPeriodEnd).max(Comparator.naturalOrder()).orElseThrow();
        if (!target.isAfter(LocalDate.now())) {
            throw new BadRequestException("La date commune est dans le futur");
        }
        ProrationPolicy policy = policyService.current().getProrationPolicy();
        List<Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int aligned = 0;
        int skipped = 0;

        for (Subscription subscription : subscriptions) {
            LocalDate end = subscription.getCurrentPeriodEnd();
            if (end.isEqual(target)) {
                lines.add(new Line(subscription.getSubscriptionNumber(), end, end, 0, BigDecimal.ZERO, true, "déjà alignée"));
                aligned++;
                continue;
            }
            if (end.isAfter(target)) {
                lines.add(new Line(subscription.getSubscriptionNumber(), end, end, 0, BigDecimal.ZERO, false,
                        "la période en cours va au-delà de la date commune · un alignement demanderait de rendre de l'argent"));
                skipped++;
                continue;
            }
            int days = (int) ChronoUnit.DAYS.between(end, target);
            BigDecimal bridging = bridging(subscription, end.plusDays(1), target, policy);
            if (!simulateOnly) {
                subscription.setCurrentPeriodEnd(target);
                subscription.setNextBillingDate(target.plusDays(1));
                subscriptionRepository.save(subscription);
                billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.ACTIVE);
                if (bridging.signum() > 0) {
                    billingSupport.createStandaloneBillableItem(subscription, BRIDGING_SOURCE,
                            "Raccordement des échéances · " + days + " jour(s) jusqu'au " + target, bridging, end.plusDays(1), target);
                }
                eventWriter.writeEvent(subscription, SubscriptionEventType.DATES_ALIGNED,
                        "{\"from\":\"" + end + "\",\"to\":\"" + target + "\",\"bridging\":" + bridging.toPlainString() + ",\"by\":\"" + actor + "\"}");
            }
            lines.add(new Line(subscription.getSubscriptionNumber(), end, target, days, bridging, true, simulateOnly ? "simulation" : "alignée"));
            total = total.add(bridging);
            aligned++;
        }
        log.info("Alignement {} {} · cible {} · {} alignee(s), {} laissee(s), raccordement {}", subscriberType, subscriberCode, target,
                aligned, skipped, total);
        return new Outcome(subscriberType, subscriberCode, target, subscriptions.size(), aligned, skipped, total, lines);
    }

    /** Les jours de raccordement au tarif de la periode, periode virtuelle par periode virtuelle. */
    private BigDecimal bridging(Subscription subscription, LocalDate from, LocalDate to, ProrationPolicy policy) {
        long periodDays = ChronoUnit.DAYS.between(subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd()) + 1;
        BigDecimal total = BigDecimal.ZERO;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            LocalDate virtualEnd = cursor.plusDays(periodDays - 1);
            LocalDate end = virtualEnd.isAfter(to) ? to : virtualEnd;
            total = total.add(proration.share(subscription.getTotalAmount(), cursor, virtualEnd, cursor, end,
                    policy == ProrationPolicy.NONE ? ProrationPolicy.DAILY : policy));
            cursor = virtualEnd.plusDays(1);
        }
        return total;
    }
}
