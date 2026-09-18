package com.sni.bokaticowork.features.subscription.subscription.pass.worker;

import com.sni.bokaticowork.features.subscription.subscription.pass.service.PassAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Detecte les pass a signaler, une fois par jour.
 *
 * <p>Les trois detections sont appelees separement, chacune dans sa transaction : une erreur sur
 * les pass inutilises ne doit pas annuler les alertes d'expiration deja posees.</p>
 *
 * <p>Une fois par jour suffit, et c'est meme la bonne cadence. Ces alertes se comptent en jours ·
 * les verifier plus souvent ne previendrait personne plus tot, et multiplierait les occasions de
 * doublon si la memoire des annonces venait a faillir.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PassAlertWorker {

    private final PassAlertService alertService;

    @Scheduled(cron = "${bokati.pass.alert.cron:0 0 8 * * *}")
    public void announcePassAlerts() {
        int expiring = run("expiration", alertService::announceExpiring);
        int lowBalance = run("solde bas", alertService::announceLowBalance);
        int unused = run("inutilise", alertService::announceUnused);

        int total = expiring + lowBalance + unused;
        if (total > 0) {
            log.info("Alertes pass : {} annoncee(s) · {} expiration, {} solde bas, {} inutilise",
                    total, expiring, lowBalance, unused);
        }
    }

    private int run(String label, java.util.function.IntSupplier detection) {
        try {
            return detection.getAsInt();
        } catch (Exception ex) {
            log.error("Detection des alertes pass « {} » en echec : {}", label, ex.getMessage(), ex);
            return 0;
        }
    }
}
