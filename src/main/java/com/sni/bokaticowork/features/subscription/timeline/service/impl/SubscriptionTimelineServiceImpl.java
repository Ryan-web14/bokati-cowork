package com.sni.bokaticowork.features.subscription.timeline.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.timeline.dto.CreateTimelineEventRequest;
import com.sni.bokaticowork.features.subscription.timeline.dto.SubscriptionTimelineEventResponse;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import com.sni.bokaticowork.features.subscription.timeline.mapper.interfaces.SubscriptionTimelineMapper;
import com.sni.bokaticowork.features.subscription.timeline.model.SubscriptionTimelineEvent;
import com.sni.bokaticowork.features.subscription.timeline.repository.SubscriptionTimelineEventRepository;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionTimelineServiceImpl implements SubscriptionTimelineService {

    private final SubscriptionTimelineEventRepository timelineRepository;
    private final SubscriptionTimelineMapper timelineMapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public SubscriptionTimelineEventResponse create(CreateTimelineEventRequest request) {
        Subscription subscription = StringUtils.hasText(request.subscriptionNumber())
                ? getSubscription(request.subscriptionNumber())
                : null;
        SubscriptionTimelineEvent event = timelineMapper.toEntity(request);
        event.setEventNumber(sequenceGenerator.next("subscription_timeline_event"));
        event.setSubscription(subscription);
        if (subscription != null) {
            event.setOwnerType(subscription.getSubscriberType());
            event.setOwnerCode(subscription.getSubscriberCode());
        }
        return timelineMapper.toResponse(timelineRepository.save(event));
    }

    @Override
    public void write(Subscription subscription,
                      SubscriptionTimelineEventType eventType,
                      String sourceType,
                      String sourceId,
                      String title,
                      String description,
                      String payloadJson) {
        timelineRepository.save(SubscriptionTimelineEvent.builder()
                .eventNumber(sequenceGenerator.next("subscription_timeline_event"))
                .subscription(subscription)
                .ownerType(subscription == null ? null : subscription.getSubscriberType())
                .ownerCode(subscription == null ? null : subscription.getSubscriberCode())
                .eventType(eventType)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .title(title)
                .description(description)
                .payloadJson(payloadJson)
                .occurredAt(Instant.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SubscriptionTimelineEventResponse> list(String subscriptionNumber,
                                                                     SubscriberType ownerType,
                                                                     String ownerCode,
                                                                     SubscriptionTimelineEventType eventType,
                                                                     Instant occurredFrom,
                                                                     Instant occurredTo,
                                                                     Pageable pageable) {
        return new PaginatedResponse<>(timelineRepository.search(
                trim(subscriptionNumber),
                ownerType == null ? null : ownerType.name(),
                trim(ownerCode),
                eventType == null ? null : eventType.name(),
                occurredFrom,
                occurredTo,
                pageable
        ).map(timelineMapper::toResponse));
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Subscription getSubscription(String subscriptionNumber) {
        return subscriptionRepository.findBySubscriptionNumber(subscriptionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription " + subscriptionNumber + " not found"));
    }
}
