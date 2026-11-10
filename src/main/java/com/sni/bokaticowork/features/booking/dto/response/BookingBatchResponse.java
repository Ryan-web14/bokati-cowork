package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Un lot de reservations multi-ressources et ses factures, une par membre.
 *
 * <p>{@code uninvoicedBookingNumbers} : reservations du lot sans facture pour l'instant, celles
 * d'une ressource soumise a approbation · leur facture, a part, vient a l'approbation.</p>
 */
public record BookingBatchResponse(
        String batchNumber,
        BookingPaymentMode paymentMode,
        String billingCompanyCode,
        Integer bookingCount,
        Integer memberCount,
        String notes,
        String createdBy,
        Instant createdAt,
        List<BatchInvoice> invoices,
        List<String> uninvoicedBookingNumbers,
        List<BookingResponse> bookings
) {

    /** La facture d'un membre pour ses reservations du lot. */
    public record BatchInvoice(
            String documentNumber,
            BillingDocumentStatus status,
            String customerType,
            String customerCode,
            String customerName,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal balanceDue,
            String currency,
            List<String> bookingNumbers
    ) {
    }

    /** Un lot dans la liste, sans le detail des reservations. */
    public record Summary(
            String batchNumber,
            BookingPaymentMode paymentMode,
            String billingCompanyCode,
            Integer bookingCount,
            Integer memberCount,
            String notes,
            String createdBy,
            Instant createdAt
    ) {
    }
}
