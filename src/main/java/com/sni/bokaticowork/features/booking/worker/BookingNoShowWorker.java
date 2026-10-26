package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Gère automatiquement le cycle de vie des réservations passées :
 *
 *  - IN_PROGRESS: réservations pointées d'avance dont l'heure est arrivée
 *  - NO_SHOW  : créneaux dépassés sans check-in ni démarrage admin
 *  - COMPLETED: créneaux dépassés après check-in ou démarrage admin
 *
 * Chaque réservation est traitée dans sa propre transaction · une seule en échec n'arrête plus
 * les autres, ni les étapes suivantes.
 *
 * Cadence configurable via {@code bokati.booking.lifecycle.worker-delay-ms}
 * (défaut : 60 s).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNoShowWorker {

    static final int BATCH = 200;

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${bokati.booking.lifecycle.worker-delay-ms:${bokati.booking.no-show.worker-delay-ms:60000}}")
    public void processOverdueBookings() {
        try {
            int started = bookingService.startCheckedInBookings(BATCH);
            if (started > 0) {
                log.info("Auto-demarrage : {} réservation(s) pointée(s) d'avance passée(s) en cours", started);
            }
        } catch (Exception ex) {
            log.error("Auto-demarrage des reservations en echec : {}", ex.getMessage(), ex);
        }

        int noShows = each("absence", () -> bookingService.overdueNoShowCandidates(BATCH), bookingService::markOverdueNoShow);
        if (noShows > 0) {
            log.info("Auto no-show: {} réservation(s) marquée(s)", noShows);
        }
        int completed = each("cloture", () -> bookingService.overdueCompletionCandidates(BATCH), bookingService::markOverdueCompleted);
        if (completed > 0) {
            log.info("Auto-terminée: {} réservation(s) clôturée(s)", completed);
        }
    }

    /** Applique l'action a chaque reservation · un echec est consigne, et on passe a la suivante. */
    static int each(String step, Supplier<List<String>> candidates, Predicate<String> action) {
        List<String> bookingNumbers;
        try {
            bookingNumbers = candidates.get();
        } catch (Exception ex) {
            log.error("Recherche des reservations a traiter ({}) en echec : {}", step, ex.getMessage(), ex);
            return 0;
        }
        int done = 0;
        for (String bookingNumber : bookingNumbers) {
            try {
                if (action.test(bookingNumber)) {
                    done++;
                }
            } catch (Exception ex) {
                log.error("Reservation {} · {} automatique en echec : {}", bookingNumber, step, ex.getMessage(), ex);
            }
        }
        return done;
    }
}
