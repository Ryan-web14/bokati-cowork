package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BookingEntitlementBridge {

    private static final String REFERENCE_TYPE = "BOOKING";

    private final EntitlementService entitlementService;
    private final UsageRecordService usageRecordService;

    public void reserve(Booking booking, BigDecimal quantity) {
        if (!StringUtils.hasText(booking.getEntitlementCode())) {
            throw new BadRequestException("Entitlement code is required for subscription or pass booking");
        }
        entitlementService.reserve(operation(booking, quantity, "reserve", "Booking confirmed"));
    }

    public void consume(Booking booking, BigDecimal quantity) {
        if (!StringUtils.hasText(booking.getEntitlementCode())) {
            return;
        }
        entitlementService.consume(operation(booking, quantity, "consume", "Booking completed"));
        usageRecordService.record(new CreateUsageRecordRequest(
                booking.getOwnerType(),
                booking.getOwnerCode(),
                booking.getEntitlementCode(),
                quantity,
                toEntitlementUnit(booking),
                REFERENCE_TYPE,
                booking.getBookingNumber(),
                false,
                false,
                null,
                booking.getCurrency(),
                Instant.now(),
                booking.getMetadataJson()
        ));
    }

    public void release(Booking booking, BigDecimal quantity) {
        if (!StringUtils.hasText(booking.getEntitlementCode())) {
            return;
        }
        entitlementService.release(operation(booking, quantity, "release", "Booking cancelled"));
    }

    private EntitlementOperationRequest operation(Booking booking, BigDecimal quantity, String action, String reason) {
        return new EntitlementOperationRequest(
                booking.getOwnerType(),
                booking.getOwnerCode(),
                booking.getEntitlementCode(),
                quantity,
                REFERENCE_TYPE,
                booking.getBookingNumber(),
                "booking:" + booking.getBookingNumber() + ":" + action,
                reason
        );
    }

    private EntitlementUnit toEntitlementUnit(Booking booking) {
        return switch (booking.getBookingUnit()) {
            case DAY -> EntitlementUnit.DAY;
            case HOUR, HALF_DAY -> EntitlementUnit.HOUR;
            case WEEK, MONTH -> EntitlementUnit.BOOKING;
        };
    }
}
