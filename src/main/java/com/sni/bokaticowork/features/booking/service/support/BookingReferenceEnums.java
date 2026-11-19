package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.reference.service.ReferenceEnumRegistry;
import com.sni.bokaticowork.features.booking.enums.BookingChannel;
import com.sni.bokaticowork.features.booking.enums.BookingHoldStatus;
import com.sni.bokaticowork.features.booking.enums.BookingLineType;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantRole;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantStatus;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentStatus;
import com.sni.bokaticowork.features.booking.enums.BookingRecurrenceFrequency;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import org.springframework.stereotype.Component;

/**
 * Valeurs fixes des reservations, declarees au registre commun (lot L0) : lues par
 * {@code GET /reference/enums?module=BOOKING} et par {@code GET /bookings/reference} (lot B7).
 */
@Component
public class BookingReferenceEnums implements ReferenceEnumRegistry.Contributor {

    public static final String MODULE = "BOOKING";

    @Override
    public void contribute(ReferenceEnumRegistry.Builder registry) {
        registry.add(MODULE, "STATUS", "Statut de la réservation", BookingStatus.class, status -> capitalize(status.label()))
                .add(MODULE, "PAYMENT_MODE", "Mode de règlement", BookingPaymentMode.class, BookingReferenceEnums::paymentMode)
                .add(MODULE, "PAYMENT_STATUS", "État du règlement", BookingPaymentStatus.class, BookingPaymentStatus::getLabel)
                // PORTAL et TEAM sont poses par le serveur ; l'equipe choisit parmi les autres (lot B5).
                .add(MODULE, "CHANNEL", "Canal", BookingChannel.class, BookingReferenceEnums::channel,
                        channel -> channel != BookingChannel.PORTAL && channel != BookingChannel.TEAM)
                .add(MODULE, "PARTICIPANT_ROLE", "Rôle du participant", BookingParticipantRole.class, BookingReferenceEnums::participantRole)
                .add(MODULE, "PARTICIPANT_STATUS", "État du participant", BookingParticipantStatus.class, BookingReferenceEnums::participantStatus)
                .add(MODULE, "RECURRENCE", "Fréquence d'une série", BookingRecurrenceFrequency.class, BookingReferenceEnums::frequency)
                .add(MODULE, "HOLD_STATUS", "État d'une pré-réservation", BookingHoldStatus.class, BookingReferenceEnums::holdStatus)
                .add(MODULE, "WAITLIST_STATUS", "État d'une inscription en liste d'attente", BookingWaitlistStatus.class,
                        BookingReferenceEnums::waitlistStatus)
                .add(MODULE, "LINE_TYPE", "Type de ligne", BookingLineType.class, BookingReferenceEnums::lineType, type -> false)
                .add(MODULE, "OPTION_KIND", "Type d'option à la création", java.util.List.of(
                        new com.sni.bokaticowork.core.reference.dto.ReferenceDtos.EnumValue("SERVICE", "Service du catalogue", null, 10, true),
                        new com.sni.bokaticowork.core.reference.dto.ReferenceDtos.EnumValue("AMENITY", "Équipement optionnel", null, 20, true)));
    }

    static String capitalize(String text) {
        return text == null || text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    static String paymentMode(BookingPaymentMode mode) {
        return switch (mode) {
            case DIRECT -> "Paiement direct";
            case WALLET -> "Portefeuille";
            case SUBSCRIPTION -> "Abonnement";
            case PASS -> "Pass";
        };
    }

    static String channel(BookingChannel channel) {
        return switch (channel) {
            case PORTAL -> "Espace client";
            case TEAM -> "Back-office";
            case PHONE -> "Téléphone";
            case DESK -> "Accueil";
            case EMAIL -> "Courriel";
            case WHATSAPP -> "WhatsApp";
        };
    }

    static String participantRole(BookingParticipantRole role) {
        return switch (role) {
            case HOST -> "Hôte";
            case GUEST -> "Invité";
            case ORGANIZER -> "Organisateur";
            case MEMBER -> "Membre";
        };
    }

    static String participantStatus(BookingParticipantStatus status) {
        return switch (status) {
            case INVITED -> "Invité";
            case CONFIRMED -> "Confirmé";
            case DECLINED -> "A décliné";
            case REMOVED -> "Retiré";
        };
    }

    static String frequency(BookingRecurrenceFrequency frequency) {
        return switch (frequency) {
            case DAILY -> "Chaque jour";
            case WEEKLY -> "Chaque semaine";
            case MONTHLY -> "Chaque mois";
        };
    }

    static String holdStatus(BookingHoldStatus status) {
        return switch (status) {
            case ACTIVE -> "Active";
            case CONFIRMED -> "Convertie en réservation";
            case RELEASED -> "Libérée";
            case EXPIRED -> "Expirée";
        };
    }

    static String waitlistStatus(BookingWaitlistStatus status) {
        return switch (status) {
            case WAITING -> "En attente";
            case OFFERED -> "Place proposée";
            case BOOKED -> "Réservée";
            case CANCELLED -> "Annulée";
            case EXPIRED -> "Offre expirée";
        };
    }

    static String lineType(BookingLineType type) {
        return switch (type) {
            case RESOURCE -> "Ressource";
            case ENTITLEMENT -> "Droit d'abonnement ou de pass";
            case BILLABLE -> "Élément facturé";
            case SERVICE -> "Service";
            case AMENITY -> "Équipement";
            case PARTICIPANT -> "Participant supplémentaire";
            case DEPOSIT -> "Caution";
            case OVERRUN -> "Dépassement";
            case NO_SHOW -> "Non-présentation";
            case DISCOUNT -> "Remise";
        };
    }
}
