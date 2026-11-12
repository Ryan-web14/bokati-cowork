package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reservation multi-ressources, cote administration : une ou plusieurs lignes (ressource et
 * creneau), pour un ou plusieurs membres.
 *
 * <p>{@code memberIds} donne les membres de toutes les lignes ; une ligne qui porte ses propres
 * {@code memberIds} les utilise a la place. Chaque couple ligne et membre devient une reservation.
 * Le lot passe en entier ou pas du tout : une seule reservation impossible refuse le lot, avec la
 * liste de ce qui bloque.</p>
 *
 * <p>Chaque membre recoit une seule facture pour l'ensemble de ses reservations du lot.</p>
 */
public record CreateBookingBatchRequest(
        /** Membres par defaut (identifiants {@code MBR-...}), appliques aux lignes sans membres propres. */
        List<String> memberIds,
        @NotEmpty @Size(max = 50) @Valid List<Line> lines,
        @NotNull BookingPaymentMode paymentMode,
        /** L'organisation pour le compte de laquelle les membres reservent · vide : selon le rattachement. */
        String billingCompanyCode,
        Boolean confirmImmediately,
        Boolean sendEmail,
        String notes,
        /** Rejouer la meme cle rend le lot deja cree au lieu d'en creer un second. */
        @Size(max = 180) String idempotencyKey
) {

    public record Line(
            @NotBlank String resourceCode,
            @NotNull LocalDateTime startedAt,
            @NotNull LocalDateTime endedAt,
            @Min(1) Integer quantity,
            ResourceBookingUnit bookingUnit,
            /** Membres de cette ligne seulement · vide : les membres par defaut du lot. */
            List<String> memberIds,
            String notes,
            @Valid List<BookingParticipantRequest> participants
    ) {
    }
}
