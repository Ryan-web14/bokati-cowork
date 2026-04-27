package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletHoldExpiryWorker {

    private final WalletHoldService walletHoldService;

    @Scheduled(fixedDelayString = "${bokati.payment.workers.wallet-hold-expiry-delay-ms:300000}")
    public void expireWalletHolds() {
        int expired = walletHoldService.expireDueHolds();
        if (expired > 0) {
            log.info("Expired {} wallet holds", expired);
        }
    }
}
