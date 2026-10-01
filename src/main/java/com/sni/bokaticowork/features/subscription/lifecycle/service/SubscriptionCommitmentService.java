package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.lifecycle.model.EarlyTerminationFormula;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionCommitment;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionCommitmentRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * L'engagement · pose, lu, chiffre.
 *
 * <p>Il ne dit jamais « on ne peut pas resilier ». Il dit combien coute de le faire avant le terme,
 * selon une formule qui est une donnee de la politique ou de l'engagement lui-meme.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionCommitmentService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SubscriptionCommitmentRepository commitmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPolicyService policyService;
    private final SubscriptionEventWriter eventWriter;

    public record Spec(Integer commitmentMonths, LocalDate commitmentStart, EarlyTerminationFormula formula,
                       BigDecimal percent, BigDecimal fixedFee, Boolean autoRenewCommitment) {
    }

    /** Ce que couterait une rupture a une date donnee · zero hors engagement. */
    public record EarlyTermination(boolean early, int remainingMonths, BigDecimal fee, EarlyTerminationFormula formula, LocalDate commitmentEnd) {
        public static EarlyTermination none() {
            return new EarlyTermination(false, 0, BigDecimal.ZERO, EarlyTerminationFormula.NONE, null);
        }
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionCommitment> of(Subscription subscription) {
        return commitmentRepository.findBySubscription_Id(subscription.getId());
    }

    @Transactional(readOnly = true)
    public SubscriptionCommitment get(String subscriptionNumber) {
        return of(subscription(subscriptionNumber))
                .orElseThrow(() -> new ResourceNotFoundException("Cet abonnement n'a pas d'engagement"));
    }

    /**
     * Pose l'engagement · a la souscription depuis le prix de plan, par un devis, ou a la main.
     *
     * <p>Un engagement existant est remplace, pas cumule : le nouveau dit ce qui vaut desormais.
     * Sans formule, celle de la politique s'applique.</p>
     */
    @Transactional
    public SubscriptionCommitment set(Subscription subscription, Spec spec, SubscriptionCommitment.Source source, String actor) {
        if (spec.commitmentMonths() == null || spec.commitmentMonths() < 1) {
            throw new BadRequestException("Un engagement se compte en mois, au moins un");
        }
        SubscriptionPolicy policy = policyService.current();
        LocalDate start = spec.commitmentStart() != null ? spec.commitmentStart()
                : subscription.getStartDate() != null ? subscription.getStartDate() : LocalDate.now();
        EarlyTerminationFormula formula = spec.formula() != null ? spec.formula() : policy.getEarlyTerminationFormula();
        if (formula == EarlyTerminationFormula.FIXED_FEE && spec.fixedFee() == null && policy.getEarlyTerminationFixedFee() == null) {
            throw new BadRequestException("Une pénalité fixe demande un montant");
        }
        SubscriptionCommitment commitment = commitmentRepository.findBySubscription_Id(subscription.getId())
                .orElseGet(() -> SubscriptionCommitment.builder().subscription(subscription).build());
        commitment.setCommitmentMonths(spec.commitmentMonths());
        commitment.setCommitmentStart(start);
        commitment.setCommitmentEnd(start.plusMonths(spec.commitmentMonths()).minusDays(1));
        commitment.setEarlyTerminationFormula(formula);
        commitment.setEarlyTerminationPercent(spec.percent() != null ? spec.percent() : policy.getEarlyTerminationPercent());
        commitment.setEarlyTerminationFixedFee(spec.fixedFee() != null ? spec.fixedFee() : policy.getEarlyTerminationFixedFee());
        commitment.setAutoRenewCommitment(Boolean.TRUE.equals(spec.autoRenewCommitment()));
        commitment.setSource(source);
        commitment.setCreatedBy(actor);
        SubscriptionCommitment saved = commitmentRepository.save(commitment);
        eventWriter.writeEvent(subscription, SubscriptionEventType.COMMITMENT_SET,
                "{\"months\":" + spec.commitmentMonths() + ",\"until\":\"" + saved.getCommitmentEnd() + "\",\"source\":\"" + source + "\"}");
        return saved;
    }

    /** L'engagement que le plan impose · pose a la souscription si le prix en porte un. */
    @Transactional
    public Optional<SubscriptionCommitment> setFromPlan(Subscription subscription, Integer planCommitmentMonths) {
        if (planCommitmentMonths == null || planCommitmentMonths < 1) {
            return Optional.empty();
        }
        if (commitmentRepository.findBySubscription_Id(subscription.getId()).isPresent()) {
            return commitmentRepository.findBySubscription_Id(subscription.getId());
        }
        return Optional.of(set(subscription, new Spec(planCommitmentMonths, subscription.getStartDate(), null, null, null, false),
                SubscriptionCommitment.Source.PLAN, "SYSTEM"));
    }

    @Transactional
    public void release(Subscription subscription, String actor) {
        commitmentRepository.findBySubscription_Id(subscription.getId()).ifPresent(c -> {
            commitmentRepository.delete(c);
            eventWriter.writeEvent(subscription, SubscriptionEventType.COMMITMENT_SET, "{\"released\":true,\"by\":\"" + actor + "\"}");
        });
    }

    /** Ce que coute de sortir a cette date · la reponse a donner avant d'accepter un preavis. */
    @Transactional(readOnly = true)
    public EarlyTermination earlyTerminationOn(Subscription subscription, LocalDate effectiveDate) {
        return commitmentRepository.findBySubscription_Id(subscription.getId())
                .filter(c -> effectiveDate.isBefore(c.getCommitmentEnd()))
                .map(c -> {
                    int remaining = remainingMonths(effectiveDate, c.getCommitmentEnd());
                    BigDecimal monthly = monthlyEquivalent(subscription);
                    BigDecimal fee = switch (c.getEarlyTerminationFormula()) {
                        case NONE -> BigDecimal.ZERO;
                        case FIXED_FEE -> nonNull(c.getEarlyTerminationFixedFee());
                        case REMAINING_PERIODS -> monthly.multiply(BigDecimal.valueOf(remaining));
                        case PERCENT_OF_REMAINING -> monthly.multiply(BigDecimal.valueOf(remaining))
                                .multiply(nonNull(c.getEarlyTerminationPercent())).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    };
                    return new EarlyTermination(true, remaining, fee.setScale(4, RoundingMode.HALF_UP), c.getEarlyTerminationFormula(), c.getCommitmentEnd());
                })
                .orElse(EarlyTermination.none());
    }

    /**
     * Au terme · l'engagement se reconduit s'il l'a dit, sinon il tombe et l'abonnement continue
     * sans engagement. Appele par le worker quotidien.
     */
    @Transactional
    public int rollOverEnded() {
        List<SubscriptionCommitment> ended = commitmentRepository.findAllEndedBefore(LocalDate.now());
        int rolled = 0;
        for (SubscriptionCommitment c : ended) {
            Subscription subscription = c.getSubscription();
            if (Boolean.TRUE.equals(c.getAutoRenewCommitment()) && subscription.getStatus().open()) {
                LocalDate start = c.getCommitmentEnd().plusDays(1);
                c.setCommitmentStart(start);
                c.setCommitmentEnd(start.plusMonths(c.getCommitmentMonths()).minusDays(1));
                commitmentRepository.save(c);
                eventWriter.writeEvent(subscription, SubscriptionEventType.COMMITMENT_SET,
                        "{\"months\":" + c.getCommitmentMonths() + ",\"until\":\"" + c.getCommitmentEnd() + "\",\"source\":\"ROLLOVER\"}");
                rolled++;
            } else {
                commitmentRepository.delete(c);
                eventWriter.writeEvent(subscription, SubscriptionEventType.COMMITMENT_SET, "{\"ended\":\"" + c.getCommitmentEnd() + "\"}");
            }
        }
        return rolled;
    }

    /** Le prix d'un mois, quel que soit le rythme · la base des penalites. */
    public static BigDecimal monthlyEquivalent(Subscription subscription) {
        BigDecimal total = subscription.getTotalAmount() == null ? BigDecimal.ZERO : subscription.getTotalAmount();
        BillingCycle cycle = subscription.getBillingCycle() == null ? BillingCycle.MONTHLY : subscription.getBillingCycle();
        return switch (cycle) {
            case DAILY -> total.multiply(BigDecimal.valueOf(365)).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
            case WEEKLY -> total.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
            case MONTHLY, ONE_TIME -> total;
            case QUARTERLY -> total.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
            case YEARLY -> total.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        };
    }

    /** Mois restants, tout mois entame compte. */
    static int remainingMonths(LocalDate from, LocalDate commitmentEnd) {
        if (!from.isBefore(commitmentEnd)) {
            return 0;
        }
        LocalDate exclusiveEnd = commitmentEnd.plusDays(1);
        long months = ChronoUnit.MONTHS.between(from, exclusiveEnd);
        if (from.plusMonths(months).isBefore(exclusiveEnd)) {
            months++;
        }
        return (int) Math.max(1, months);
    }

    private Subscription subscription(String number) {
        return subscriptionRepository.findBySubscriptionNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
