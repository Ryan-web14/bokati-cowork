package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Une periode echue que rien n'a fait avancer finit par fermer l'abonnement.
 *
 * <p>Trois chemins menaient a un abonnement echu, et aucun ne se refermait :</p>
 * <ul>
 *   <li>le renouvellement ne prend que les abonnements en reconduction automatique ·
 *       {@code auto_renew = false} n'etait donc ni facture, ni clos ;</li>
 *   <li>la cloture de fin de periode ne prend que ceux qui l'avaient demandee
 *       ({@code cancel_at_period_end}) ;</li>
 *   <li>la tolerance ne voit que ceux qui portent une facture de renouvellement impayee · quand
 *       le renouvellement n'a jamais produit de facture, elle ne se declenche jamais.</li>
 * </ul>
 *
 * <p>Resultat observe en production : des abonnements {@code ACTIVE} des mois apres la fin de leur
 * periode, droits ouverts, sans qu'aucune facture n'ait ete emise. Personne ne reclamait rien et
 * personne ne fermait rien.</p>
 *
 * <p>La regle est celle que la politique porte deja · on n'invente pas de delai : au-dela de
 * {@code gracePeriodDays} apres la fin de periode l'abonne est prevenu et passe en tolerance ;
 * au-dela de {@code gracePeriodDays + suspensionAfterGraceDays} l'abonnement passe
 * {@code EXPIRED}. Un reglement ou un renouvellement entre-temps fait avancer la periode, et
 * l'abonnement sort de ce balayage de lui-meme.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionExpiryService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPolicyService policyService;
    private final SubscriptionStatusManager statusManager;
    private final SubscriptionEventWriter eventWriter;
    private final @Lazy SubscriptionGraceService graceService;
    private final @Lazy SubscriptionExpiryService self;

    /** Ce qu'une passe a donne · prevenus, puis fermes. */
    public record Sweep(int warned, int expired) {
    }

    public Sweep sweep() {
        SubscriptionPolicy policy = policyService.current();
        LocalDate today = LocalDate.now();
        LocalDate warnBefore = today.minusDays(policy.getGracePeriodDays());
        LocalDate expireBefore = today.minusDays(
                (long) policy.getGracePeriodDays() + policy.getSuspensionAfterGraceDays());

        int warned = 0;
        int expired = 0;
        List<Subscription> ended = subscriptionRepository.findEndedPeriodsStillOpen(today);
        for (Subscription subscription : ended) {
            try {
                // Chacun sa transaction · un abonnement en erreur ne doit pas retenir les autres,
                // c'est exactement ce qui avait fige la facturation.
                Outcome outcome = self.settle(subscription.getId(), warnBefore, expireBefore, policy);
                if (outcome == Outcome.WARNED) warned++;
                if (outcome == Outcome.EXPIRED) expired++;
            } catch (Exception ex) {
                log.error("Abonnement {} · cloture de periode echue impossible, les suivants continuent",
                        subscription.getSubscriptionNumber(), ex);
            }
        }
        if (!ended.isEmpty()) {
            log.info("Periodes echues · {} abonnement(s) en retard, {} prevenu(s), {} ferme(s)",
                    ended.size(), warned, expired);
        }
        return new Sweep(warned, expired);
    }

    enum Outcome { NOTHING, WARNED, EXPIRED }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome settle(Long subscriptionId, LocalDate warnBefore, LocalDate expireBefore,
                          SubscriptionPolicy policy) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null || subscription.getCurrentPeriodEnd() == null
                || !subscription.getStatus().open()) {
            return Outcome.NOTHING;
        }
        LocalDate periodEnd = subscription.getCurrentPeriodEnd();
        if (!periodEnd.isBefore(LocalDate.now())) {
            // La periode a ete relancee entre la lecture et l'ecriture · plus rien a faire.
            return Outcome.NOTHING;
        }

        if (periodEnd.isBefore(expireBefore)) {
            long days = ChronoUnit.DAYS.between(periodEnd, LocalDate.now());
            String reason = "Période échue le " + periodEnd + " · aucun règlement ni renouvellement depuis "
                    + days + " jours";
            statusManager.changeStatus(subscription, SubscriptionStatus.EXPIRED, reason, "SYSTEM");
            subscriptionRepository.save(subscription);
            eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_EXPIRED,
                    "{\"periodEnd\":\"" + periodEnd + "\",\"daysOverdue\":" + days + "}");
            log.info("Abonnement {} · ferme, periode echue le {} depuis {} jours",
                    subscription.getSubscriptionNumber(), periodEnd, days);
            return Outcome.EXPIRED;
        }

        if (periodEnd.isBefore(warnBefore) && subscription.getStatus() != SubscriptionStatus.GRACE_PERIOD) {
            // Prevenir avant de fermer · l'abonne doit pouvoir regler, et la tolerance est
            // exactement l'etat prevu pour ca. La suspension au terme de la tolerance est geree
            // par le balayage existant ; si rien ne bouge, la fermeture viendra d'ici.
            boolean entered = graceService.enter(subscription,
                    "Période échue le " + periodEnd + " · renouvellement non réglé");
            return entered ? Outcome.WARNED : Outcome.NOTHING;
        }
        return Outcome.NOTHING;
    }
}
