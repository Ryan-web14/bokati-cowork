package com.sni.bokaticowork.features.subscription.timeline.service;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.timeline.dto.CreateTimelineEventRequest;
import com.sni.bokaticowork.features.subscription.timeline.dto.SubscriptionTimelineEventResponse;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface SubscriptionTimelineService {

    SubscriptionTimelineEventResponse create(CreateTimelineEventRequest request);

    void write(Subscription subscription,
               SubscriptionTimelineEventType eventType,
               String sourceType,
               String sourceId,
               String title,
               String description,
               String payloadJson);

    PaginatedResponse<SubscriptionTimelineEventResponse> list(String subscriptionNumber,
                                                              SubscriberType ownerType,
                                                              String ownerCode,
                                                              SubscriptionTimelineEventType eventType,
                                                              Instant occurredFrom,
                                                              Instant occurredTo,
                                                              Pageable pageable);
}
