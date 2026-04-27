package com.sni.bokaticowork.features.booking.dto.response;

public record BookingRepairResponse(
        int inspected,
        int repaired,
        int failed
) {
}
