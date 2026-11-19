package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.utils.time.AppTime;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Lot B9c : fiabilite d'un client et acompte.
 *
 * <p>Score sur 100 calcule sur {@code bookings.reliability.window-days} (180) jours : moins
 * {@code ...no-show-penalty} (20) par absence, moins {@code ...late-cancel-penalty} (10) par
 * annulation tardive (moins de {@code ...late-cancel-hours}, 24 h, avant le debut). Sous
 * {@code bookings.reliability.threshold} (60), le client doit garantir sa reservation :
 * {@code bookings.reliability.mode} {@code DEPOSIT} (acompte de {@code bookings.deposit.rate} %
 * pour confirmer, le reste sur place) ou {@code PREPAY} (pas de rendez-vous regle sur place,
 * paiement complet avant confirmation).</p>
 */
@Component
@RequiredArgsConstructor
public class BookingReliability {

    public static final String DEPOSIT = "DEPOSIT";
    public static final String PREPAY = "PREPAY";

    public record Score(SubscriberType ownerType, String ownerCode, int score, long bookings, long noShows, long lateCancellations,
                        int windowDays, int threshold, boolean guaranteeRequired, String guarantee, BigDecimal depositRate) {
    }

    private final BookingRepository bookings;

    private PlatformSettings settings;

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public Score score(SubscriberType ownerType, String ownerCode) {
        int window = Math.max(1, integer("bookings.reliability.window-days", 180));
        int threshold = integer("bookings.reliability.threshold", 60);
        String mode = PREPAY.equalsIgnoreCase(text("bookings.reliability.mode", DEPOSIT)) ? PREPAY : DEPOSIT;
        BigDecimal rate = depositRate();
        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            return new Score(ownerType, ownerCode, 100, 0, 0, 0, window, threshold, false, null, rate);
        }
        LocalDateTime now = AppTime.now();
        LocalDateTime from = now.minusDays(window);
        long total = bookings.countForOwnerBetween(ownerType.name(), ownerCode, from, now);
        long noShows = bookings.countNoShowsForOwnerBetween(ownerType.name(), ownerCode, from, now);
        long late = bookings.countLateCancellations(ownerType.name(), ownerCode, from,
                Math.max(0, integer("bookings.reliability.late-cancel-hours", 24)), AppTime.zone().getId());
        int score = (int) Math.max(0, 100 - noShows * integer("bookings.reliability.no-show-penalty", 20)
                - late * integer("bookings.reliability.late-cancel-penalty", 10));
        boolean required = threshold > 0 && score < threshold;
        return new Score(ownerType, ownerCode, score, total, noShows, late, window, threshold, required, required ? mode : null, rate);
    }

    /** Acompte a demander pour ce client sur ce total, ou {@code null}. */
    public BigDecimal depositFor(SubscriberType ownerType, String ownerCode, BigDecimal total) {
        Score score = score(ownerType, ownerCode);
        if (!score.guaranteeRequired() || !DEPOSIT.equals(score.guarantee())) {
            return null;
        }
        return deposit(total, score.depositRate());
    }

    /** Le client doit-il payer en entier avant confirmation (pas de reglement sur place) ? */
    public boolean prepayRequired(SubscriberType ownerType, String ownerCode) {
        Score score = score(ownerType, ownerCode);
        return score.guaranteeRequired() && PREPAY.equals(score.guarantee());
    }

    public static BigDecimal deposit(BigDecimal total, BigDecimal rate) {
        if (total == null || total.signum() <= 0 || rate == null || rate.signum() <= 0) {
            return null;
        }
        BigDecimal capped = rate.min(BigDecimal.valueOf(100));
        return total.multiply(capped).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    public BigDecimal depositRate() {
        return BigDecimal.valueOf(Math.max(0, integer("bookings.deposit.rate", 30)));
    }

    private int integer(String key, int fallback) {
        return settings == null ? fallback : settings.integer(key, fallback);
    }

    private String text(String key, String fallback) {
        return settings == null ? fallback : settings.text(key, fallback);
    }
}
