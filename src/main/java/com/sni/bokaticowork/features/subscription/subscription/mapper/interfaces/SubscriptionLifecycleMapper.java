package com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionHistoryResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.decorator.SubscriptionLifecycleMapperDecorator;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionStatusHistory;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionLifecycleMapperDecorator.class)
public interface SubscriptionLifecycleMapper {

    default SubscriptionResponse toResponse(Subscription subscription) {
        return null;
    }

    default SubscriptionHistoryResponse toHistoryResponse(SubscriptionStatusHistory history) {
        return null;
    }
}
