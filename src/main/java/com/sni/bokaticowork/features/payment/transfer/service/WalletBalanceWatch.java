package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Surveille le passage du solde sous le seuil fixe par le titulaire.
 *
 * <p>Appele apres chaque mutation de solde, depuis l'unique chemin d'ecriture. L'alerte part une
 * fois quand le solde passe sous le seuil, et se rearme quand il repasse au-dessus · sans cela un
 * titulaire a 500 F recevrait un courriel par cafe.</p>
 *
 * <p>Ne touche qu'aux deux colonnes d'alerte du compte, jamais aux soldes : c'est le grand livre
 * qui les tient, et il est le seul a le faire.</p>
 */
@Component
@RequiredArgsConstructor
public class WalletBalanceWatch {

    private final WalletNotifier notifier;
    private final WalletRiskFlagService flagService;

    public void afterBalanceChange(WalletAccount wallet) {
        wakeIfDormant(wallet);
        BigDecimal threshold = wallet.getLowBalanceThreshold();
        if (threshold == null || wallet.getAvailableBalance() == null) {
            return;
        }
        boolean below = wallet.getAvailableBalance().compareTo(threshold) < 0;
        if (below && wallet.getLowBalanceAlertedAt() == null) {
            wallet.setLowBalanceAlertedAt(Instant.now());
            notifier.lowBalance(wallet);
        } else if (!below && wallet.getLowBalanceAlertedAt() != null) {
            // Le solde est remonte · la prochaine descente meritera une nouvelle alerte.
            wallet.setLowBalanceAlertedAt(null);
        }
    }

    /**
     * Un portefeuille dormant qui bouge se reveille, et cela se signale.
     *
     * <p>Ce n'est pas suspect en soi · un titulaire revient. Mais un compte oublie est aussi le
     * compte ideal pour qui l'a trouve, et la revue doit pouvoir le regarder avant que le solde ne
     * soit parti.</p>
     */
    private void wakeIfDormant(WalletAccount wallet) {
        if (wallet.getDormantSince() == null) {
            return;
        }
        // Apres commit : on est ici sous le verrou exclusif du compte, et l'insertion du
        // signalement en aurait besoin d'une cle partagee · les deux s'attendraient.
        flagService.raiseAfterCommit(wallet, WalletRiskFlag.Type.DORMANT_REACTIVATION, WalletRiskFlag.Severity.MEDIUM,
                "Sans mouvement depuis le " + wallet.getDormantSince(), null);
        wallet.setDormantSince(null);
    }
}
