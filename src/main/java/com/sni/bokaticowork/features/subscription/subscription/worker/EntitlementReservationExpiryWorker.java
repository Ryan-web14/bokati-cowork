package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntitlementReservationExpiryWorker {

    private final EntitlementService entitlementService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.reservation-expiry-delay-ms:300000}")
    public void expireReservations() {
        try {
            int expired = entitlementService.expireReservations();
            if (expired > 0) {
                log.info("Expired {} entitlement reservations", expired);
            }
        } catch (Exception ex) {
            log.error("EntitlementReservationExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
