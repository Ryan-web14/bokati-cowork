package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.features.accounting.service.implementation.CommitmentService;
import com.sni.bokaticowork.features.accounting.service.implementation.LoanService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Tenue quotidienne du financement (lot F17) : echeances d'emprunt ecrites seules a leur date,
 * alertes avant echeance, loyers restant dus des baux, expirations et alertes du hors bilan,
 * rapprochement du registre et des comptes 90.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancingWorker {

    private final LoanService loans;
    private final CommitmentService commitments;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;

    @Scheduled(cron = "${accounting.financing.cron:0 45 6 * * *}", zone = "Africa/Brazzaville")
    public void run() {
        if (!settings.enabled()) {
            return;
        }
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Brazzaville"));
        try {
            log.info("Financement : {} echeance(s) d'emprunt ecrite(s), {} alerte(s)", loans.postDue(today), loans.alerts(today));
        } catch (RuntimeException exception) {
            log.warn("Financement : echeances d'emprunt en echec : {}", exception.getMessage());
            notifier.notifyAccountants("Echeances d'emprunt non ecrites", "L'ecriture automatique des echeances d'emprunt a echoue : "
                    + exception.getMessage(), null, "/accounting/loans", null);
        }
        try {
            log.info("Hors bilan : {}", commitments.refresh(today));
        } catch (RuntimeException exception) {
            log.warn("Hors bilan : tenue quotidienne en echec : {}", exception.getMessage());
        }
    }
}
