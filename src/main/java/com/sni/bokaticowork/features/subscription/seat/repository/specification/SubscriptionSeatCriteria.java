package com.sni.bokaticowork.features.subscription.seat.repository.specification;

import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SubscriptionSeatCriteria {
    private String subscriptionNumber;
    private String memberCode;
    private SeatStatus status;
}
