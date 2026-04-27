package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingSuggestionResponse;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingSuggestionService;
import com.sni.bokaticowork.features.booking.service.support.BookingPricingCalculator;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingSuggestionServiceImpl implements BookingSuggestionService {

    private final ResourceService resourceService;
    private final ResourceAvailabilityService availabilityService;
    private final BookingPricingCalculator pricingCalculator;

    @Override
    public List<BookingSuggestionResponse> suggest(BookingAvailabilityRequest request) {
        Resource resource = resourceService.getResourceForService(request.resourceCode().trim());
        int duration = Math.toIntExact(Duration.between(request.startedAt(), request.endedAt()).toMinutes());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        return availabilityService.findRemainingWindows(resource.getCode(), request.startedAt().minusHours(4), request.endedAt().plusHours(4), duration, quantity)
                .stream()
                .limit(10)
                .map(window -> toSuggestion(resource, window, quantity))
                .sorted(Comparator.comparing(BookingSuggestionResponse::estimatedAmount))
                .toList();
    }

    private BookingSuggestionResponse toSuggestion(Resource resource, ResourceAvailabilityWindowResponse window, int quantity) {
        BookingPricingCalculator.Price price = pricingCalculator.calculate(resource, window.getStartedAt(), window.getEndedAt(), quantity);
        return new BookingSuggestionResponse(resource.getCode(), resource.getName(), window.getStartedAt(), window.getEndedAt(), window.getRemainingCapacity(), price.amount(), "nearby_available_slot");
    }
}
