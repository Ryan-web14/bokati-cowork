package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassExpiryWorker {

    private final PassService passService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.pass-expiry-delay-ms:600000}")
    public void expirePasses() {
        int expired = passService.expirePasses();
        if (expired > 0) {
            log.info("Expired {} passes", expired);
        }
    }
}
