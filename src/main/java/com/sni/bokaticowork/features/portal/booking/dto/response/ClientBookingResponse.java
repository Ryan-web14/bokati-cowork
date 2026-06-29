package com.sni.bokaticowork.features.portal.booking.dto.response;

import com.sni.bokaticowork.features.booking.dto.response.BookingParticipantResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClientBookingResponse {

    private String bookingNumber;
    private String resourceName;
    private String resourceGroupCode;
    private String zone;
    private String locationLabel;
    private BookingStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes;
    private Integer quantity;
    private ResourceBookingUnit bookingUnit;
    private BookingPaymentMode paymentMode;
    private String subscriptionNumber;
    private String passNumber;
    private BigDecimal unitPrice;
    private BigDecimal subtotalAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private String checkInToken;
    private String checkInQrValue;
    private String virtualMeetingUrl;
    private Instant confirmedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private List<BookingParticipantResponse> participants;
}
