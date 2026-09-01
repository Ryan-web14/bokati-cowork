package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Creation d'une reservation cote administration.
 *
 * <p>{@code bookingUnit} impose l'unite de facturation la ou la duree en designerait une autre —
 * facturer six heures en demi-journee, par exemple. Le prix n'est pas saisi : il reste lu dans la
 * grille tarifaire de la ressource pour cette unite, si bien qu'un montant facture reste rattache
 * a une regle. Vide, l'unite se deduit de la duree.
 *
 * <p>Le champ n'existe pas sur le portail client, dont les reservations passent forcement par le
 * calcul automatique.
 */
public record CreateBookingRequest(
        @NotBlank String resourceCode,
        String memberId,
        String customerId,
        String businessCode,
        @Email String email,
        String phone,
        Boolean walkIn,
        String contactName,
        @Email String contactEmail,
        String contactPhone,
        String idempotencyKey,
        String holdNumber,
        @NotNull LocalDateTime startedAt,
        @NotNull LocalDateTime endedAt,
        @Min(1) Integer quantity,
        @NotNull BookingPaymentMode paymentMode,
        Boolean confirmImmediately,
        Boolean sendEmail,
        String notes,
        String metadataJson,
        ResourceBookingUnit bookingUnit,
        @Valid List<BookingParticipantRequest> participants
) {
        public BookingIdentityLookup identityLookup() {
                return new BookingIdentityLookup(memberId, customerId, businessCode, email, phone, walkIn, contactName, contactEmail, contactPhone);
        }
}
