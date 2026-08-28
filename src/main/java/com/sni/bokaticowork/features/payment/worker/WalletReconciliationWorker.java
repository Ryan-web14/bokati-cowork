package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rapprochement quotidien des portefeuilles.
 * <p>
 * Les contraintes {@code ck_wallet_account_non_negative} et {@code ck_wallet_account_balance_split}
 * sont posees NOT VALID : elles protegent les ecritures futures mais ne rejouent pas la validation
 * sur l'historique, pour qu'un deploiement ne puisse pas echouer sur une ligne heritee incoherente.
 * Ce worker est la contrepartie : il verifie l'historique et signale les ecarts au lieu de bloquer.
 * <p>
 * Il ne corrige rien : un ajustement automatique de solde masquerait la cause. Il journalise en
 * ERROR avec le detail chiffre, actionnable manuellement via une ecriture ADJUSTMENT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletReconciliationWorker {

    private final WalletAccountRepository walletRepository;
    private final WalletReconciliationChecker checker;

    @Value("${bokati.payment.workers.wallet-reconciliation-enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${bokati.payment.workers.wallet-reconciliation-cron:0 15 3 * * *}")
    public void reconcile() {
        if (!enabled) {
            return;
        }
        List<Long> walletIds = walletRepository.findAllOpenIds();
        int drifted = 0;
        for (Long walletId : walletIds) {
            try {
                if (!checker.check(walletId)) {
                    drifted++;
                }
            } catch (Exception ex) {
                // Un compte en erreur ne doit pas interrompre le lot : le but est de voir
                // l'ensemble des ecarts en une passe, pas de s'arreter au premier.
                log.error("Rapprochement du portefeuille {} en echec", walletId, ex);
                drifted++;
            }
        }
        if (drifted == 0) {
            log.info("Rapprochement portefeuilles · {} compte(s) verifie(s), aucun ecart", walletIds.size());
        } else {
            log.error("Rapprochement portefeuilles · {} ecart(s) sur {} compte(s) verifie(s)",
                    drifted, walletIds.size());
        }
    }
}
