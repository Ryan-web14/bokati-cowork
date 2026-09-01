package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingAvailabilityRequest(
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
        BookingPaymentMode paymentMode,
        @NotNull LocalDateTime startedAt,
        @NotNull LocalDateTime endedAt,
        @Min(1) Integer quantity,

        /** Voir {@link CreateBookingRequest#bookingUnit()} · ici pour que le devis annonce ce qui sera facture. */
        ResourceBookingUnit bookingUnit
) {
        public BookingIdentityLookup identityLookup() {
                return new BookingIdentityLookup(memberId, customerId, businessCode, email, phone, walkIn, contactName, contactEmail, contactPhone);
        }
}
