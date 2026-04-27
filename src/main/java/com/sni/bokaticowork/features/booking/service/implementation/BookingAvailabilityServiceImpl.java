package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingAvailabilityService;
import com.sni.bokaticowork.features.booking.service.support.BookingIdentityResolver;
import com.sni.bokaticowork.features.booking.service.support.BookingPaymentContextResolver;
import com.sni.bokaticowork.features.booking.service.support.BookingPricingCalculator;
import com.sni.bokaticowork.features.booking.service.support.BookingResourceGuard;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingAvailabilityServiceImpl implements BookingAvailabilityService {

    private final ResourceService resourceService;
    private final ResourceAvailabilityService resourceAvailabilityService;
    private final BookingResourceGuard resourceGuard;
    private final BookingPricingCalculator pricingCalculator;
    private final BookingIdentityResolver identityResolver;
    private final BookingPaymentContextResolver paymentContextResolver;

    @Override
    public BookingAvailabilityResponse check(BookingAvailabilityRequest request) {
        Resource resource = resourceService.getResourceForService(request.resourceCode().trim());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        try {
            resourceGuard.validateBookable(resource, request.startedAt(), request.endedAt(), quantity);
            resourceGuard.validateNoSingleCapacityConflict(resource, request.startedAt(), request.endedAt(), null);
            if (request.paymentMode() != null && request.paymentMode() != BookingPaymentMode.DIRECT) {
                var identity = identityResolver.resolve(request.identityLookup());
                paymentContextResolver.resolve(request.paymentMode(), identity, resource);
            }
            int duration = Math.toIntExact(Duration.between(request.startedAt(), request.endedAt()).toMinutes());
            List<ResourceAvailabilityWindowResponse> windows = resourceAvailabilityService.findRemainingWindows(
                    resource.getCode(),
                    request.startedAt(),
                    request.endedAt(),
                    duration,
                    quantity
            );
            boolean exact = windows.stream().anyMatch(window ->
                    window.getStartedAt().equals(request.startedAt()) && window.getEndedAt().equals(request.endedAt()));
            BookingPricingCalculator.Price price = pricingCalculator.calculate(resource, request.startedAt(), request.endedAt(), quantity);
            String message = exact ? "Available" : "No exact availability window found";
            if (exact && request.paymentMode() == BookingPaymentMode.SUBSCRIPTION) {
                message = "Available - covered by subscription";
            } else if (exact && request.paymentMode() == BookingPaymentMode.PASS) {
                message = "Available - covered by pass";
            }
            return new BookingAvailabilityResponse(
                    resource.getCode(),
                    resource.getName(),
                    request.startedAt(),
                    request.endedAt(),
                    duration,
                    quantity,
                    exact,
                    windows.stream().map(ResourceAvailabilityWindowResponse::getRemainingCapacity).findFirst().orElse(null),
                    price.unit(),
                    price.unitPrice(),
                    price.amount(),
                    price.currency(),
                    message
            );
        } catch (BadRequestException | ConflictException | ResourceNotFoundException ex) {
            return new BookingAvailabilityResponse(
                    resource.getCode(),
                    resource.getName(),
                    request.startedAt(),
                    request.endedAt(),
                    null,
                    quantity,
                    false,
                    null,
                    null,
                    null,
                    null,
                    "XAF",
                    ex.getMessage()
            );
        }
    }
}
