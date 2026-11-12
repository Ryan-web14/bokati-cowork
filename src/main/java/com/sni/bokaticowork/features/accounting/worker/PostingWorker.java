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

    @Scheduled(fixedDelayString = "${accounting.posting.worker-delay-ms:30000}", initialDelayString = "${accounting.posting.worker-delay-ms:30000}")
    public void run() {
        try {
            PostingDtos.BatchResult result = engine.processPending();
            if (result.failed() > 0) {
                log.warn("Comptabilite · {} evenement(s) en echec sur {} : {}", result.failed(), result.processed(), result.errors());
            }
        } catch (RuntimeException exception) {
            log.error("Comptabilite · moteur d'imputation en echec : {}", exception.getMessage(), exception);
        }
    }
}
