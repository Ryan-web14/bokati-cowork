package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.subscription.overage.enums.OveragePolicyMode;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOveragePolicy;
import com.sni.bokaticowork.features.subscription.overage.repository.SubscriptionOveragePolicyRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Le depassement est-il permis sur ce droit.
 *
 * <p>Un droit epuise n'est pas un droit absent, et la difference decide entre deux reponses tres
 * differentes. « Votre abonnement ne couvre pas cette salle » est un refus definitif. « Vous avez
 * epuise vos cinq heures » ouvre le depassement, si le plan l'autorise · la reservation passe, et
 * les heures supplementaires sont facturees.</p>
 *
 * <p>Le module de depassement existait deja, avec ses politiques, ses tarifs et sa facturation. Il
 * n'etait simplement jamais atteint depuis une reservation : celle-ci etait refusee avant, parce
 * que la recherche de droit exigeait un solde positif. Ce garde est le chainon manquant, pas un
 * second mecanisme.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingOverageGuard {

    private final SubscriptionOveragePolicyRepository overagePolicyRepository;

    /**
     * @return la politique applicable si elle autorise le depassement, sinon vide
     */
    @Transactional(readOnly = true)
    public Optional<SubscriptionOveragePolicy> allowedPolicy(PlanVersion planVersion, String entitlementCode) {
        if (planVersion == null || planVersion.getId() == null || !StringUtils.hasText(entitlementCode)) {
            return Optional.empty();
        }
        return overagePolicyRepository.findActivePolicy(planVersion.getId(), entitlementCode.trim())
                .filter(this::permits);
    }

    public boolean allows(PlanVersion planVersion, String entitlementCode) {
        return allowedPolicy(planVersion, entitlementCode).isPresent();
    }

    /**
     * {@code BLOCK} est le defaut implicite : sans politique, rien ne depasse. C'est le sens le plus
     * sur · laisser consommer sans avoir decide ce que cela coute revient a offrir la prestation.
     */
    private boolean permits(SubscriptionOveragePolicy policy) {
        return policy.getMode() == OveragePolicyMode.BILLABLE
                || policy.getMode() == OveragePolicyMode.ALLOW_UNBILLED;
    }
}
