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
            throw new ResourceNotFoundException("Resource not found");
        }
        List<AmenityResponse> amenities = resourceService.listAmenities(code);
        List<ResourcePricingRuleResponse> pricingRules = resourcePricingRuleService
                .listByResource(code, Pageable.unpaged())
                .getData()
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getActive()))
                .toList();
        ResourcePolicy policy = resource.getResourcePolicy();
        ResourceType type = resource.getResourceType();
        ResourceGroup group = resource.getResourceGroup();
        return ClientResourceDetailResponse.builder()
                .code(resource.getCode())
                .name(resource.getName())
                .description(resource.getDescription())
                .typeCode(type != null ? type.getCode() : null)
                .typeName(type != null ? type.getName() : null)
                .groupCode(group != null ? group.getCode() : null)
                .groupName(group != null ? group.getName() : null)
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
                .build();
    }

    @Transactional(readOnly = true)
    public PublicResourceCalendarResponse getCalendar(String code, LocalDate fromDate, LocalDate toDate) {
        assertPortalVisible(code);
        return publicResourceCalendarService.calendar(code, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public List<ResourceAvailabilityWindowResponse> getAvailableSlots(String code, LocalDateTime startedAt,
                                                                       LocalDateTime endedAt,
                                                                       Integer durationMinutes,
                                                                       Integer quantity) {
        assertPortalVisible(code);
        return resourceAvailabilityService.findRemainingWindows(code, startedAt, endedAt, durationMinutes, quantity);
    }

    @Transactional(readOnly = true)
    public ResourcePriceQuoteResponse getQuote(String code, String bookingUnit,
                                                LocalDateTime startedAt, LocalDateTime endedAt) {
        assertPortalVisible(code);
        return resourcePricingRuleService.quote(code, bookingUnit, startedAt, endedAt);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertPortalVisible(String code) {
        Resource resource = resourceService.getResourceForService(code);
        if (!Boolean.TRUE.equals(resource.getPortalVisible())
                || resource.getStatus() != ResourceStatus.ACTIVE
                || !Boolean.TRUE.equals(resource.getBookingEnabled())) {
            throw new ResourceNotFoundException("Resource not found");
        }
    }

    private ClientResourceSummaryResponse toSummaryResponse(Resource resource) {
        ResourceType type = resource.getResourceType();
        ResourceGroup group = resource.getResourceGroup();
        return ClientResourceSummaryResponse.builder()
                .code(resource.getCode())
                .name(resource.getName())
                .description(resource.getDescription())
                .typeCode(type != null ? type.getCode() : null)
                .typeName(type != null ? type.getName() : null)
                .groupCode(group != null ? group.getCode() : null)
                .groupName(group != null ? group.getName() : null)
                .capacity(resource.getCapacity())
                .zone(resource.getZone())
                .locationLabel(resource.getLocationLabel())
                .displayOrder(resource.getDisplayOrder())
                .build();
    }
}
