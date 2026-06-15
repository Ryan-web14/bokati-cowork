package com.sni.bokaticowork.features.portal.booking.dto.request;

import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClientCreateBookingRequest {

    @NotBlank
    private String resourceCode;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private LocalDateTime endedAt;

    @Min(1)
    private Integer quantity = 1;

    @NotNull
    private BookingPaymentMode paymentMode;

    private String notes;

    private String idempotencyKey;

    @Valid
    private List<BookingParticipantRequest> participants;
}
