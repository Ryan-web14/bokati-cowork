package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SubscriptionPeriodCalculator {

    public LocalDate periodEnd(LocalDate startDate, BillingCycle cycle) {
        return switch (cycle) {
            case DAILY -> startDate;
            case WEEKLY -> startDate.plusWeeks(1).minusDays(1);
            case MONTHLY -> startDate.plusMonths(1).minusDays(1);
            case QUARTERLY -> startDate.plusMonths(3).minusDays(1);
            case YEARLY -> startDate.plusYears(1).minusDays(1);
            case ONE_TIME -> startDate;
        };
    }

    public LocalDate nextBillingDate(LocalDate startDate, BillingCycle cycle) {
        return switch (cycle) {
            case DAILY -> startDate.plusDays(1);
            case WEEKLY -> startDate.plusWeeks(1);
            case MONTHLY -> startDate.plusMonths(1);
            case QUARTERLY -> startDate.plusMonths(3);
            case YEARLY -> startDate.plusYears(1);
            case ONE_TIME -> null;
        };
    }
}
