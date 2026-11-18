package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lot B2 : couverture partielle d'une reservation par les heures incluses d'un abonnement ou d'un
 * pass.
 *
 * <p>Quand le solde du droit ne couvre qu'une partie de la reservation et que le plan n'autorise
 * pas le depassement, les minutes couvertes sont prises sur le droit et le reste est facture au
 * prorata du prix. Avant, la reservation etait soit couverte en entier, soit refusee.</p>
 */
@Slf4j
@Component
public class BookingPartialCoverage {

    private final BookingEntitlementBridge entitlementBridge;
    private EntitlementService entitlementService;

    public BookingPartialCoverage(@Lazy BookingEntitlementBridge entitlementBridge) {
        this.entitlementBridge = entitlementBridge;
    }

    @Autowired(required = false)
    void setEntitlementService(@Lazy EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    /**
     * Minutes couvertes par le droit, ou {@code null} si la reservation est couverte en entier,
     * n'a aucun solde, ou si le depassement est permis (il est alors facture par le module des
     * depassements).
     */
    public Integer coveredMinutes(Booking booking) {
        if (entitlementService == null || booking.getPaymentMode() == null || !booking.getPaymentMode().usesEntitlement()
                || !StringUtils.hasText(booking.getEntitlementCode()) || booking.getDurationMinutes() == null
                || booking.getDurationMinutes() <= 0) {
            return null;
        }
        try {
            BookingEntitlementBridge.Charge charge = entitlementBridge.charge(booking);
            BigDecimal needed = charge.quantity();
            if (needed.signum() <= 0 || entitlementBridge.overageAllows(booking)) {
                return null;
            }
            BigDecimal remaining = remaining(booking);
            if (remaining.signum() <= 0 || remaining.compareTo(needed) >= 0) {
                return null;
            }
            int quantity = booking.getQuantity() == null || booking.getQuantity() < 1 ? 1 : booking.getQuantity();
            long minutes = switch (charge.unit()) {
                case HOUR -> remaining.multiply(BigDecimal.valueOf(60)).divide(BigDecimal.valueOf(quantity), 0, RoundingMode.DOWN).longValue();
                case DAY -> remaining.divide(BigDecimal.valueOf(quantity), 0, RoundingMode.DOWN).longValue() * BookingPricingCalculator.DAY_MINUTES;
                default -> 0L;
            };
            if (minutes <= 0 || minutes >= booking.getDurationMinutes()) {
                return null;
            }
            return (int) minutes;
        } catch (RuntimeException ex) {
            log.info("Couverture partielle de {} non calculee : {}", booking.getBookingNumber(), ex.getMessage());
            return null;
        }
    }

    private BigDecimal remaining(Booking booking) {
        SubscriberType ownerType = booking.getEntitlementOwnerType() != null ? booking.getEntitlementOwnerType() : booking.getOwnerType();
        String ownerCode = booking.getEntitlementOwnerType() != null ? booking.getEntitlementOwnerCode() : booking.getOwnerCode();
        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            return BigDecimal.ZERO;
        }
        return entitlementService.balances(ownerType, ownerCode).stream()
                .filter(grant -> booking.getEntitlementCode().equalsIgnoreCase(grant.entitlementCode()))
                .map(EntitlementGrantResponse::quantityRemaining)
                .filter(value -> value != null && value.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Part du prix a facturer : les minutes non couvertes, au prorata. */
    public static BigDecimal uncoveredAmount(BigDecimal fullPrice, int durationMinutes, int coveredMinutes) {
        if (fullPrice == null || durationMinutes <= 0) {
            return BigDecimal.ZERO;
        }
        int uncovered = Math.max(0, durationMinutes - coveredMinutes);
        return fullPrice.multiply(BigDecimal.valueOf(uncovered)).divide(BigDecimal.valueOf(durationMinutes), 0, RoundingMode.HALF_UP);
    }
}
