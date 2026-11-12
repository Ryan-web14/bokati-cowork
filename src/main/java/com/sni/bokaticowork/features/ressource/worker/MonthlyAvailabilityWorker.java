package com.sni.bokaticowork.features.ressource.worker;

import com.sni.bokaticowork.features.ressource.service.support.MonthlyAvailabilityGenerator;
import lombok.RequiredArgsConstructor;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ouvre chaque mois les disponibilites de toutes les ressources.
 *
 * <p>Deux declenchements · une fois peu apres le demarrage, pour couvrir le reste du mois des le
 * deploiement, puis le premier de chaque mois. Les ressources qui ont deja des creneaux dans le
 * mois sont sautees · voir {@link MonthlyAvailabilityGenerator}.</p>
 *
 * <p>Le passage mensuel tourne a 03:00 et non a minuit · les creneaux sont dates a l'heure du
 * serveur, et un serveur en UTC lu depuis un fuseau en avance serait encore au mois precedent a
 * minuit. A 03:00, tous les fuseaux d'UTC-2 a UTC+3 sont deja au premier du mois.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyAvailabilityWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "bokati.resource.workers.monthly-availability-cron";
    private static final String INITIAL_DELAY_MS_KEY = "bokati.resource.workers.monthly-availability-initial-delay-ms";
    private static final String ENABLED_KEY = "bokati.resource.workers.monthly-availability-enabled";
    private static final long DEFAULT_INITIAL_DELAY_MS = 120000L;

    private final MonthlyAvailabilityGenerator generator;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${bokati.resource.workers.monthly-availability-enabled:true}")
    private boolean enabled;

    /** Fuseau du passage mensuel, lu une fois au demarrage. */
    @Value("${app.time-zone:Africa/Lagos}")
    private String timeZone = "Africa/Lagos";

    private SettingTriggers triggers;
    private ObjectProvider<MonthlyAvailabilityWorker> self;
    private PlatformSettings platformSettings;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<MonthlyAvailabilityWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /**
     * Passage au deploiement apres un delai lu une fois, puis passage mensuel dont l'expression
     * cron est relue a chaque execution : un changement prend effet sans redemarrage.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        long initialDelay = platformSettings == null ? DEFAULT_INITIAL_DELAY_MS
                : platformSettings.number(INITIAL_DELAY_MS_KEY, DEFAULT_INITIAL_DELAY_MS);
        registrar.addOneTimeTask(() -> self.getObject().onStartup(), Duration.ofMillis(Math.max(0, initialDelay)));
        registrar.addTriggerTask(() -> self.getObject().monthly(),
                triggers.cron(CRON_KEY, "0 0 3 1 * *", ZoneId.of(timeZone)));
    }

    /** Au deploiement · une seule fois, apres que le reste de l'application a demarre. */
    public void onStartup() {
        run("deploiement");
    }

    /** Le premier de chaque mois. */
    public void monthly() {
        run("mensuel");
    }

    void run(String trigger) {
        boolean active = platformSettings == null ? enabled : platformSettings.bool(ENABLED_KEY, enabled);
        if (!active || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            MonthlyAvailabilityGenerator.Report report = generator.generate(LocalDateTime.now());
            log.info("Disponibilites mensuelles ({}) · {} ressources ouvertes ({} plages), {} deja pourvues sautees, "
                            + "{} plages refusees (fermeture, chevauchement)",
                    trigger, report.resourcesFilled(), report.rangesCreated(), report.resourcesSkipped(),
                    report.rangesRefused());
        } catch (RuntimeException ex) {
            log.error("Disponibilites mensuelles ({}) · passage interrompu", trigger, ex);
        } finally {
            running.set(false);
        }
    }
}
