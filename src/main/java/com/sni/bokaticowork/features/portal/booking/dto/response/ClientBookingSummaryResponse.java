package com.sni.bokaticowork.features.portal.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class ClientBookingSummaryResponse {

    private String bookingNumber;
    private String resourceCode;
    private String resourceName;
    private String resourceTypeCode;
    private String resourceGroupCode;
    private BookingStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes;
    private Integer quantity;
    private ResourceBookingUnit bookingUnit;
    private BookingPaymentMode paymentMode;
    private BigDecimal totalAmount;
    private String currency;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private Instant completedAt;
    private Instant createdAt;
}
