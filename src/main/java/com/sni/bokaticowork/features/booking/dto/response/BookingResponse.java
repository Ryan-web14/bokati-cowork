package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentStatus;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        String bookingNumber,
        String resourceCode,
        String resourceName,
        String resourceTypeCode,
        String resourceGroupCode,
        SubscriberType ownerType,
        String ownerCode,
        String contactName,
        String contactEmail,
        String contactPhone,
        BookingStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer durationMinutes,
        Integer quantity,
        ResourceBookingUnit bookingUnit,
        BookingPaymentMode paymentMode,
        BookingPaymentStatus paymentStatus,
        String paymentStatusLabel,
        String subscriptionNumber,
        String passNumber,
        String entitlementCode,
        BigDecimal unitPrice,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        String currency,
        String notes,
        String metadataJson,
        String checkInToken,
        String checkInQrValue,
        String virtualMeetingUrl,
        Instant confirmedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        List<BookingLineResponse> lines,
        List<BookingParticipantResponse> participants,
        /** Places de la ressource · a ne pas confondre avec {@code quantity}, ce qui est reserve. */
        Integer resourceCapacity,
        /** La piece est louee entiere · {@code quantity} compte alors des participants, pas des places. */
        Boolean wholeResourceBooking,
        /** Ce qui est reserve, pret a afficher · « 2 places réservées sur 4 », « Espace entier · 3 participants (12 places) ». */
        String seatsLabel,
        /** Le lot multi-ressources dont la reservation fait partie · {@code null} pour une reservation seule. */
        String batchNumber,
        /** Lot B2b : nombre de reports et de changements de ressource deja faits (0 : jamais modifiee). */
        Integer modificationCount,
        Instant lastModifiedAt,
        /** Lot B2b : derniere modification, lisible (« Report au … · +5000 XAF (pièce …) »). */
        String lastModification,
        /** Lot B7 : auteur de la reservation (session), canal de prise. */
        String createdBy,
        com.sni.bokaticowork.features.booking.enums.BookingChannel channel,
        String holdNumber,
        /** Numero de la serie recurrente, nul hors serie. */
        String recurrenceGroupNumber,
        /** Echeance de paiement d'une reservation en attente de paiement (lot B1). */
        LocalDateTime paymentDueAt,
        /** Reglement constate (lot B1) ; nul tant que rien n'est regle. */
        Instant paidAt,
        Instant checkedInAt,
        Instant checkedOutAt,
        String approvedBy,
        Instant approvedAt,
        String rejectedBy,
        Instant rejectedAt,
        String rejectionReason,
        String rejectionReasonCode,
        String cancellationReason,
        String cancellationReasonCode,
        /** Facture de la reservation ; seulement sur la fiche ({@code GET /bookings/{numero}}), nul dans les listes. */
        String invoiceNumber,
        /** Supplements (services, equipements, participants, caution, depassement, non-presentation), hors ressource. */
        BigDecimal extrasAmount,
        /** {@code totalAmount} (ressource, remises deduites) plus {@code extrasAmount}. */
        BigDecimal grandTotal
) {
}
