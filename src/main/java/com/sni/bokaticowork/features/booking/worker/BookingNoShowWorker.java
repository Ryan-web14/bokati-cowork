package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gère automatiquement le cycle de vie des réservations passées :
 *
 *  - NO_SHOW  : créneaux dépassés sans check-in ni démarrage admin
 *  - COMPLETED: créneaux dépassés après check-in ou démarrage admin
 *
 * Cadence configurable via {@code bokati.booking.lifecycle.worker-delay-ms}
 * (défaut : 60 s).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNoShowWorker {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${bokati.booking.lifecycle.worker-delay-ms:${bokati.booking.no-show.worker-delay-ms:60000}}")
    public void processOverdueBookings() {
        try {
            int noShows = bookingService.markOverdueNoShows(200);
            if (noShows > 0) {
                log.info("Auto no-show: {} réservation(s) marquée(s)", noShows);
            }
            int completed = bookingService.markOverdueCompleted(200);
            if (completed > 0) {
                log.info("Auto-terminée: {} réservation(s) clôturée(s)", completed);
            }
        } catch (Exception ex) {
            log.error("BookingNoShowWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
