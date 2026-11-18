package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarDays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Lot B1 (decision D1) : echeance de paiement d'une reservation en attente de paiement.
 *
 * <ul>
 *   <li>2 h par defaut ({@code bookings.payment-due.default-minutes}) ;</li>
 *   <li>creneau proche : au plus tard 30 min avant le debut ({@code ...before-start-minutes}) ;
 *   si ce moment est deja passe, au debut (paiement a l'arrivee) ;</li>
 *   <li>creneau a plus de 3 jours ({@code ...far-days}) : 24 h ({@code ...far-minutes}) ;</li>
 *   <li>une echeance un samedi, un dimanche ou un jour ferie passe au prochain jour ouvre a 10:00
 *   ({@code ...business-hour}), sans depasser la limite avant le debut.</li>
 * </ul>
 */
@Component
public class BookingPaymentDue {

    static final String PREFIX = "bookings.payment-due.";

    private PlatformSettings settings;
    private ResourceCalendarDays calendarDays;

    @Autowired(required = false)
    void setOptional(PlatformSettings settings, ResourceCalendarDays calendarDays) {
        this.settings = settings;
        this.calendarDays = calendarDays;
    }

    private int setting(String key, int fallback) {
        return settings == null ? fallback : settings.integer(PREFIX + key, fallback);
    }

    public LocalDateTime dueAt(LocalDateTime now, LocalDateTime start) {
        LocalDateTime latest = start.minusMinutes(Math.max(0, setting("before-start-minutes", 30)));
        if (!latest.isAfter(now)) {
            // Creneau imminent : le client regle a son arrivee.
            return start.isAfter(now) ? start : now;
        }
        boolean far = Duration.between(now, start).toDays() >= Math.max(1, setting("far-days", 3));
        LocalDateTime due = now.plusMinutes(far ? Math.max(1, setting("far-minutes", 1440)) : Math.max(1, setting("default-minutes", 120)));
        if (!businessDay(due)) {
            LocalDateTime next = due.toLocalDate().plusDays(1).atTime(LocalTime.of(Math.min(23, Math.max(0, setting("business-hour", 10))), 0));
            for (int guard = 0; guard < 10 && !businessDay(next); guard++) {
                next = next.plusDays(1);
            }
            due = next;
        }
        return due.isAfter(latest) ? latest : due;
    }

    private boolean businessDay(LocalDateTime at) {
        DayOfWeek day = at.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return calendarDays == null || !calendarDays.isHoliday(at.toLocalDate());
    }
}
