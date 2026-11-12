package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.Trigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
public class BookingNoShowWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.booking.lifecycle.worker-delay-ms";
    private static final String LEGACY_DELAY_KEY = "bokati.booking.no-show.worker-delay-ms";
    private static final long DEFAULT_DELAY_MS = 60_000L;
    /** Meme plancher que SettingTriggers : une valeur aberrante ne sature pas le planificateur. */
    private static final long MIN_DELAY_MS = 1_000L;

    private ObjectProvider<BookingNoShowWorker> self;
    private PlatformSettings platformSettings;

    static final int BATCH = 200;

    private final BookingService bookingService;

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

    @Autowired
    void setSelf(ObjectProvider<BookingNoShowWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().processOverdueBookings(), delayTrigger());
    }

    /** Delai fixe relu a chaque passage : la cle lifecycle, sinon l'ancienne cle no-show, sinon 60 s. */
    private Trigger delayTrigger() {
        return context -> {
            long delay = Math.max(MIN_DELAY_MS, delayMs());
            Instant last = context.lastCompletion();
            return last == null ? context.getClock().instant() : last.plusMillis(delay);
        };
    }

    private long delayMs() {
        if (platformSettings == null) {
            return DEFAULT_DELAY_MS;
        }
        return platformSettings.number(DELAY_KEY, platformSettings.number(LEGACY_DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
