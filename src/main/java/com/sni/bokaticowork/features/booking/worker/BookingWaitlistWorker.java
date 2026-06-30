package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.features.booking.service.interfaces.BookingWaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingWaitlistWorker {

    private final BookingWaitlistService waitlistService;

    @Scheduled(fixedDelayString = "${bokati.booking.waitlist.worker-delay-ms:300000}")
    public void process() {
        try {
            int expired = waitlistService.expireOffers(100);
            int promoted = waitlistService.promoteAvailable(100);
            if (expired > 0 || promoted > 0) {
                log.info("Booking waitlist processed: expired={}, promoted={}", expired, promoted);
            }
        } catch (Exception ex) {
            log.error("BookingWaitlistWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
