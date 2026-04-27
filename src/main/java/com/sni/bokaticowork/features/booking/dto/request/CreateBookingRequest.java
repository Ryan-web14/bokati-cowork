package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

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
        @Valid List<BookingParticipantRequest> participants
) {
        public BookingIdentityLookup identityLookup() {
                return new BookingIdentityLookup(memberId, customerId, businessCode, email, phone, walkIn, contactName, contactEmail, contactPhone);
        }
}
