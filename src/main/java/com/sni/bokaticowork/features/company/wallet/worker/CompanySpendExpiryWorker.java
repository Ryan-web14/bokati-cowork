package com.sni.bokaticowork.features.company.wallet.worker;

import com.sni.bokaticowork.features.company.wallet.service.CompanyWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Annule les depenses que le gerant n'a pas validees dans le delai, et rend leurs fonds.
 *
 * <p>Une transaction par depense · un echec n'arrete pas le lot, la depense est reprise au
 * passage suivant.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanySpendExpiryWorker {

    private static final int BATCH = 50;

    private final CompanyWalletService walletService;

    @Scheduled(fixedDelayString = "${bokati.company.wallet.approval-expiry-delay-ms:600000}")
    public void expirePendingSpends() {
        int expired = 0;
        for (Long spendId : walletService.expiredPendingIds(BATCH)) {
            try {
                if (walletService.expire(spendId)) {
                    expired++;
                }
            } catch (RuntimeException ex) {
                log.warn("Depense {} · expiration impossible, reprise au prochain passage : {}", spendId, ex.getMessage());
            }
        }
        if (expired > 0) {
            log.info("{} depense(s) sans validation du gerant annulee(s), fonds rendus", expired);
        }
    }
}
