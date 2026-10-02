package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.lifecycle.model.EarlyTerminationFormula;
import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** La politique du cycle de vie · lue partout, modifiee a un seul endroit. */
@Service
@RequiredArgsConstructor
public class SubscriptionPolicyService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SubscriptionPolicyRepository repository;

    public record Update(ProrationPolicy prorationPolicy, Integer defaultNoticeDays,
                         EarlyTerminationFormula earlyTerminationFormula, BigDecimal earlyTerminationPercent, BigDecimal earlyTerminationFixedFee,
                         Integer freezeMaxPerYear, Integer freezeMaxDays, Integer freezeNoticeDays, BigDecimal freezeFeePercent,
                         Integer gracePeriodDays, Integer suspensionAfterGraceDays, Integer renewalExpiryDays,
                         Integer quoteValidityDays) {
    }

    /** La politique active · sans ligne en base, les valeurs par defaut du modele valent. */
    @Transactional(readOnly = true)
    public SubscriptionPolicy current() {
        return repository.findFirstByActiveTrueOrderByIdAsc()
                .orElseGet(() -> SubscriptionPolicy.builder().policyCode("DEFAULT").name("Politique par défaut").build());
    }

    @Transactional
    public SubscriptionPolicy update(Update update) {
        SubscriptionPolicy policy = repository.findFirstByActiveTrueOrderByIdAsc()
                .orElseGet(() -> SubscriptionPolicy.builder().policyCode("DEFAULT").name("Politique par défaut").build());
        if (update.prorationPolicy() != null) policy.setProrationPolicy(update.prorationPolicy());
        if (update.defaultNoticeDays() != null) policy.setDefaultNoticeDays(nonNegative(update.defaultNoticeDays(), "préavis"));
        if (update.earlyTerminationFormula() != null) policy.setEarlyTerminationFormula(update.earlyTerminationFormula());
        if (update.earlyTerminationPercent() != null) policy.setEarlyTerminationPercent(percent(update.earlyTerminationPercent(), "pénalité de rupture"));
        if (update.earlyTerminationFixedFee() != null) {
            policy.setEarlyTerminationFixedFee(update.earlyTerminationFixedFee().signum() < 0 ? null : update.earlyTerminationFixedFee());
        }
        if (update.freezeMaxPerYear() != null) policy.setFreezeMaxPerYear(nonNegative(update.freezeMaxPerYear(), "gels par an"));
        if (update.freezeMaxDays() != null) policy.setFreezeMaxDays(nonNegative(update.freezeMaxDays(), "durée de gel"));
        if (update.freezeNoticeDays() != null) policy.setFreezeNoticeDays(nonNegative(update.freezeNoticeDays(), "préavis de gel"));
        if (update.freezeFeePercent() != null) policy.setFreezeFeePercent(percent(update.freezeFeePercent(), "part facturée pendant le gel"));
        if (update.gracePeriodDays() != null) policy.setGracePeriodDays(nonNegative(update.gracePeriodDays(), "tolérance"));
        if (update.suspensionAfterGraceDays() != null) policy.setSuspensionAfterGraceDays(nonNegative(update.suspensionAfterGraceDays(), "délai avant suspension"));
        if (update.renewalExpiryDays() != null) {
            policy.setRenewalExpiryDays(nonNegative(update.renewalExpiryDays(), "fermeture apres reconduction en echec"));
        }
        if (update.quoteValidityDays() != null) {
            if (update.quoteValidityDays() < 1) throw new BadRequestException("La validité d'un devis est d'au moins un jour");
            policy.setQuoteValidityDays(update.quoteValidityDays());
        }
        if (policy.getEarlyTerminationFormula() == EarlyTerminationFormula.FIXED_FEE && policy.getEarlyTerminationFixedFee() == null) {
            throw new BadRequestException("Une pénalité fixe demande un montant");
        }
        return repository.save(policy);
    }

    private static int nonNegative(int value, String what) {
        if (value < 0) throw new BadRequestException("Le nombre de jours (" + what + ") ne peut pas être négatif");
        return value;
    }

    private static BigDecimal percent(BigDecimal value, String what) {
        if (value.signum() < 0 || value.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("Le pourcentage (" + what + ") est entre 0 et 100");
        }
        return value;
    }
}
