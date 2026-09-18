package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.security.service.WalletConfirmationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ferme les demandes de confirmation echues.
 *
 * <p>La confirmation verifie deja l'echeance a la volee : une demande perimee ne peut pas etre
 * utilisee, worker ou pas. Ce passage ne sert donc pas a la securite mais a la lisibilite · sans
 * lui, une table pleine de {@code PENDING} qui ne le sont plus depuis des semaines rendrait toute
 * lecture du journal de confirmation trompeuse, et une alerte sur « les confirmations en attente »
 * impossible a regler.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletConfirmationExpiryWorker {

    private final WalletConfirmationService confirmationService;

    @Scheduled(fixedDelayString = "${bokati.wallet.confirmation.expiry-delay-ms:300000}")
    public void expire() {
        int closed = confirmationService.expireStale();
        if (closed > 0) {
            log.info("Portefeuille · {} demande(s) de confirmation echue(s) fermee(s)", closed);
        }
    }
}
