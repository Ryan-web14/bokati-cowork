package com.sni.bokaticowork.features.subscription.seat.dto;

import com.sni.bokaticowork.features.subscription.seat.enums.SeatRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionSeatRequest(
        @NotBlank String memberCode,
        @NotNull SeatRole role
) {
}
