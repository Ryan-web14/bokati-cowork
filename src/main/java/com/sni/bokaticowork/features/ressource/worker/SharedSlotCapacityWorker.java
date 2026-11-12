package com.sni.bokaticowork.features.ressource.worker;

import com.sni.bokaticowork.features.ressource.service.support.SharedSlotCapacityAligner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;

/**
 * Remet les creneaux a venir a la capacite reelle de leur ressource.
 *
 * <p>Premier passage peu apres le demarrage, donc des le deploiement · c'est lui qui rattrape les
 * creneaux deja ouverts a une mauvaise quantite. Puis toutes les heures, pour suivre une capacite
 * modifiee depuis. Un passage sans ecart ne fait qu'une requete et n'ecrit rien dans le journal.</p>
 *
 * <p>Une salle de douze places passe de 1 a 12 · c'est un affichage, la salle reste reservee par
 * un seul groupe a la fois. Voir {@link SharedSlotCapacityAligner}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SharedSlotCapacityWorker implements SchedulingConfigurer {

    private static final String SLOT_CAPACITY_DELAY_MS_KEY = "bokati.resource.workers.slot-capacity-delay-ms";
    private static final String SLOT_CAPACITY_INITIAL_DELAY_MS_KEY = "bokati.resource.workers.slot-capacity-initial-delay-ms";
    private static final String SLOT_CAPACITY_ENABLED_KEY = "bokati.resource.workers.slot-capacity-enabled";

    private SettingTriggers triggers;
    private ObjectProvider<SharedSlotCapacityWorker> self;
    private PlatformSettings platformSettings;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SharedSlotCapacityWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().align(), triggers.fixedDelay(SLOT_CAPACITY_DELAY_MS_KEY, 3600000L, platformSettings == null ? 60000L : platformSettings.number(SLOT_CAPACITY_INITIAL_DELAY_MS_KEY, 60000L)));
    }

    private final SharedSlotCapacityAligner aligner;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${bokati.resource.workers.slot-capacity-enabled:true}")
    private boolean enabled;

    public void align() {
        boolean active = platformSettings == null ? enabled : platformSettings.bool(SLOT_CAPACITY_ENABLED_KEY, enabled);
        if (!active || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            int aligned = aligner.alignAll();
            if (aligned > 0) {
                log.info("Capacite des creneaux · {} creneaux remis a la capacite reelle de leur ressource",
                        aligned);
            }
        } catch (RuntimeException ex) {
            log.error("Capacite des creneaux · passage interrompu", ex);
        } finally {
            running.set(false);
        }
    }
}
