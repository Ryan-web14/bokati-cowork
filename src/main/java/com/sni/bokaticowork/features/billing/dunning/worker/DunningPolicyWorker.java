package com.sni.bokaticowork.features.billing.dunning.worker;

import com.sni.bokaticowork.features.billing.dunning.model.DunningNotice;
import com.sni.bokaticowork.features.billing.dunning.model.DunningStep;
import com.sni.bokaticowork.features.billing.dunning.service.DunningRunner;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * La relance parametree, chaque matin.
 *
 * <p>Boucle sur les factures echues, palier par palier · chaque palier dans sa propre transaction,
 * pour qu'un echec sur une facture n'arrete pas les autres.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DunningPolicyWorker {

    private final DunningRunner runner;

    public record Outcome(int documents, int notices, int failed) {
    }

    @Scheduled(cron = "${bokati.billing.dunning.cron:0 30 8 * * *}")
    public void scheduled() {
        try {
            run();
        } catch (Exception ex) {
            log.error("DunningPolicyWorker failed: {}", ex.getMessage(), ex);
        }
    }

    public Outcome run() {
        int documents = 0;
        int notices = 0;
        int failed = 0;
        for (BillingDocument document : runner.overdueDocuments()) {
            documents++;
            for (DunningStep step : runner.dueSteps(document)) {
                try {
                    DunningNotice notice = runner.execute(document, step);
                    if (notice.getOutcome() == DunningNotice.Outcome.FAILED) failed++; else notices++;
                } catch (RuntimeException ex) {
                    failed++;
                    log.warn("Relance {} palier {} · {}", document.getDocumentNumber(), step.getStepOrder(), ex.getMessage());
                }
            }
        }
        if (notices > 0 || failed > 0) {
            log.info("Relances · {} facture(s) echue(s), {} relance(s), {} echec(s)", documents, notices, failed);
        }
        return new Outcome(documents, notices, failed);
    }
}
