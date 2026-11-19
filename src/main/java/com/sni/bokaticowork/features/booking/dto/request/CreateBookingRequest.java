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
 * <p>{@code bookingUnit} impose l'unite de facturation la ou la duree en designerait une autre · 
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
        @Valid List<BookingParticipantRequest> participants,
        /**
         * L'organisation pour le compte de laquelle le membre reserve · le choix « pour
         * l'organisation » du mode au choix. Vide : la facture suit le mode du rattachement.
         */
        String billingCompanyCode,
        /** Lot B2b : canal (PHONE, DESK, EMAIL, WHATSAPP pour l'equipe) ; vide : deduit de l'appel. */
        com.sni.bokaticowork.features.booking.enums.BookingChannel channel,
        /** Lot B2b : options prises avec la reservation (services du catalogue, equipements optionnels). */
        @Valid List<BookingOptionRequest> options,
        /**
         * Lot B3 : rendez-vous sans paiement, regle sur place. Mode {@code DIRECT} seulement ; le
         * creneau est retenu jusqu'a l'echeance de paiement, puis rendu sans reglement.
         */
        Boolean payOnSite
) {
        @com.fasterxml.jackson.annotation.JsonCreator
        public CreateBookingRequest {
        }

        /** Sans canal ni options : forme d'avant le lot B2b. */
        public CreateBookingRequest(String resourceCode, String memberId, String customerId, String businessCode,
                                    String email, String phone, Boolean walkIn, String contactName, String contactEmail,
                                    String contactPhone, String idempotencyKey, String holdNumber, LocalDateTime startedAt,
                                    LocalDateTime endedAt, Integer quantity, BookingPaymentMode paymentMode,
                                    Boolean confirmImmediately, Boolean sendEmail, String notes, String metadataJson,
                                    ResourceBookingUnit bookingUnit, List<BookingParticipantRequest> participants,
                                    String billingCompanyCode) {
                this(resourceCode, memberId, customerId, businessCode, email, phone, walkIn, contactName, contactEmail,
                        contactPhone, idempotencyKey, holdNumber, startedAt, endedAt, quantity, paymentMode,
                        confirmImmediately, sendEmail, notes, metadataJson, bookingUnit, participants, billingCompanyCode,
                        null, null, null);
        }

        /** Forme du lot B2b, sans rendez-vous regle sur place. */
        public CreateBookingRequest(String resourceCode, String memberId, String customerId, String businessCode,
                                    String email, String phone, Boolean walkIn, String contactName, String contactEmail,
                                    String contactPhone, String idempotencyKey, String holdNumber, LocalDateTime startedAt,
                                    LocalDateTime endedAt, Integer quantity, BookingPaymentMode paymentMode,
                                    Boolean confirmImmediately, Boolean sendEmail, String notes, String metadataJson,
                                    ResourceBookingUnit bookingUnit, List<BookingParticipantRequest> participants,
                                    String billingCompanyCode, com.sni.bokaticowork.features.booking.enums.BookingChannel channel,
                                    List<BookingOptionRequest> options) {
                this(resourceCode, memberId, customerId, businessCode, email, phone, walkIn, contactName, contactEmail,
                        contactPhone, idempotencyKey, holdNumber, startedAt, endedAt, quantity, paymentMode,
                        confirmImmediately, sendEmail, notes, metadataJson, bookingUnit, participants, billingCompanyCode,
                        channel, options, null);
        }

        /** Sans organisation choisie · la facture suit le mode du rattachement du membre. */
        public CreateBookingRequest(String resourceCode, String memberId, String customerId, String businessCode,
                                    String email, String phone, Boolean walkIn, String contactName, String contactEmail,
                                    String contactPhone, String idempotencyKey, String holdNumber, LocalDateTime startedAt,
                                    LocalDateTime endedAt, Integer quantity, BookingPaymentMode paymentMode,
                                    Boolean confirmImmediately, Boolean sendEmail, String notes, String metadataJson,
                                    ResourceBookingUnit bookingUnit, List<BookingParticipantRequest> participants) {
                this(resourceCode, memberId, customerId, businessCode, email, phone, walkIn, contactName, contactEmail,
                        contactPhone, idempotencyKey, holdNumber, startedAt, endedAt, quantity, paymentMode,
                        confirmImmediately, sendEmail, notes, metadataJson, bookingUnit, participants, null, null, null, null);
        }

        public BookingIdentityLookup identityLookup() {
                return new BookingIdentityLookup(memberId, customerId, businessCode, email, phone, walkIn, contactName, contactEmail, contactPhone);
        }
}
