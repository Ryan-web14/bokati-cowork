package com.sni.bokaticowork.features.booking.dto.request;

public record BookingApprovalRequest(
        String actor,
        String reason,
        Boolean sendEmail
) {
}
