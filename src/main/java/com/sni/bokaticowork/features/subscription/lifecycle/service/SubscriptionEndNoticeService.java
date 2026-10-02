package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
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
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La fin d'un abonnement s'annonce avant d'arriver.
 *
 * <p>Un abonnement qui ne se reconduit pas tout seul s'arrete a la fin de sa periode. Jusqu'ici il
 * ne s'arretait pas du tout · le renouvellement ne prend que la reconduction automatique, la
 * cloture de fin de periode que ceux qui l'avaient demandee. Un abonnement a renouvellement manuel
 * restait donc {@code ACTIVE} indefiniment, sans qu'on ait ni reclame le renouvellement, ni ferme
 * les droits.</p>
 *
 * <p>Le calendrier, du point de vue de l'abonne :</p>
 * <ul>
 *   <li><b>J-7</b> · « votre abonnement se termine le … » · il a une semaine pour renouveler ;</li>
 *   <li><b>J-3</b> · le rappel ;</li>
 *   <li><b>le jour meme</b> · « il prend fin a minuit » ;</li>
 *   <li><b>le lendemain</b> · l'abonnement est annule.</li>
 * </ul>
 *
 * <p>Renouveler emet une facture · c'est le renouvellement qui facture, pas l'avis. Les avis
 * disent quoi faire, ils n'engagent rien : personne ne recoit de facture pour une periode qu'il
 * n'a pas demandee.</p>
 *
 * <p>Un abonnement en reconduction automatique ne recoit aucun de ces avis · il ne prend pas fin,
 * il se renouvelle, et lui annoncer une fin serait faux. Quand son renouvellement echoue, c'est
 * {@link SubscriptionExpiryService} qui le rattrape.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionEndNoticeService {

    /** Les jalons, du plus lointain au plus proche · 0 est le jour de la fin. */
    static final int[] STAGES = {7, 3, 0};

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionStatusManager statusManager;
    private final SubscriptionEventWriter eventWriter;
    private final OutboxService outboxService;
    private final @Lazy SubscriptionEndNoticeService self;

    /** Ce qu'une passe a donne · avis envoyes, abonnements clos. */
    public record Sweep(int noticed, int cancelled) {
    }

    public Sweep sweep() {
        LocalDate today = LocalDate.now();
        int noticed = 0;
        int cancelled = 0;
        for (Subscription subscription : subscriptionRepository.findEndingSubscriptions(today.plusDays(STAGES[0]))) {
            try {
                // Chacun sa transaction · un abonnement en erreur ne retient pas les autres.
                Outcome outcome = self.settle(subscription.getId(), today);
                if (outcome == Outcome.NOTICED) noticed++;
                if (outcome == Outcome.CANCELLED) cancelled++;
            } catch (Exception ex) {
                log.error("Abonnement {} · avis de fin impossible, les suivants continuent",
                        subscription.getSubscriptionNumber(), ex);
            }
        }
        return new Sweep(noticed, cancelled);
    }

    enum Outcome { NOTHING, NOTICED, CANCELLED }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome settle(Long subscriptionId, LocalDate today) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null || subscription.getCurrentPeriodEnd() == null
                || !subscription.getStatus().open()) {
            return Outcome.NOTHING;
        }
        LocalDate periodEnd = subscription.getCurrentPeriodEnd();
        long daysLeft = ChronoUnit.DAYS.between(today, periodEnd);

        // Le lendemain de la fin · l'abonnement est clos.
        if (daysLeft < 0) {
            return cancel(subscription, periodEnd);
        }

        // L'avis le plus urgent qui s'applique · un abonnement vu pour la premiere fois a trois
        // jours de sa fin recoit l'avis de J-3, pas la serie des trois.
        Integer stage = stageFor(daysLeft);
        if (stage == null) {
            return Outcome.NOTHING;
        }
        Integer sent = subscription.getEndNoticeStage();
        if (sent != null && sent <= stage) {
            return Outcome.NOTHING;
        }
        notice(subscription, stage, periodEnd, daysLeft);
        subscription.setEndNoticeStage(stage);
        subscriptionRepository.save(subscription);
        return Outcome.NOTICED;
    }

    /** Le jalon applicable · nul quand la fin est encore trop loin pour en parler. */
    static Integer stageFor(long daysLeft) {
        for (int i = STAGES.length - 1; i >= 0; i--) {
            if (daysLeft <= STAGES[i]) {
                return STAGES[i];
            }
        }
        return null;
    }

    private Outcome cancel(Subscription subscription, LocalDate periodEnd) {
        String reason = "Abonnement arrivé à son terme le " + periodEnd + " · non renouvelé";
        statusManager.changeStatus(subscription, SubscriptionStatus.CANCELLED, reason, "SYSTEM");
        subscription.setCancelledAt(Instant.now());
        subscription.setCancellationReason(reason);
        subscription.setEndNoticeStage(null);
        subscriptionRepository.save(subscription);
        eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_CANCELLED,
                "{\"periodEnd\":\"" + periodEnd + "\",\"cause\":\"TERM_REACHED\"}");
        publish(subscription, "SUBSCRIPTION_ENDED",
                "Votre abonnement " + subscription.getSubscriptionNumber() + " a pris fin",
                Map.of("periodEnd", periodEnd.toString()));
        log.info("Abonnement {} · clos, terme atteint le {}", subscription.getSubscriptionNumber(), periodEnd);
        return Outcome.CANCELLED;
    }

    private void notice(Subscription subscription, int stage, LocalDate periodEnd, long daysLeft) {
        String subject = stage == 0
                ? "Votre abonnement " + subscription.getSubscriptionNumber() + " prend fin ce soir"
                : "Votre abonnement " + subscription.getSubscriptionNumber() + " se termine dans "
                        + daysLeft + (daysLeft > 1 ? " jours" : " jour");
        Map<String, String> facts = Map.of(
                "periodEnd", periodEnd.toString(),
                "daysLeft", String.valueOf(daysLeft),
                "stage", String.valueOf(stage),
                "endsTonight", String.valueOf(stage == 0),
                "manualRenewal", String.valueOf(!Boolean.TRUE.equals(subscription.getAutoRenew()))
        );
        publish(subscription, stage == 0 ? "SUBSCRIPTION_ENDS_TODAY" : "SUBSCRIPTION_ENDING_SOON", subject, facts);
        log.info("Abonnement {} · avis de fin J-{} pour le {}", subscription.getSubscriptionNumber(), stage, periodEnd);
    }

    private void publish(Subscription subscription, String eventType, String subject, Map<String, String> extra) {
        String email = SubscriptionGraceService.recipientEmail(subscription);
        if (!StringUtils.hasText(email)) {
            // Sans adresse joignable, l'avis ne peut pas partir · la cloture suit son cours.
            log.info("Abonnement {} · aucun destinataire joignable pour {}",
                    subscription.getSubscriptionNumber(), eventType);
            return;
        }
        Map<String, Object> payload = new HashMap<>(extra);
        payload.put("recipientEmail", email);
        payload.put("subject", subject);
        payload.put("templateCode", eventType.toLowerCase());
        payload.put("subscriptionNumber", subscription.getSubscriptionNumber());
        payload.put("planName", subscription.getPlanVersion() == null ? null : subscription.getPlanVersion().getName());
        outboxService.publish(eventType, "SUBSCRIPTION", subscription.getSubscriptionNumber(), payload);
    }

    /** Les jalons franchis n'ont plus de sens une fois la periode repartie. */
    public static void resetNotices(Subscription subscription) {
        subscription.setEndNoticeStage(null);
    }

    /** Utilise par les tests · la liste des jalons est une regle, pas une donnee. */
    static List<Integer> stages() {
        return List.of(7, 3, 0);
    }
}
