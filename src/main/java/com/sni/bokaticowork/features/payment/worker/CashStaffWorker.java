package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.service.interfaces.CashStaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import java.time.ZoneId;

/**
 * Traitement periodique du personnel de caisse (lot C2) : absences qui commencent (la session
 * laissee ouverte par l'absent est suspendue) ou finissent, rappel avant le depart, alerte des
 * sessions ouvertes au-dela de la duree maximale de leur caisse.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashStaffWorker implements SchedulingConfigurer {

    private static final String STAFF_WORKER_CRON_KEY = "bokati.cash.staff-worker-cron";

    private SettingTriggers triggers;
    private ObjectProvider<CashStaffWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<CashStaffWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.cron(STAFF_WORKER_CRON_KEY, "0 10 * * * *", ZoneId.of("Africa/Brazzaville")));
    }

    private final CashStaffService staffService;
    private final com.sni.bokaticowork.features.payment.service.interfaces.CashOperationsService operations;
    private final com.sni.bokaticowork.features.payment.service.interfaces.CashDashboardService dashboard;

    public void run() {
        try {
            staffService.runPeriodicChecks();
        } catch (RuntimeException exception) {
            log.error("Caisse · traitement periodique du personnel en echec : {}", exception.getMessage(), exception);
        }
        try {
            // Visas expires, transferts trop longtemps en transit (lot C3).
            operations.runPeriodicChecks();
        } catch (RuntimeException exception) {
            log.error("Caisse · traitement periodique des operations en echec : {}", exception.getMessage(), exception);
        }
        try {
            // Caisses non comptees depuis trop longtemps (lot C6).
            dashboard.runPeriodicChecks();
        } catch (RuntimeException exception) {
            log.error("Caisse · alertes de comptage en echec : {}", exception.getMessage(), exception);
        }
    }
}
