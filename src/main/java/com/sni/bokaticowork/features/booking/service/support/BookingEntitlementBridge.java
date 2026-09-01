package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Traduit une reservation en mouvement de droit.
 *
 * <p>Le solde d'un droit est libelle dans l'unite de sa definition, pas dans celle de la
 * reservation. Traduire entre les deux est l'objet de ce composant, et c'est precisement ce qui
 * manquait : la quantite etait calculee dans l'unite de la reservation puis debitee telle quelle.
 * Cinq heures facturees en demi-journee retiraient <b>1</b> d'un droit compte en heures, et une
 * journee de dix heures en retirait <b>1</b> egalement.
 *
 * <p>Rien ne signalait l'ecart : ni {@code EntitlementService} ni {@code
 * EntitlementGrantBalanceOperator} ne lisent l'unite de la definition, et la requete qui choisit le
 * droit ne la filtre pas davantage. L'unite n'apparaissait que dans les reponses d'API.
 */
@Component
@RequiredArgsConstructor
public class BookingEntitlementBridge {

    private static final String REFERENCE_TYPE = "BOOKING";

    private final EntitlementService entitlementService;
    private final UsageRecordService usageRecordService;
    private final EntitlementDefinitionRepository definitionRepository;

    /**
     * Ce qu'une reservation retire au droit, exprime dans l'unite de celui-ci.
     *
     * @param displayUnit equivalent pour la ligne de reservation, dont l'unite est un
     *                    {@link ResourceBookingUnit} · les unites sans equivalent conservent celle
     *                    de la reservation, leur quantite valant alors 1
     */
    public record Charge(BigDecimal quantity, EntitlementUnit unit, ResourceBookingUnit displayUnit) {
    }

    /**
     * Quantite a debiter et unite dans laquelle elle s'exprime.
     *
     * <p>Publique pour que la ligne {@code ENTITLEMENT} de la reservation annonce exactement ce qui
     * est retire au droit · deux calculs separes finiraient par diverger.
     */
    public Charge charge(Booking booking) {
        EntitlementDefinition definition = definitionRepository
                .findByCodeIgnoreCase(booking.getEntitlementCode())
                .orElseThrow(() -> new BadRequestException(
                        "Droit introuvable : " + booking.getEntitlementCode()));

        EntitlementUnit unit = definition.getUnit();
        BigDecimal perBooking = switch (unit) {
            case HOUR -> BigDecimal.valueOf(minutes(booking))
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            case DAY -> BigDecimal.valueOf(
                    (long) Math.ceil((double) minutes(booking) / BookingPricingCalculator.DAY_MINUTES));
            // Un droit compte en passages ou en reservations se consomme a l'unite · la duree
            // n'entre pas en ligne de compte.
            case BOOKING, VISIT -> BigDecimal.ONE;
            // Aucune conversion n'existe depuis une duree. Debiter au jugé reviendrait a reproduire
            // le defaut que ce composant corrige, en silence et sur le solde d'un membre.
            case CREDIT, MEMBER, PERCENT, AMOUNT, BOOLEAN -> throw new BadRequestException(
                    "Le droit " + definition.getCode() + " est compte en " + unit
                            + " · une reservation ne sait pas s'y convertir. Rattachez cette ressource"
                            + " a un droit en HOUR, DAY, BOOKING ou VISIT.");
        };

        BigDecimal quantity = perBooking
                .multiply(BigDecimal.valueOf(booking.getQuantity() == null ? 1 : booking.getQuantity()))
                .setScale(4, RoundingMode.HALF_UP);
        return new Charge(quantity, unit, displayUnit(unit, booking));
    }

    public void reserve(Booking booking) {
        if (!StringUtils.hasText(booking.getEntitlementCode())) {
            throw new BadRequestException("Entitlement code is required for subscription or pass booking");
        }
        entitlementService.reserve(operation(booking, charge(booking).quantity(), "reserve", "Booking confirmed"));
    }

    public void consume(Booking booking) {
        if (!StringUtils.hasText(booking.getEntitlementCode())) {
            return;
        }
        Charge charge = charge(booking);
        entitlementService.consume(operation(booking, charge.quantity(), "consume", "Booking completed"));
        usageRecordService.record(new CreateUsageRecordRequest(
                booking.getOwnerType(),
                booking.getOwnerCode(),
                booking.getEntitlementCode(),
                charge.quantity(),
                charge.unit(),
                REFERENCE_TYPE,
                booking.getBookingNumber(),
                false,
                false,
                null,
                booking.getCurrency(),
                Instant.now(),
                booking.getMetadataJson()
        ));
    }

    public void release(Booking booking) {
        if (!StringUtils.hasText(booking.getEntitlementCode())) {
            return;
        }
        entitlementService.release(operation(booking, charge(booking).quantity(), "release", "Booking cancelled"));
    }

    private long minutes(Booking booking) {
        return booking.getDurationMinutes() == null ? 0L : booking.getDurationMinutes();
    }

    private ResourceBookingUnit displayUnit(EntitlementUnit unit, Booking booking) {
        return switch (unit) {
            case HOUR -> ResourceBookingUnit.HOUR;
            case DAY -> ResourceBookingUnit.DAY;
            default -> booking.getBookingUnit();
        };
    }

    private EntitlementOperationRequest operation(Booking booking, BigDecimal quantity, String action, String reason) {
        return new EntitlementOperationRequest(
                booking.getOwnerType(),
                booking.getOwnerCode(),
                booking.getEntitlementCode(),
                quantity,
                REFERENCE_TYPE,
                booking.getBookingNumber(),
                "booking:" + booking.getBookingNumber() + ":" + action,
                reason
        );
    }
}
