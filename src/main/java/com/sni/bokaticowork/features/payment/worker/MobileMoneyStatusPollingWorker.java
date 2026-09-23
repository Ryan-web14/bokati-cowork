package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneySupervisionService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On relit le statut jusqu'a ce que l'operateur tranche · on ne tranche jamais a sa place.
 *
 * <p>Une demande mobile money attend que le client saisisse son code. Il peut le faire dans la
 * minute, ou une heure plus tard en sortant de reunion. L'ancien worker abandonnait en
 * {@code FAILED} au bout de vingt-cinq minutes : un paiement confirme ensuite arrivait sur une
 * transaction deja declaree echouee et une intention deja annulee.</p>
 *
 * <p>Desormais chaque depot porte sa prochaine echeance de verification, rapprochee d'abord puis
 * espacee. Au bout du temps imparti sans reponse definitive, le depot passe {@code UNRESOLVED} ·
 * pas {@code FAILED} : la transaction reste en cours, l'intention reste payable, et quelqu'un est
 * prevenu pour rapprocher. Une reponse tardive de l'operateur reste toujours acceptee.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bokati.payment.pawaypay.enabled", havingValue = "true")
public class MobileMoneyStatusPollingWorker {

    private static final int BATCH = 100;

    private final MobileMoneySupervisionService supervision;
    private final PawapayDepositService depositService;

    /** Ce qu'une passe a donne · utile aux tests et au journal. */
    public record Sweep(int checked, int settled, int unresolved) {
    }

    @Scheduled(fixedDelayString = "${bokati.payment.pawaypay.polling-delay-ms:60000}")
    public void pollProcessingTransactions() {
        try {
            Sweep sweep = run();
            if (sweep.checked() > 0) {
                log.info("Depots mobile money · {} verifie(s), {} tranche(s), {} sans reponse",
                        sweep.checked(), sweep.settled(), sweep.unresolved());
            }
        } catch (Exception ex) {
            log.error("MobileMoneyStatusPollingWorker failed: {}", ex.getMessage(), ex);
        }
    }

    public Sweep run() {
        List<PawapayDeposit> due = supervision.due(BATCH);
        int settled = 0;
        int unresolved = 0;
        for (PawapayDeposit deposit : due) {
            try {
                MobileMoneySupervisionService.Verdict verdict = supervision.settle(deposit);
                if (verdict == MobileMoneySupervisionService.Verdict.SETTLED) settled++;
                if (verdict == MobileMoneySupervisionService.Verdict.UNRESOLVED) unresolved++;
            } catch (Exception ex) {
                // Un depot qui fait echouer sa relecture ne doit ni bloquer les suivants ni rester
                // en tete de file · on lui pose simplement la prochaine echeance.
                log.error("Verification du depot {} en erreur · reprise a la prochaine echeance",
                        deposit.getDepositId(), ex);
                depositService.scheduleNextCheck(deposit.getDepositId(), null);
            }
        }
        return new Sweep(due.size(), settled, unresolved);
    }
}
