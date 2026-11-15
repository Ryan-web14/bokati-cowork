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

    static final int WANTED = 5;
    static final int MAX = 10;
    static final int DAYS_AHEAD = 7;

    /**
     * Creneaux libres de la meme duree, du plus proche au plus eloigne de l'heure demandee : d'abord a
     * quatre heures pres ; s'il y en a moins de cinq, sur toute la journee ; puis sur les sept jours
     * suivants. Avant, seule la fenetre de quatre heures etait cherchee : une salle occupee toute la
     * matinee ne proposait rien, alors qu'elle etait libre l'apres-midi.
     */
    @Override
    public List<BookingSuggestionResponse> suggest(BookingAvailabilityRequest request) {
        Resource resource = resourceService.getResourceForService(request.resourceCode().trim());
        int duration = Math.toIntExact(Duration.between(request.startedAt(), request.endedAt()).toMinutes());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        java.time.LocalDateTime wanted = request.startedAt();
        java.util.Map<java.time.LocalDateTime, BookingSuggestionResponse> found = new java.util.LinkedHashMap<>();
        collect(found, resource, wanted.minusHours(4), request.endedAt().plusHours(4), duration, quantity, "nearby_available_slot");
        if (found.size() < WANTED) {
            java.time.LocalDate day = wanted.toLocalDate();
            collect(found, resource, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), duration, quantity, "same_day_slot");
        }
        for (int offset = 1; offset <= DAYS_AHEAD && found.size() < WANTED; offset++) {
            java.time.LocalDate day = wanted.toLocalDate().plusDays(offset);
            collect(found, resource, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), duration, quantity, "later_day_slot");
        }
        return found.values().stream()
                .sorted(Comparator.comparing((BookingSuggestionResponse suggestion) ->
                        Math.abs(Duration.between(wanted, suggestion.startedAt()).toMinutes())))
                .limit(MAX)
                .toList();
    }

    private void collect(java.util.Map<java.time.LocalDateTime, BookingSuggestionResponse> found, Resource resource, java.time.LocalDateTime from,
                         java.time.LocalDateTime to, int duration, int quantity, String reason) {
        for (ResourceAvailabilityWindowResponse window : availabilityService.findRemainingWindows(resource.getCode(), from, to, duration, quantity)) {
            if (found.size() >= MAX * 3) {
                return;
            }
            found.computeIfAbsent(window.getStartedAt(), start -> toSuggestion(resource, window, quantity, reason));
        }
    }

    private BookingSuggestionResponse toSuggestion(Resource resource, ResourceAvailabilityWindowResponse window, int quantity, String reason) {
        BookingPricingCalculator.Price price = pricingCalculator.calculate(resource, window.getStartedAt(), window.getEndedAt(), quantity);
        return new BookingSuggestionResponse(resource.getCode(), resource.getName(), window.getStartedAt(), window.getEndedAt(), window.getRemainingCapacity(), price.amount(), reason);
    }
}
