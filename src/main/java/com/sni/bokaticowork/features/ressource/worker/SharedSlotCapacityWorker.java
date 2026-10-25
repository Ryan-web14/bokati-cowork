package com.sni.bokaticowork.features.ressource.worker;

import com.sni.bokaticowork.features.ressource.service.support.SharedSlotCapacityAligner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

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
public class SharedSlotCapacityWorker {

    private final SharedSlotCapacityAligner aligner;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${bokati.resource.workers.slot-capacity-enabled:true}")
    private boolean enabled;

    @Scheduled(initialDelayString = "${bokati.resource.workers.slot-capacity-initial-delay-ms:60000}",
            fixedDelayString = "${bokati.resource.workers.slot-capacity-delay-ms:3600000}")
    public void align() {
        if (!enabled || !running.compareAndSet(false, true)) {
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
