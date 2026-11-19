package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.utils.time.AppTime;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingChannel;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Lot B9f : remplissage.
 *
 * <ul>
 *   <li><b>Derniere minute</b> : une reservation du portail qui commence dans les
 *   {@code bookings.last-minute.hours} (24) heures recoit la remise {@code bookings.last-minute.discount-rate}
 *   (0 : coupee) ; la liste des creneaux libres a venir est proposee au portail.</li>
 *   <li><b>Prolongation en un clic</b> : la fin est repoussee de N minutes, par le chemin du
 *   deplacement (capacite, politique, prix recalcule, hausse confirmee).</li>
 * </ul>
 */
@Slf4j
@Component
public class BookingFilling {

    static final String RATE_KEY = "bookings.last-minute.discount-rate";
    static final String HOURS_KEY = "bookings.last-minute.hours";

    private final BookingRepository bookings;
    private final ObjectProvider<BookingService> bookingService;
    private final ObjectProvider<BookingExtras> extras;

    private PlatformSettings settings;

    public BookingFilling(BookingRepository bookings, ObjectProvider<BookingService> bookingService, ObjectProvider<BookingExtras> extras) {
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.extras = extras;
    }

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    public int lastMinuteRate() {
        return settings == null ? 0 : Math.max(0, Math.min(90, settings.integer(RATE_KEY, 0)));
    }

    public int lastMinuteHours() {
        return settings == null ? 24 : Math.max(1, settings.integer(HOURS_KEY, 24));
    }

    /** Le creneau est-il de derniere minute (et la remise active) ? */
    public boolean isLastMinute(LocalDateTime start) {
        if (lastMinuteRate() <= 0 || start == null) {
            return false;
        }
        LocalDateTime now = AppTime.now();
        return start.isAfter(now) && !start.isAfter(now.plusHours(lastMinuteHours()));
    }

    /** Prix remise d'un montant, ou le montant tel quel hors derniere minute. */
    public BigDecimal discounted(BigDecimal amount) {
        if (amount == null || lastMinuteRate() <= 0) {
            return amount;
        }
        return amount.subtract(amount.multiply(BigDecimal.valueOf(lastMinuteRate())).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP));
    }

    /** A la creation au portail : remise de derniere minute, avant toute facture. */
    public void applyLastMinute(Booking booking) {
        if (booking.getChannel() != BookingChannel.PORTAL || !isLastMinute(booking.getStartedAt())
                || booking.getTotalAmount() == null || booking.getTotalAmount().signum() <= 0) {
            return;
        }
        BigDecimal discount = booking.getTotalAmount().subtract(discounted(booking.getTotalAmount()));
        if (discount.signum() > 0) {
            extras.getObject().applyManualDiscount(booking, discount, "Offre de dernière minute (-" + lastMinuteRate() + " %)");
        }
    }

    /** Prolonge la fin de {@code minutes} (reservation confirmee ou en cours) ; une hausse demande {@code acceptPriceChange}. */
    public BookingResponse extend(String bookingNumber, int minutes, Boolean acceptPriceChange) {
        if (minutes <= 0 || minutes > 24 * 60) {
            throw new BadRequestException("Prolongation de 1 à 1 440 minutes");
        }
        return bookingService.getObject().extend(bookingNumber, minutes, acceptPriceChange);
    }
}
