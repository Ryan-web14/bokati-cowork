package com.sni.bokaticowork.features.ressource.worker;

import com.sni.bokaticowork.features.ressource.service.support.MonthlyAvailabilityGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
public class MonthlyAvailabilityWorker {

    private final MonthlyAvailabilityGenerator generator;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${bokati.resource.workers.monthly-availability-enabled:true}")
    private boolean enabled;

    /** Au deploiement · une seule fois, apres que le reste de l'application a demarre. */
    @Scheduled(initialDelayString = "${bokati.resource.workers.monthly-availability-initial-delay-ms:120000}")
    public void onStartup() {
        run("deploiement");
    }

    /** Le premier de chaque mois. */
    @Scheduled(cron = "${bokati.resource.workers.monthly-availability-cron:0 0 3 1 * *}",
            zone = "${app.time-zone:Africa/Lagos}")
    public void monthly() {
        run("mensuel");
    }

    void run(String trigger) {
        if (!enabled || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            MonthlyAvailabilityGenerator.Report report = generator.generate(LocalDateTime.now());
            log.info("Disponibilites mensuelles ({}) · {} ressources ouvertes ({} journees), {} deja pourvues sautees, "
                            + "{} journees refusees (fermeture, chevauchement)",
                    trigger, report.resourcesFilled(), report.daysCreated(), report.resourcesSkipped(),
                    report.daysRefused());
        } catch (RuntimeException ex) {
            log.error("Disponibilites mensuelles ({}) · passage interrompu", trigger, ex);
        } finally {
            running.set(false);
        }
    }
}
