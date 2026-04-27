package com.sni.bokaticowork.features.booking.repository.specification;

import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class BookingCriteria {
    private SubscriberType ownerType;
    private String ownerCode;
    private String resourceCode;
    private BookingStatus status;
    private LocalDateTime startedFrom;
    private LocalDateTime startedTo;
}
