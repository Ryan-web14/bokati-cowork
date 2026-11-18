package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceLocationRequests;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceLocationResponses;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAmenityLinkRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePricingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lot R5b : « Trouver une salle » par besoin. Une ressource convient si elle est reservable, du bon
 * type et du bon site, assez grande, equipee de tout ce qui est exige, libre sur le creneau et dans
 * le budget. Sinon, des alternatives expliquees : autre horaire le meme jour, autre site, au-dessus
 * du budget, equipement manquant.
 */
@Component
@RequiredArgsConstructor
public class ResourceFinder {

    static final int MAX_MATCHES = 20;
    static final int MAX_ALTERNATIVES = 10;

    private final ResourceRepository resourceRepository;
    private final ResourceAmenityLinkRepository amenityLinks;
    @Lazy
    private final ResourceAvailabilityService availabilityService;
    @Lazy
    private final ResourcePricingRuleService pricingService;

    @Transactional(readOnly = true)
    public ResourceLocationResponses.FindResult find(ResourceLocationRequests.Find request) {
        if (request == null || request.startedAt() == null || request.endedAt() == null || !request.endedAt().isAfter(request.startedAt())) {
            throw new BadRequestException("Le début et la fin du créneau sont requis, la fin après le début");
        }
        int participants = request.participants() == null || request.participants() < 1 ? 1 : request.participants();
        int minutes = Math.toIntExact(Duration.between(request.startedAt(), request.endedAt()).toMinutes());
        Set<String> wanted = request.amenityCodes() == null ? Set.of() : request.amenityCodes().stream()
                .filter(StringUtils::hasText).map(code -> code.trim().toUpperCase(Locale.ROOT)).collect(Collectors.toSet());

        List<ResourceLocationResponses.Option> matches = new ArrayList<>();
        List<ResourceLocationResponses.Option> alternatives = new ArrayList<>();
        List<Resource> candidates = resourceRepository.findAll().stream()
                .filter(ResourceFinder::bookable)
                .filter(resource -> resource.getCapacity() == null || resource.getCapacity() >= participants)
                .filter(resource -> !StringUtils.hasText(request.typeCode()) || (resource.getResourceType() != null
                        && request.typeCode().trim().equalsIgnoreCase(resource.getResourceType().getCode())))
                .sorted(Comparator.comparing((Resource r) -> r.getCapacity() == null ? Integer.MAX_VALUE : r.getCapacity())
                        .thenComparing(Resource::getCode))
                .toList();

        for (Resource resource : candidates) {
            List<String> notes = new ArrayList<>();
            boolean otherSite = StringUtils.hasText(request.siteCode())
                    && (resource.getSite() == null || !request.siteCode().trim().equalsIgnoreCase(resource.getSite().getCode()));
            if (otherSite) {
                notes.add("Autre site" + (resource.getSite() == null ? "" : " : " + resource.getSite().getName()));
            }
            Set<String> missing = missingAmenities(resource, wanted);
            if (!missing.isEmpty()) {
                notes.add("Sans : " + String.join(", ", missing));
            }
            int quantity = resource.isWholeResourceBooking() ? 1 : participants;
            if (free(resource, request.startedAt(), request.endedAt(), minutes, quantity)) {
                ResourcePriceQuoteResponse quote = quote(resource, request, participants);
                BigDecimal price = quote == null ? null : quote.amount();
                if (request.maxBudget() != null && price != null && price.compareTo(request.maxBudget()) > 0) {
                    notes.add("Au-dessus du budget");
                }
                ResourceLocationResponses.Option option = option(resource, request.startedAt(), request.endedAt(), quote, notes);
                if (notes.isEmpty()) {
                    if (matches.size() < MAX_MATCHES) matches.add(option);
                } else if (alternatives.size() < MAX_ALTERNATIVES) {
                    alternatives.add(option);
                }
            } else if (!otherSite && missing.isEmpty() && alternatives.size() < MAX_ALTERNATIVES) {
                // Bonne salle, mauvais horaire : le creneau libre le plus proche le meme jour.
                ResourceAvailabilityWindowResponse nearest = nearestWindow(resource, request.startedAt(), minutes, quantity);
                if (nearest != null) {
                    List<String> otherTime = new ArrayList<>(List.of("Autre horaire le même jour"));
                    ResourceLocationRequests.Find shifted = new ResourceLocationRequests.Find(nearest.getStartedAt(),
                            nearest.getEndedAt(), participants, request.amenityCodes(), request.siteCode(), request.typeCode(),
                            request.maxBudget(), request.ownerType(), request.ownerCode());
                    ResourcePriceQuoteResponse quote = quote(resource, shifted, participants);
                    if (request.maxBudget() != null && quote != null && quote.amount() != null
                            && quote.amount().compareTo(request.maxBudget()) > 0) {
                        otherTime.add("Au-dessus du budget");
                    }
                    alternatives.add(option(resource, nearest.getStartedAt(), nearest.getEndedAt(), quote, otherTime));
                }
            }
        }
        matches.sort(Comparator.comparing((ResourceLocationResponses.Option o) -> o.price() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : o.price()));
        return new ResourceLocationResponses.FindResult(matches, alternatives);
    }

    private static boolean bookable(Resource resource) {
        return !Boolean.TRUE.equals(resource.getDeleted()) && Boolean.TRUE.equals(resource.getActive())
                && Boolean.TRUE.equals(resource.getBookingEnabled()) && resource.getStatus() == ResourceStatus.ACTIVE;
    }

    private Set<String> missingAmenities(Resource resource, Set<String> wanted) {
        if (wanted.isEmpty()) {
            return Set.of();
        }
        Set<String> present = amenityLinks.findAllByResource(resource).stream()
                .filter(link -> link.getAmenity() != null && link.getAmenity().getCode() != null)
                .map(link -> link.getAmenity().getCode().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return wanted.stream().filter(code -> !present.contains(code)).sorted()
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private boolean free(Resource resource, LocalDateTime start, LocalDateTime end, int minutes, int quantity) {
        try {
            return availabilityService.findRemainingWindows(resource.getCode(), start, end, minutes, quantity, true).stream()
                    .anyMatch(window -> start.equals(window.getStartedAt()));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private ResourceAvailabilityWindowResponse nearestWindow(Resource resource, LocalDateTime start, int minutes, int quantity) {
        try {
            LocalDateTime dayStart = start.toLocalDate().atStartOfDay();
            return availabilityService.findRemainingWindows(resource.getCode(), dayStart, dayStart.plusDays(1), minutes, quantity, true)
                    .stream()
                    .min(Comparator.comparingLong(window -> Math.abs(Duration.between(start, window.getStartedAt()).toMinutes())))
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ResourcePriceQuoteResponse quote(Resource resource, ResourceLocationRequests.Find request, int participants) {
        try {
            return pricingService.quote(resource.getCode(), null, request.startedAt(), request.endedAt(), participants,
                    request.ownerType(), request.ownerCode(), null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static ResourceLocationResponses.Option option(Resource resource, LocalDateTime start, LocalDateTime end,
                                                           ResourcePriceQuoteResponse quote, List<String> notes) {
        return new ResourceLocationResponses.Option(resource.getCode(), resource.getName(),
                resource.getResourceType() == null ? null : resource.getResourceType().getCode(),
                resource.getSite() == null ? null : resource.getSite().getCode(),
                resource.getFloor() == null ? null : resource.getFloor().getCode(),
                resource.getCapacity(), start, end, quote == null ? null : quote.amount(),
                quote == null ? null : quote.currency(), List.copyOf(notes));
    }
}
