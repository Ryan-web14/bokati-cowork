package com.sni.bokaticowork.features.subscription.timeline.mapper.decorator;

import com.sni.bokaticowork.features.subscription.timeline.dto.CreateTimelineEventRequest;
import com.sni.bokaticowork.features.subscription.timeline.dto.SubscriptionTimelineEventResponse;
import com.sni.bokaticowork.features.subscription.timeline.mapper.interfaces.SubscriptionTimelineMapper;
import com.sni.bokaticowork.features.subscription.timeline.model.SubscriptionTimelineEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public abstract class SubscriptionTimelineMapperDecorator implements SubscriptionTimelineMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionTimelineMapper delegate;

    @Override
    public SubscriptionTimelineEvent toEntity(CreateTimelineEventRequest request) {
        SubscriptionTimelineEvent event = delegate.toEntity(request);
        event.setOwnerCode(trim(request.ownerCode()));
        event.setSourceType(trim(request.sourceType()));
        event.setSourceId(trim(request.sourceId()));
        event.setTitle(request.title().trim());
        event.setDescription(trim(request.description()));
        event.setPayloadJson(trim(request.payloadJson()));
        return event;
    }

    @Override
    public SubscriptionTimelineEventResponse toResponse(SubscriptionTimelineEvent event) {
        return new SubscriptionTimelineEventResponse(
                event.getEventNumber(),
                event.getSubscription() == null ? null : event.getSubscription().getSubscriptionNumber(),
                event.getOwnerType(),
                event.getOwnerCode(),
                event.getEventType(),
                event.getSourceType(),
                event.getSourceId(),
                event.getTitle(),
                event.getDescription(),
                event.getPayloadJson(),
                event.getOccurredAt()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
