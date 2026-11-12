package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

/** Comptabilisation des evenements en file (lot F2), quand la comptabilite est active. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostingWorker implements SchedulingConfigurer {

    private static final String WORKER_DELAY_MS_KEY = "accounting.posting.worker-delay-ms";
    private static final String AUTO_DELAY_MS_KEY = "accounting.lettering.auto-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<PostingWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<PostingWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.fixedDelay(WORKER_DELAY_MS_KEY, 30000L, -1));
        registrar.addTriggerTask(() -> self.getObject().letter(), triggers.fixedDelay(AUTO_DELAY_MS_KEY, 600000L, -1));
    }

    private final PostingEngine engine;
    private final com.sni.bokaticowork.features.accounting.service.implementation.CashAccountingService cash;
    private final com.sni.bokaticowork.features.accounting.service.implementation.SalesAccountingService sales;

    public void run() {
        try {
            PostingDtos.BatchResult fed = cash.feed();
            if (fed.failed() > 0) {
                log.warn("Comptabilite · {} fait(s) de caisse non transmis : {}", fed.failed(), fed.errors());
            }
        } catch (RuntimeException exception) {
            log.error("Comptabilite · alimentation de la caisse en echec : {}", exception.getMessage(), exception);
        }
        try {
            PostingDtos.BatchResult fed = sales.feed();
            if (fed.failed() > 0) {
                log.warn("Comptabilite · {} fait(s) de vente ou d'encaissement non transmis : {}", fed.failed(), fed.errors());
            }
        } catch (RuntimeException exception) {
            log.error("Comptabilite · alimentation des ventes en echec : {}", exception.getMessage(), exception);
        }
        try {
            PostingDtos.BatchResult result = engine.processPending();
            if (result.failed() > 0) {
                log.warn("Comptabilite · {} evenement(s) en echec sur {} : {}", result.failed(), result.processed(), result.errors());
            }
        } catch (RuntimeException exception) {
            log.error("Comptabilite · moteur d'imputation en echec : {}", exception.getMessage(), exception);
        }
    }

    /** Lettrage automatique des comptes clients et des avances, par reference (lot F4). */
    public void letter() {
        try {
            PostingDtos.BatchResult result = sales.autoLetter();
            if (result.failed() > 0) {
                log.warn("Comptabilite · lettrage automatique : {} groupe(s) en echec : {}", result.failed(), result.errors());
            }
        } catch (RuntimeException exception) {
            log.error("Comptabilite · lettrage automatique en echec : {}", exception.getMessage(), exception);
        }
    }
}
