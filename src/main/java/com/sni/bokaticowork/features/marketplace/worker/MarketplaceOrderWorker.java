package com.sni.bokaticowork.features.marketplace.worker;

import com.sni.bokaticowork.features.marketplace.service.MarketplaceOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fait avancer les commandes · reglees par un chemin qui ne previent pas la commande (caisse,
 * portefeuille de l'organisation), echues, ou dont un article reste a sortir du stock.
 *
 * <p>Une transaction par commande · un echec n'arrete pas le lot.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketplaceOrderWorker {

    private static final int BATCH = 100;

    private final MarketplaceOrderService orderService;

    @Scheduled(fixedDelayString = "${bokati.marketplace.sync-delay-ms:60000}", initialDelayString = "${bokati.marketplace.sync-initial-delay-ms:60000}")
    public void syncPendingOrders() {
        int changed = 0;
        for (String orderNumber : orderService.numbersToSync(BATCH)) {
            try {
                if (orderService.sync(orderNumber)) {
                    changed++;
                }
            } catch (RuntimeException ex) {
                log.warn("Commande {} · mise a jour impossible, reprise au prochain passage : {}", orderNumber, ex.getMessage());
            }
        }
        if (changed > 0) {
            log.info("{} commande(s) de la marketplace mise(s) a jour", changed);
        }
    }
}
