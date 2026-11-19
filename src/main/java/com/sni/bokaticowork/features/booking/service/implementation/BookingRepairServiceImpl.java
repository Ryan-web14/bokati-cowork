package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.features.booking.dto.response.BookingRepairResponse;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingHoldService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingRepairService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Reparation a la demande (lot B7, ecart 7). Avant : un evenement « inspecte » sur des
 * reservations prises au hasard, et toujours 0 reparation.
 *
 * <p>Rejoue maintenant, tout de suite, ce que les taches automatiques font a leur rythme, pour
 * rattraper un retard (serveur arrete, tache en echec) : options echues expirees, reservations non
 * reglees a l'echeance expirees, reservations pointees d'avance demarrees, absences et clotures
 * des reservations dont la fin est passee. Chaque reservation est traitee a part : un echec est
 * compte et n'arrete pas le reste.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingRepairServiceImpl implements BookingRepairService {

    private final BookingService bookingService;
    private final BookingHoldService holdService;

    @Override
    public BookingRepairResponse repair(int limit) {
        int batch = Math.max(1, Math.min(limit, 500));
        int[] failed = {0};
        int steps = safely(() -> holdService.expireDue(batch), failed)
                + safely(() -> bookingService.expireUnpaid(batch), failed)
                + safely(() -> bookingService.startCheckedInBookings(batch), failed);
        List<String> noShows = candidates(() -> bookingService.overdueNoShowCandidates(batch), failed);
        List<String> completions = candidates(() -> bookingService.overdueCompletionCandidates(batch), failed);
        int repaired = steps + each(noShows, bookingService::markOverdueNoShow, failed)
                + each(completions, bookingService::markOverdueCompleted, failed);
        int inspected = steps + noShows.size() + completions.size();
        log.info("Réparation des réservations : {} examinée(s), {} corrigée(s), {} échec(s)", inspected, repaired, failed[0]);
        return new BookingRepairResponse(inspected, repaired, failed[0]);
    }

    private static int safely(Supplier<Integer> step, int[] failed) {
        try {
            Integer done = step.get();
            return done == null ? 0 : done;
        } catch (RuntimeException ex) {
            log.error("Réparation des réservations : étape en échec : {}", ex.getMessage(), ex);
            failed[0]++;
            return 0;
        }
    }

    private static List<String> candidates(Supplier<List<String>> source, int[] failed) {
        try {
            return source.get();
        } catch (RuntimeException ex) {
            log.error("Réparation des réservations : recherche en échec : {}", ex.getMessage(), ex);
            failed[0]++;
            return List.of();
        }
    }

    private static int each(List<String> bookingNumbers, Predicate<String> action, int[] failed) {
        int done = 0;
        for (String bookingNumber : bookingNumbers) {
            try {
                if (action.test(bookingNumber)) {
                    done++;
                }
            } catch (RuntimeException ex) {
                log.error("Réparation de la réservation {} en échec : {}", bookingNumber, ex.getMessage(), ex);
                failed[0]++;
            }
        }
        return done;
    }
}
