package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.service.interfaces.CashStaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Traitement periodique du personnel de caisse (lot C2) : absences qui commencent (la session
 * laissee ouverte par l'absent est suspendue) ou finissent, rappel avant le depart, alerte des
 * sessions ouvertes au-dela de la duree maximale de leur caisse.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashStaffWorker {

    private final CashStaffService staffService;

    @Scheduled(cron = "${bokati.cash.staff-worker-cron:0 10 * * * *}", zone = "Africa/Brazzaville")
    public void run() {
        try {
            staffService.runPeriodicChecks();
        } catch (RuntimeException exception) {
            log.error("Caisse · traitement periodique du personnel en echec : {}", exception.getMessage(), exception);
        }
    }
}
