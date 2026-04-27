package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillableItemResponse(
        String billableNumber,
        String sourceType,
        String sourceId,
        SubscriberType subscriberType,
        String subscriberCode,
        String description,
        BigDecimal amount,
        String currency,
        String taxCode,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        BillableItemStatus status,
        Long invoiceId
) {
}
