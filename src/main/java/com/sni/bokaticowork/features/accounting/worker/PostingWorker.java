package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Comptabilisation des evenements en file (lot F2), quand la comptabilite est active. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostingWorker {

    private final PostingEngine engine;
    private final com.sni.bokaticowork.features.accounting.service.implementation.CashAccountingService cash;
    private final com.sni.bokaticowork.features.accounting.service.implementation.SalesAccountingService sales;

    @Scheduled(fixedDelayString = "${accounting.posting.worker-delay-ms:30000}", initialDelayString = "${accounting.posting.worker-delay-ms:30000}")
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
    @Scheduled(fixedDelayString = "${accounting.lettering.auto-delay-ms:600000}", initialDelayString = "${accounting.lettering.auto-delay-ms:600000}")
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
