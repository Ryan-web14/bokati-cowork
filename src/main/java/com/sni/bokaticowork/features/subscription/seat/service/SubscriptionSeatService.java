package com.sni.bokaticowork.features.subscription.seat.service;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.seat.dto.CreateSubscriptionSeatRequest;
import com.sni.bokaticowork.features.subscription.seat.dto.SubscriptionSeatResponse;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;
import org.springframework.data.domain.Pageable;

public interface SubscriptionSeatService {

    SubscriptionSeatResponse add(String subscriptionNumber, CreateSubscriptionSeatRequest request);

    SubscriptionSeatResponse remove(String subscriptionNumber, String memberCode);

    PaginatedResponse<SubscriptionSeatResponse> list(String subscriptionNumber, String memberCode, SeatStatus status, Pageable pageable);
}
