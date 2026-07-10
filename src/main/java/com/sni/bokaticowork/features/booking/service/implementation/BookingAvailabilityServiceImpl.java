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

        // Step 1: check physical slot availability · if this fails the slot is truly occupied
        try {
            resourceGuard.validateBookable(resource, request.startedAt(), request.endedAt(), quantity);
            resourceGuard.validateNoSingleCapacityConflict(resource, request.startedAt(), request.endedAt(), null);
        } catch (BadRequestException | ConflictException | ResourceNotFoundException ex) {
            return unavailableResponse(resource, request, quantity, ex.getMessage());
        }

        // Step 2: fetch available windows and verify the requested slot fits within one of them
        int duration = Math.toIntExact(Duration.between(request.startedAt(), request.endedAt()).toMinutes());
        List<ResourceAvailabilityWindowResponse> windows;
        try {
            windows = resourceAvailabilityService.findRemainingWindows(
                    resource.getCode(), request.startedAt(), request.endedAt(), duration, quantity);
        } catch (Exception ex) {
            return unavailableResponse(resource, request, quantity, ex.getMessage());
        }

        // "fits within" instead of exact equality · handles cases where the window spans the full day
        boolean slotFits = windows.stream().anyMatch(w ->
                !w.getStartedAt().isAfter(request.startedAt()) && !w.getEndedAt().isBefore(request.endedAt()));

        if (!slotFits) {
            return unavailableResponse(resource, request, quantity,
                    "Creneau non disponible pour les horaires demandes");
        }

        // Step 3: validate payment context only AFTER confirming the slot is available
        // so that a missing subscription does not produce the same response as a slot conflict
        if (request.paymentMode() != null && request.paymentMode() != BookingPaymentMode.DIRECT) {
            try {
                var identity = identityResolver.resolve(request.identityLookup());
                paymentContextResolver.resolve(request.paymentMode(), identity, resource);
            } catch (ResourceNotFoundException | BadRequestException ex) {
                // Slot is available but payment context is missing · return clear payment error
                return unavailableResponse(resource, request, quantity, ex.getMessage());
            }
        }

        // Step 4: pricing and success response
        try {
            BookingPricingCalculator.Price price = pricingCalculator.calculate(resource, request.startedAt(), request.endedAt(), quantity);
            String message = "Disponible";
            if (request.paymentMode() == BookingPaymentMode.SUBSCRIPTION) {
                message = "Disponible · couvert par abonnement";
            } else if (request.paymentMode() == BookingPaymentMode.PASS) {
                message = "Disponible · couvert par pass";
            }
            return new BookingAvailabilityResponse(
                    resource.getCode(),
                    resource.getName(),
                    request.startedAt(),
                    request.endedAt(),
                    duration,
                    quantity,
                    true,
                    windows.stream().map(ResourceAvailabilityWindowResponse::getRemainingCapacity).findFirst().orElse(null),
                    price.unit(),
                    price.unitPrice(),
                    price.amount(),
                    price.currency(),
                    message
            );
        } catch (Exception ex) {
            return unavailableResponse(resource, request, quantity, ex.getMessage());
        }
    }

    private BookingAvailabilityResponse unavailableResponse(Resource resource, BookingAvailabilityRequest request, int quantity, String message) {
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
                message
        );
    }
}
