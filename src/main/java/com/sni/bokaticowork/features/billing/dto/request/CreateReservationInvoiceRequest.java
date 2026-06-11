package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record CreateReservationInvoiceRequest(
        /** BOOKING (réservation système) ou EXTERNAL (hors-système). */
        @NotBlank String mode,

        /* ---- Mode BOOKING ---- */
        String bookingNumber,

        /* ---- Mode EXTERNAL ---- */
        String customerType,
        String customerCode,
        String currency,
        @Valid ExternalBookingDetails externalBooking,

        /* ---- Communs ---- */
        BillingDocumentType documentType,
        LocalDate issueDate,
        LocalDate dueDate,
        String terms,
        String paymentInstructions,
        String description,
        String internalNotes,
        List<CreateBillingDocumentLineRequest> extraLines
) {
}
