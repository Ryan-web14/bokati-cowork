package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingTransferRequest(
        @NotNull SubscriberType newOwnerType,
        @NotBlank String newOwnerCode,
        String newContactName,
        String newContactEmail,
        String newContactPhone,
        String actor,
        String note,
        Boolean sendEmail
) {}
