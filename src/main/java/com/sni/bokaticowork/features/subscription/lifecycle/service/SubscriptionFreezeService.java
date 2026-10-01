package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionFreeze;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionFreezeRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Le gel encadre · combien de fois, combien de temps, et ce qui est facture pendant.
 *
 * <p>Le gel existait ; ses bornes non. Ici chaque gel est compte sur douze mois glissants, borne en
 * jours, et facture selon la part que la politique retient. Un gel sans bornes est un moyen de ne
 * plus payer sans resilier.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionFreezeService {

    static final String FEE_SOURCE = "FREEZE_FEE";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SubscriptionFreezeRepository freezeRepository;
    private final SubscriptionPolicyService policyService;
    private final SubscriptionBillingSupport billingSupport;
    private final SubscriptionEventWriter eventWriter;

    /** Ce que la politique permet encore a cet abonnement. */
    public record Allowance(int freezesUsedThisYear, int freezesAllowedPerYear, int maxDays, BigDecimal feePercent, boolean canFreeze) {
    }

    @Transactional(readOnly = true)
    public Allowance allowance(Subscription subscription) {
        SubscriptionPolicy policy = policyService.current();
        int used = freezeRepository.findBySubscriptionSince(subscription.getId(), LocalDate.now().minusYears(1)).size();
        return new Allowance(used, policy.getFreezeMaxPerYear(), policy.getFreezeMaxDays(), policy.getFreezeFeePercent(),
                used < policy.getFreezeMaxPerYear());
    }

    /**
     * Ouvre un gel · verifie les bornes, ecrit le gel, facture la part retenue.
     *
     * @throws ConflictException si le quota annuel est atteint ou la duree depasse le maximum
     */
    @Transactional
    public SubscriptionFreeze start(Subscription subscription, LocalDate until, String reason, String actor) {
        SubscriptionPolicy policy = policyService.current();
        LocalDate today = LocalDate.now();
        if (until == null || !until.isAfter(today)) {
            throw new BadRequestException("La date de reprise est après aujourd'hui");
        }
        int days = (int) ChronoUnit.DAYS.between(today, until);
        if (days > policy.getFreezeMaxDays()) {
            throw new ConflictException("freeze", "un gel ne dépasse pas " + policy.getFreezeMaxDays() + " jours · demandé " + days);
        }
        List<SubscriptionFreeze> lastYear = freezeRepository.findBySubscriptionSince(subscription.getId(), today.minusYears(1));
        if (lastYear.size() >= policy.getFreezeMaxPerYear()) {
            throw new ConflictException("freeze", "le quota de " + policy.getFreezeMaxPerYear() + " gel(s) sur douze mois est atteint");
        }
        freezeRepository.findOpenBySubscription(subscription.getId()).ifPresent(open -> {
            throw new ConflictException("freeze", "un gel est déjà ouvert depuis le " + open.getStartedOn());
        });

        BigDecimal fee = feeFor(subscription, today, until.minusDays(1), policy);
        SubscriptionFreeze freeze = SubscriptionFreeze.builder()
                .subscription(subscription).startedOn(today).plannedUntil(until).daysPlanned(days)
                .feeAmount(fee).reason(StringUtils.hasText(reason) ? reason.trim() : null).requestedBy(actor).build();
        if (fee.signum() > 0) {
            freeze.setFeeBillableNumber(billingSupport.createStandaloneBillableItem(subscription, FEE_SOURCE,
                    "Gel de l'abonnement du " + today + " au " + until + " · " + plain(policy.getFreezeFeePercent()) + " % du tarif",
                    fee, today, until.minusDays(1)));
        }
        SubscriptionFreeze saved = freezeRepository.save(freeze);
        eventWriter.writeEvent(subscription, SubscriptionEventType.FREEZE_STARTED,
                "{\"until\":\"" + until + "\",\"days\":" + days + ",\"fee\":" + fee.toPlainString() + ",\"usedThisYear\":" + (lastYear.size() + 1) + "}");
        log.info("Abonnement {} · gel {} jours jusqu'au {} (frais {})", subscription.getSubscriptionNumber(), days, until, fee);
        return saved;
    }

    /** Ferme le gel ouvert · a la reprise, prevue ou anticipee. */
    @Transactional
    public void end(Subscription subscription, LocalDate resumedOn) {
        freezeRepository.findOpenBySubscription(subscription.getId()).ifPresent(freeze -> {
            freeze.setResumedOn(resumedOn);
            freeze.setDaysEffective((int) Math.max(0, ChronoUnit.DAYS.between(freeze.getStartedOn(), resumedOn)));
            freezeRepository.save(freeze);
        });
    }

    @Transactional(readOnly = true)
    public List<SubscriptionFreeze> history(Subscription subscription) {
        return freezeRepository.findBySubscription_IdOrderByStartedOnDesc(subscription.getId());
    }

    /** La part du tarif retenue pendant les jours geles · au prorata de la periode, puis au pourcentage. */
    BigDecimal feeFor(Subscription subscription, LocalDate from, LocalDate to, SubscriptionPolicy policy) {
        if (policy.getFreezeFeePercent() == null || policy.getFreezeFeePercent().signum() == 0
                || subscription.getCurrentPeriodStart() == null || subscription.getCurrentPeriodEnd() == null) {
            return BigDecimal.ZERO;
        }
        // Les jours geles peuvent depasser la periode courante · on compte au tarif de la periode, jour par jour
        long periodDays = ChronoUnit.DAYS.between(subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd()) + 1;
        long frozenDays = ChronoUnit.DAYS.between(from, to) + 1;
        BigDecimal daily = subscription.getTotalAmount().divide(BigDecimal.valueOf(periodDays), 8, RoundingMode.HALF_UP);
        return daily.multiply(BigDecimal.valueOf(frozenDays)).multiply(policy.getFreezeFeePercent())
                .divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
