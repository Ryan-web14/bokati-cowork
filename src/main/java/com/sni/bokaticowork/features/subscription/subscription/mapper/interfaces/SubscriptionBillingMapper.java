package com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillableItemResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillingScheduleResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.decorator.SubscriptionBillingMapperDecorator;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.BillingSchedule;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionBillingMapperDecorator.class)
public interface SubscriptionBillingMapper {

    default BillableItemResponse toBillableItemResponse(BillableItem item) {
        return null;
    }

    default BillingScheduleResponse toBillingScheduleResponse(BillingSchedule schedule) {
        return null;
    }
}
