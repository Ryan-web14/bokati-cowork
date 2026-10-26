package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEntitlementBridge {

    private static final String REFERENCE_TYPE = "BOOKING";

    private final EntitlementService entitlementService;
    private final UsageRecordService usageRecordService;
    private final EntitlementDefinitionRepository definitionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BookingOverageGuard overageGuard;

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

    /**
     * Retient le droit, ou laisse passer en depassement.
     *
     * <p>Un solde insuffisant n'est pas toujours un refus. Si le contexte de paiement a deja admis
     * la reservation, c'est que le plan autorise le depassement · echouer ici annulerait une
     * reservation que le systeme vient d'accepter, et le client verrait une erreur apres avoir vu
     * une confirmation.</p>
     *
     * <p>La consommation, elle, a bien lieu : elle est enregistree comme usage au moment de la
     * cloture, et c'est ce que le module de depassement transforme en facture.</p>
     */
    public void reserve(Booking booking) {
        if (!StringUtils.hasText(booking.getEntitlementCode())) {
            throw new BadRequestException("Entitlement code is required for subscription or pass booking");
        }
        EntitlementOperationRequest request = operation(booking, charge(booking).quantity(), "reserve",
                "Booking confirmed", holdUntil(booking));
        // On demande avant d'agir · rattraper le refus ne suffisait pas, l'exception levee par le
        // service des droits avait deja condamne la transaction de la reservation, et le
        // depassement echouait a la validation.
        if (!entitlementService.check(request).allowed() && overageAllowed(booking)) {
            log.info("Reservation {} · droit {} epuise, retenue impossible, passage en depassement",
                    booking.getBookingNumber(), booking.getEntitlementCode());
            return;
        }
        entitlementService.reserve(request);
    }

    /**
     * La retenue tient jusqu'apres la fin de la reservation · une semaine de marge pour la cloture.
     *
     * <p>Elle expirait au bout de quinze minutes : le droit d'une reservation prise trois jours a
     * l'avance revenait au solde le jour meme, le membre pouvait le depenser ailleurs, et la cloture
     * echouait faute de solde.</p>
     */
    public static Instant holdUntil(Booking booking) {
        if (booking.getEndedAt() == null) {
            return null;
        }
        return booking.getEndedAt().plusDays(7).atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    /**
     * Le depassement est-il permis pour cette reservation.
     *
     * <p>Interroge la politique du plan de l'abonnement porteur. Un pass n'a pas de politique de
     * depassement : son solde est ce qui a ete achete, et le depasser reviendrait a offrir ce qui
     * n'a pas ete vendu.</p>
     */
    private boolean overageAllowed(Booking booking) {
        if (!StringUtils.hasText(booking.getSubscriptionNumber())) {
            return false;
        }
        return subscriptionRepository.findBySubscriptionNumber(booking.getSubscriptionNumber())
                .map(subscription -> overageGuard.allows(subscription.getPlanVersion(), booking.getEntitlementCode()))
                .orElse(false);
    }

    /**
     * Consomme le droit, ou laisse la consommation au seul enregistrement d'usage.
     *
     * <p>En depassement, il n'y a plus rien a decompter · le solde est deja a zero. L'usage, lui,
     * est enregistre dans tous les cas, et c'est lui que le module de depassement lit pour produire
     * la charge puis la facture. Sans cet enregistrement, les heures supplementaires seraient
     * consommees sans jamais etre facturees.</p>
     */
    public boolean consume(Booking booking) {
        if (!StringUtils.hasText(booking.getEntitlementCode())) {
            return false;
        }
        Charge charge = charge(booking);
        EntitlementOperationRequest request = operation(booking, charge.quantity(), "consume", "Booking completed", null);
        boolean overage = false;
        if (entitlementService.coverable(request)) {
            entitlementService.consume(request);
        } else if (overageAllowed(booking)) {
            overage = true;
            log.info("Reservation {} · solde epuise, la consommation passe en depassement",
                    booking.getBookingNumber());
        } else {
            // Ni retenue ni solde, et pas de depassement permis · la cloture ne doit pas en mourir.
            // Elle echouait, et avec elle tout le lot du worker, a chaque passage : la reservation
            // restait confirmee pour toujours et les suivantes n'etaient plus jamais traitees.
            log.warn("Reservation {} · droit {} non decompte a la cloture : ni retenue active ni solde suffisant ({} demande)",
                    booking.getBookingNumber(), booking.getEntitlementCode(), charge.quantity().stripTrailingZeros().toPlainString());
            return false;
        }

        // Le drapeau de consommation decide de tout ce qui suit, et c'est ici que le depassement se
        // jouait sans jamais aboutir.
        //
        // En temps normal il vaut faux : le droit vient d'etre decompte juste au-dessus, et laisser
        // l'enregistrement d'usage le decompter une seconde fois retirerait deux fois la meme heure.
        //
        // En depassement, rien n'a ete decompte · le solde etait deja a zero. Le laisser a faux
        // faisait sortir processUsage des sa premiere ligne, donc aucune politique consultee,
        // aucune charge creee, aucune facture emise. Les heures supplementaires etaient consommees
        // et offertes.
        usageRecordService.record(new CreateUsageRecordRequest(
                booking.getOwnerType(),
                booking.getOwnerCode(),
                booking.getEntitlementCode(),
                charge.quantity(),
                charge.unit(),
                REFERENCE_TYPE,
                booking.getBookingNumber(),
                overage,
                false,
                null,
                booking.getCurrency(),
                Instant.now(),
                booking.getMetadataJson()
        ));
        return true;
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
        return operation(booking, quantity, action, reason, null);
    }

    private EntitlementOperationRequest operation(Booking booking, BigDecimal quantity, String action, String reason,
                                                  Instant holdUntil) {
        return new EntitlementOperationRequest(
                booking.getOwnerType(),
                booking.getOwnerCode(),
                booking.getEntitlementCode(),
                quantity,
                REFERENCE_TYPE,
                booking.getBookingNumber(),
                "booking:" + booking.getBookingNumber() + ":" + action,
                reason,
                holdUntil
        );
    }
}
