package com.sni.bokaticowork.features.portal.resource.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.dto.response.PublicResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourceSearchCriteria;
import com.sni.bokaticowork.features.ressource.repository.specification.specification.ResourceSpecification;
import com.sni.bokaticowork.features.ressource.service.interfaces.PublicResourceCalendarService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceGroupService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePricingRuleService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceTypeService;
import com.sni.bokaticowork.features.portal.resource.dto.response.ClientResourceDetailResponse;
import com.sni.bokaticowork.features.portal.resource.dto.response.ClientResourceSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceService resourceService;
    private final ResourceTypeService resourceTypeService;
    private final ResourceGroupService resourceGroupService;
    private final ResourcePricingRuleService resourcePricingRuleService;
    private final ResourceAvailabilityService resourceAvailabilityService;
    private final PublicResourceCalendarService publicResourceCalendarService;

    private com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService visibility;
    private com.sni.bokaticowork.features.portal.context.ClientContextService clientContext;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setVisibility(com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService visibility,
                       @org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.portal.context.ClientContextService clientContext) {
        this.visibility = visibility;
        this.clientContext = clientContext;
    }

    /** Lot B10 : contexte du membre connecte (identites, organisations, segments, plan). */
    private com.sni.bokaticowork.features.ressource.service.pricing.PriceContext memberContext() {
        if (visibility == null || clientContext == null) {
            return null;
        }
        try {
            var member = clientContext.getAuthenticatedMember();
            return visibility.context("MEMBER", member == null ? null : member.getMemberId());
        } catch (RuntimeException ex) {
            return visibility.context(null, null);
        }
    }

    private com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService.Access access(Resource resource) {
        var context = memberContext();
        return visibility == null || context == null
                ? com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService.Access.DIRECT
                : visibility.access(resource, context);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ClientResourceSummaryResponse> listPortalResources(
            String typeCode, String groupCode, String query, Integer minCapacity, Pageable pageable) {

        ResourceSearchCriteria criteria = ResourceSearchCriteria.builder()
                .portalVisible(Boolean.TRUE)
                .bookingEnabled(Boolean.TRUE)
                .active(Boolean.TRUE)
                .status(ResourceStatus.ACTIVE)
                .minCapacity(minCapacity)
                .build();

        if (StringUtils.hasText(typeCode)) {
            try {
                ResourceType type = resourceTypeService.getTypeForService(typeCode);
                criteria.setTypeId(type.getId());
            } catch (Exception ignored) { }
        }

        if (StringUtils.hasText(groupCode)) {
            try {
                ResourceGroup group = resourceGroupService.getResourceGroupForService(groupCode);
                criteria.setGroupId(group.getId());
            } catch (Exception ignored) { }
        }

        if (StringUtils.hasText(query)) {
            criteria.setName(query);
        }
        // Lot B10 : les ressources reservees n'apparaissent qu'a leur audience.
        var context = memberContext();
        if (visibility != null && context != null) {
            criteria.setRestrictedVisibleIds(visibility.restrictedVisibleIds(context));
        }

        Specification<Resource> spec = ResourceSpecification.search(criteria);
        Page<Resource> page = resourceRepository.findAll(spec, pageable);
        return new PaginatedResponse<>(page.map(this::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public ClientResourceDetailResponse getPortalResource(String code) {
        Resource resource = resourceService.getResourceForService(code);
        if (!Boolean.TRUE.equals(resource.getPortalVisible())
                || resource.getStatus() != ResourceStatus.ACTIVE
                || !Boolean.TRUE.equals(resource.getBookingEnabled())) {
            throw new ResourceNotFoundException("Ressource introuvable : " + code);
        }
        var resourceAccess = access(resource);
        if (resourceAccess == com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService.Access.HIDDEN) {
            throw new ResourceNotFoundException("Ressource introuvable : " + code);
        }
        List<AmenityResponse> amenities = resourceService.listAmenities(code);
        // Lot B10 : un prix reserve a d'autres clients n'est jamais montre.
        var context = memberContext();
        java.util.Set<String> keys = context == null ? java.util.Set.of() : context.audienceKeys();
        List<ResourcePricingRuleResponse> pricingRules = resourcePricingRuleService
                .listByResource(code, Pageable.unpaged())
                .getData()
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getActive()))
                .filter(r -> r.getAudience() == null || r.getAudience().isEmpty()
                        || r.getAudience().stream().anyMatch(ref -> keys.contains(ref.type() + ":" + ref.code())))
                .toList();
        ResourcePolicy policy = resource.getEffectivePolicy();
        ResourceType type = resource.getResourceType();
        return ClientResourceDetailResponse.builder()
                .code(resource.getCode())
                .name(resource.getName())
                .description(resource.getDescription())
                .typeName(type != null ? type.getName() : null)
                .capacity(resource.getCapacity())
                .zone(resource.getZone())
                .locationLabel(resource.getLocationLabel())
                .displayOrder(resource.getDisplayOrder())
                .minBookingDurationMinutes(policy != null ? policy.getMinBookingDurationMinutes() : null)
                .maxBookingDurationMinutes(policy != null ? policy.getMaxBookingDurationMinutes() : null)
                .minBookingNoticeMinutes(policy != null ? policy.getMinBookingNoticeMinutes() : null)
                .cancellationNoticeMinutes(policy != null ? policy.getCancellationNoticeMinutes() : null)
                .allowCancellation(policy != null ? policy.getAllowCancellation() : null)
                .amenities(amenities)
                .pricingRules(pricingRules)
                .bookingOnRequest(resourceAccess == com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService.Access.ON_REQUEST)
                .build();
    }

    @Transactional(readOnly = true)
    public PublicResourceCalendarResponse getCalendar(String code, LocalDate fromDate, LocalDate toDate,
                                                      boolean subscriber) {
        assertPortalVisible(code);
        return publicResourceCalendarService.calendar(code, fromDate, toDate, subscriber);
    }

    @Transactional(readOnly = true)
    public List<ResourceAvailabilityWindowResponse> getAvailableSlots(String code, LocalDateTime startedAt,
                                                                       LocalDateTime endedAt,
                                                                       Integer durationMinutes,
                                                                       Integer quantity,
                                                                       boolean subscriber) {
        assertPortalVisible(code);
        return resourceAvailabilityService.findRemainingWindows(code, startedAt, endedAt, durationMinutes, quantity,
                subscriber);
    }

    @Transactional(readOnly = true)
    public ResourcePriceQuoteResponse getQuote(String code, LocalDateTime startedAt, LocalDateTime endedAt,
                                                Integer quantity, String memberId) {
        assertPortalVisible(code);
        // Aucune unite imposee cote client : le meme calcul que la reservation du portail.
        return resourcePricingRuleService.quote(code, null, startedAt, endedAt, quantity,
                memberId == null ? null : "MEMBER", memberId, "PORTAL");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** La ressource est-elle proposee sur le portail · introuvable sinon, comme une ressource inexistante. */
    public void assertPortalVisible(String code) {
        Resource resource = resourceService.getResourceForService(code);
        if (!Boolean.TRUE.equals(resource.getPortalVisible())
                || resource.getStatus() != ResourceStatus.ACTIVE
                || !Boolean.TRUE.equals(resource.getBookingEnabled())
                || access(resource) == com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService.Access.HIDDEN) {
            throw new ResourceNotFoundException("Ressource introuvable : " + code);
        }
    }

    private ClientResourceSummaryResponse toSummaryResponse(Resource resource) {
        ResourceType type = resource.getResourceType();
        return ClientResourceSummaryResponse.builder()
                .code(resource.getCode())
                .name(resource.getName())
                .description(resource.getDescription())
                .typeName(type != null ? type.getName() : null)
                .capacity(resource.getCapacity())
                .zone(resource.getZone())
                .locationLabel(resource.getLocationLabel())
                .displayOrder(resource.getDisplayOrder())
                .build();
    }
}
