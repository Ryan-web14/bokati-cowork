package com.sni.bokaticowork.features.subscription.lifecycle.worker;

import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionCommitmentService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionEndNoticeService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionExpiryService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionQuoteService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionTerminationService;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;

/**
 * Ce que le cycle de vie fait tout seul, chaque heure.
 *
 * <p>Les preavis arrives a leur date d'effet s'achevent, les devis perimes expirent, les echeances
 * impayees entrent en tolerance puis en suspension, les abonnements qui prennent fin l'annoncent
 * puis se cloturent, les reconductions automatiques en echec sont rattrapees, les engagements
 * arrives a terme se reconduisent ou tombent, et les abonnements qui attendaient des pieces sont
 * reessayes · si les pieces sont arrivees, ils s'activent sans que personne n'y pense.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionLifecycleWorker implements SchedulingConfigurer {

    private static final String LIFECYCLE_DELAY_MS_KEY = "bokati.subscription.workers.lifecycle-delay-ms";
    private static final String LIFECYCLE_INITIAL_DELAY_MS_KEY = "bokati.subscription.workers.lifecycle-initial-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SubscriptionLifecycleWorker> self;
    private PlatformSettings platformSettings;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SubscriptionLifecycleWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.fixedDelay(LIFECYCLE_DELAY_MS_KEY, 3600000L, platformSettings == null ? 120000L : platformSettings.number(LIFECYCLE_INITIAL_DELAY_MS_KEY, 120000L)));
    }

    private final SubscriptionTerminationService terminationService;
    private final SubscriptionQuoteService quoteService;
    private final SubscriptionGraceService graceService;
    private final SubscriptionEndNoticeService endNoticeService;
    private final SubscriptionExpiryService expiryService;
    private final SubscriptionCommitmentService commitmentService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionLifecycleOperator lifecycleOperator;

    public void run() {
        step("preavis acheves", terminationService::completeDue);
        step("devis expires", quoteService::expireOpen);
        step("tolerance", () -> {
            SubscriptionGraceService.Sweep sweep = graceService.sweep();
            return sweep.entered() + sweep.suspended();
        });
        // Les abonnements qui prennent reellement fin · J-7, J-3, le jour meme, puis la cloture.
        step("fins annoncees", () -> {
            SubscriptionEndNoticeService.Sweep sweep = endNoticeService.sweep();
            return sweep.noticed() + sweep.cancelled();
        });
        // Le filet des reconductions automatiques en echec · ce que la tolerance ne voit pas,
        // faute de facture de renouvellement.
        step("periodes echues", () -> {
            SubscriptionExpiryService.Sweep sweep = expiryService.sweep();
            return sweep.warned() + sweep.expired();
        });
        step("engagements reconduits", commitmentService::rollOverEnded);
        step("pieces arrivees", this::retryPendingDocuments);
    }

    @Transactional
    public int retryPendingDocuments() {
        int activated = 0;
        for (Subscription subscription : subscriptionRepository.findAllByStatus(SubscriptionStatus.PENDING_DOCUMENTS.name())) {
            try {
                lifecycleOperator.activate(subscription, "Pièces reçues · activation", "SYSTEM");
                if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                    activated++;
                }
            } catch (RuntimeException ex) {
                log.warn("Abonnement {} · reprise apres pieces impossible : {}", subscription.getSubscriptionNumber(), ex.getMessage());
            }
        }
        return activated;
    }

    private void step(String label, java.util.function.IntSupplier task) {
        try {
            int count = task.getAsInt();
            if (count > 0) {
                log.info("Cycle de vie · {} : {}", label, count);
            }
        } catch (Exception ex) {
            log.error("Cycle de vie · {} en echec : {}", label, ex.getMessage(), ex);
        }
    }
}
