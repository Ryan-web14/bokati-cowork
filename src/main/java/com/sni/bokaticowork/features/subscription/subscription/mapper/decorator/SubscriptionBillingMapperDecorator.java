package com.sni.bokaticowork.features.subscription.subscription.mapper.decorator;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillableItemResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillingScheduleResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionBillingMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.BillingSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class SubscriptionBillingMapperDecorator implements SubscriptionBillingMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionBillingMapper delegate;

    @Override
    public BillableItemResponse toBillableItemResponse(BillableItem item) {
        return new BillableItemResponse(item.getBillableNumber(), item.getSourceType(), item.getSourceId(), item.getSubscriberType(), item.getSubscriberCode(), item.getDescription(), item.getAmount(), item.getCurrency(), item.getTaxCode(), item.getBillingPeriodStart(), item.getBillingPeriodEnd(), item.getStatus(), item.getInvoiceId());
    }

    @Override
    public BillingScheduleResponse toBillingScheduleResponse(BillingSchedule schedule) {
        return new BillingScheduleResponse(schedule.getSubscription().getSubscriptionNumber(), schedule.getBillingCycle(), schedule.getNextBillingDate(), schedule.getCurrentPeriodStart(), schedule.getCurrentPeriodEnd(), schedule.getStatus(), schedule.getRetryCount(), schedule.getLastAttemptAt());
    }
}
