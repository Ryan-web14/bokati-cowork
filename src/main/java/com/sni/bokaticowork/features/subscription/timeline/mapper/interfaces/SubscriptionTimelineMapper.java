package com.sni.bokaticowork.features.subscription.timeline.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.timeline.dto.CreateTimelineEventRequest;
import com.sni.bokaticowork.features.subscription.timeline.dto.SubscriptionTimelineEventResponse;
import com.sni.bokaticowork.features.subscription.timeline.mapper.decorator.SubscriptionTimelineMapperDecorator;
import com.sni.bokaticowork.features.subscription.timeline.model.SubscriptionTimelineEvent;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionTimelineMapperDecorator.class)
public interface SubscriptionTimelineMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventNumber", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SubscriptionTimelineEvent toEntity(CreateTimelineEventRequest request);

    SubscriptionTimelineEventResponse toResponse(SubscriptionTimelineEvent event);
}
