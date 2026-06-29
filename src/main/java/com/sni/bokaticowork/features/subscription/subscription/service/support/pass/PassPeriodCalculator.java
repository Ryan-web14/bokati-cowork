package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassDurationUnit;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class PassPeriodCalculator {

    public Instant periodEnd(Instant from, Integer duration, PassDurationUnit unit) {
        if (from == null) throw new BadRequestException("Pass start date is required");
        ZonedDateTime zdt = from.atZone(ZoneOffset.UTC);
        return switch (unit) {
            case DAY -> zdt.plusDays(duration).toInstant();
            case WEEK -> zdt.plusWeeks(duration).toInstant();
            case MONTH -> zdt.plusMonths(duration).toInstant();
            case YEAR -> zdt.plusYears(duration).toInstant();
        };
    }

    public Instant nextRenewalDate(Instant currentEnd, Integer duration, PassDurationUnit unit) {
        return periodEnd(currentEnd, duration, unit);
    }
}
