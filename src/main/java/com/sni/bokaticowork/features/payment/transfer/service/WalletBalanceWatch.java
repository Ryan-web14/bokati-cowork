package com.sni.bokaticowork.features.payment.transfer.service;

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

    public void afterBalanceChange(WalletAccount wallet) {
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
}
