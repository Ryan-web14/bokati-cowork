package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Verification de coherence d'un portefeuille, isolee dans son propre bean.
 * <p>
 * Composant distinct de {@link WalletReconciliationWorker} et non methode privee de celui-ci :
 * {@code REQUIRES_NEW} passe par le proxy Spring, donc un auto-appel depuis le worker laisserait
 * chaque compte dans la transaction du lot et un seul ecart ferait tomber la passe entiere.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletReconciliationChecker {

    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final WalletRiskFlagService flagService;

    /**
     * @return {@code true} si le compte est coherent sur les deux controles
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean check(Long walletId) {
        WalletAccount wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null) {
            return true;
        }
        boolean consistent = true;

        // Controle 1 · repartition. Un ecart signale une mutation de solde faite hors de
        // WalletLedgerService, qui est le seul proprietaire legitime de ces colonnes.
        BigDecimal split = wallet.getAvailableBalance().add(wallet.getHeldBalance());
        if (wallet.getLedgerBalance().compareTo(split) != 0) {
            log.error("Ecart de repartition · portefeuille={} comptable={} disponible={} bloque={} (attendu comptable={})",
                    wallet.getWalletNumber(), wallet.getLedgerBalance(), wallet.getAvailableBalance(),
                    wallet.getHeldBalance(), split);
            consistent = false;
        }

        // Controle 2 · grand livre. Un ecart signale un solde modifie sans ecriture, ou l'inverse.
        BigDecimal fromLedger = ledgerRepository.sumLedgerImpact(walletId);
        if (wallet.getLedgerBalance().compareTo(fromLedger) != 0) {
            log.error("Ecart de grand livre · portefeuille={} comptable={} somme des ecritures={} difference={}",
                    wallet.getWalletNumber(), wallet.getLedgerBalance(), fromLedger,
                    wallet.getLedgerBalance().subtract(fromLedger));
            consistent = false;
        }

        // Un ecart ne se corrige pas, il se signale : le signalement ouvre une revue humaine, et
        // c'est elle qui decidera. Ecraser un solde pour faire taire un ecart effacerait la seule
        // trace de ce qui a mal tourne.
        if (!consistent) {
            flagService.raise(wallet, WalletRiskFlag.Type.INTEGRITY_BREAK, WalletRiskFlag.Severity.CRITICAL,
                    "Solde comptable " + wallet.getLedgerBalance() + " · somme des ecritures " + fromLedger
                            + " · disponible " + wallet.getAvailableBalance() + " + bloque " + wallet.getHeldBalance(),
                    wallet.getWalletNumber());
        }
        if (wallet.getLedgerBalance().signum() < 0 || wallet.getAvailableBalance().signum() < 0) {
            flagService.raise(wallet, WalletRiskFlag.Type.NEGATIVE_BALANCE, WalletRiskFlag.Severity.CRITICAL,
                    "Solde negatif · comptable " + wallet.getLedgerBalance() + ", disponible " + wallet.getAvailableBalance(),
                    wallet.getWalletNumber());
        }
        return consistent;
    }
}
