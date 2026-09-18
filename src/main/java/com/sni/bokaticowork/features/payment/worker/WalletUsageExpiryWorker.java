package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.transfer.service.WalletPaymentRequestService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ferme ce qui n'a pas abouti · transferts jamais confirmes, demandes de paiement echues.
 *
 * <p>Ni les uns ni les autres ne bloquent d'argent : un transfert non confirme n'a rien debite, une
 * demande n'a rien retenu. Ce passage sert la lisibilite de l'historique, pas la securite · la
 * confirmation verifie deja l'echeance a la volee.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletUsageExpiryWorker {

    private final WalletTransferService transferService;
    private final WalletPaymentRequestService paymentRequestService;

    @Scheduled(fixedDelayString = "${bokati.wallet.usage.expiry-delay-ms:600000}")
    public void expire() {
        int transfers = transferService.expireStale();
        int requests = paymentRequestService.expireStale();
        if (transfers > 0 || requests > 0) {
            log.info("Portefeuille · {} transfert(s) non confirme(s) et {} demande(s) echue(s) fermes", transfers, requests);
        }
    }
}
