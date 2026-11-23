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

    private com.sni.bokaticowork.features.ressource.service.pricing.PriceContextResolver priceContextResolver;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPriceContextResolver(com.sni.bokaticowork.features.ressource.service.pricing.PriceContextResolver priceContextResolver) {
        this.priceContextResolver = priceContextResolver;
    }

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
        // Meme contexte de prix que la verification de disponibilite et la reservation (23/11/2026).
        String ownerType = org.springframework.util.StringUtils.hasText(request.memberId()) ? "MEMBER"
                : org.springframework.util.StringUtils.hasText(request.businessCode()) ? "BUSINESS_ENTITY"
                : org.springframework.util.StringUtils.hasText(request.customerId()) ? "CUSTOMER" : null;
        String ownerCode = org.springframework.util.StringUtils.hasText(request.memberId()) ? request.memberId()
                : org.springframework.util.StringUtils.hasText(request.businessCode()) ? request.businessCode() : request.customerId();
        com.sni.bokaticowork.features.ressource.service.pricing.PriceContext context =
                priceContextResolver == null ? null : priceContextResolver.forOwner(ownerType, ownerCode);
        collect(found, resource, wanted.minusHours(4), request.endedAt().plusHours(4), duration, quantity, "nearby_available_slot", context);
        if (found.size() < WANTED) {
            java.time.LocalDate day = wanted.toLocalDate();
            collect(found, resource, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), duration, quantity, "same_day_slot", context);
        }
        for (int offset = 1; offset <= DAYS_AHEAD && found.size() < WANTED; offset++) {
            java.time.LocalDate day = wanted.toLocalDate().plusDays(offset);
            collect(found, resource, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), duration, quantity, "later_day_slot", context);
        }
        return found.values().stream()
                .sorted(Comparator.comparing((BookingSuggestionResponse suggestion) ->
                        Math.abs(Duration.between(wanted, suggestion.startedAt()).toMinutes())))
                .limit(MAX)
                .toList();
    }

    private void collect(java.util.Map<java.time.LocalDateTime, BookingSuggestionResponse> found, Resource resource, java.time.LocalDateTime from,
                         java.time.LocalDateTime to, int duration, int quantity, String reason,
                         com.sni.bokaticowork.features.ressource.service.pricing.PriceContext context) {
        for (ResourceAvailabilityWindowResponse window : availabilityService.findRemainingWindows(resource.getCode(), from, to, duration, quantity)) {
            if (found.size() >= MAX * 3) {
                return;
            }
            found.computeIfAbsent(window.getStartedAt(), start -> toSuggestion(resource, window, quantity, reason, context));
        }
    }

    private BookingSuggestionResponse toSuggestion(Resource resource, ResourceAvailabilityWindowResponse window, int quantity, String reason,
                                                   com.sni.bokaticowork.features.ressource.service.pricing.PriceContext context) {
        // Un creneau sans prix applicable reste propose, sans montant : la liste ne tombe pas pour lui.
        java.math.BigDecimal amount;
        try {
            amount = pricingCalculator.calculate(resource, window.getStartedAt(), window.getEndedAt(), quantity, null, context).amount();
        } catch (com.sni.bokaticowork.core.exception.customs.BadRequestException ex) {
            amount = null;
        }
        return new BookingSuggestionResponse(resource.getCode(), resource.getName(), window.getStartedAt(), window.getEndedAt(), window.getRemainingCapacity(), amount, reason);
    }
}
