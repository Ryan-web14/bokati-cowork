package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.enums.ResourcePriceAdjustmentType;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePricingRuleService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class ResourcePricingRuleServiceImpl implements ResourcePricingRuleService {

    private final ResourcePricingRuleRepository pricingRuleRepository;
    private final ResourceService resourceService;

    @Override
    public void createPricingRule(CreateResourcePricingRuleRequest request) {
        List<String> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource pricing rule request", errors);
        }

        Resource resource = resourceService.getResourceForService(request.getResourceCode().trim());
        ResourcePricingRule rule = ResourcePricingRule.builder()
                .resource(resource)
                .resourceBookingUnit(parseUnit(request.getBookingUnit()))
                .price(request.getPrice())
                .label(request.getLabel())
                .dayOfWeek(request.getDayOfWeek())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .adjustmentType(parseAdjustmentType(request.getAdjustmentType()))
                .adjustmentValue(request.getAdjustmentValue())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .lastMinuteMinutes(request.getLastMinuteMinutes())
                .priority(request.getPriority() == null ? 0 : request.getPriority())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        pricingRuleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourcePricingRuleResponse getPricingRule(Long id) {
        return toResponse(getPricingRuleForService(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourcePricingRuleResponse> list(Pageable pageable) {
        Page<ResourcePricingRuleResponse> page = pricingRuleRepository.findAll(pageable).map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourcePricingRuleResponse> listByResource(String resourceCode, Pageable pageable) {
        Resource resource = resourceService.getResourceForService(resourceCode);
        Page<ResourcePricingRuleResponse> page = pricingRuleRepository.findAllByResource(resource, pageable).map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourcePriceQuoteResponse quote(String resourceCode, String bookingUnit, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (!StringUtils.hasText(resourceCode) || !StringUtils.hasText(bookingUnit) || startedAt == null || endedAt == null) {
            throw new BadRequestException("Resource code, booking unit, start and end dates are required");
        }
        if (!endedAt.isAfter(startedAt)) {
            throw new BadRequestException("End date must be after start date");
        }
        Resource resource = resourceService.getResourceForService(resourceCode);
        validateAgainstPolicy(resource, startedAt, endedAt);
        ResourceBookingUnit unit = parseUnit(bookingUnit);
        ResourcePricingRule baseRule = pricingRuleRepository.findAllActiveByResourceId(resource.getId()).stream()
                .filter(rule -> unit.equals(rule.getResourceBookingUnit()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No active pricing rule found for resource " + resourceCode));

        List<ResourcePricingRule> candidates = pricingRuleRepository.findApplicableRules(
                resource,
                unit,
                startedAt.toLocalDate(),
                startedAt.getDayOfWeek().getValue(),
                startedAt.toLocalTime()
        );
        ResourcePricingRule applied = candidates.stream()
                .filter(rule -> appliesLastMinute(rule, startedAt))
                .findFirst()
                .orElse(baseRule);

        Integer unitPrice = applyAdjustment(baseRule.getPrice(), applied);
        long quantity = quantityFor(unit, startedAt, endedAt);

        return ResourcePriceQuoteResponse.builder()
                .resourceCode(resource.getCode())
                .bookingUnit(unit.name())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .basePrice(baseRule.getPrice())
                .finalPrice(unitPrice * (int) quantity)
                .appliedRuleId(applied.getId())
                .appliedRuleLabel(applied.getLabel())
                .adjustmentType(applied.getAdjustmentType() == null ? null : applied.getAdjustmentType().name())
                .adjustmentValue(applied.getAdjustmentValue())
                .build();
    }

    @Override
    public ResourcePricingRuleResponse updatePricingRule(Long id, UpdateResourcePricingRuleRequest request) {
        ResourcePricingRule rule = getPricingRuleForService(id);
        if (request.getBookingUnit() != null) {
            rule.setResourceBookingUnit(parseUnit(request.getBookingUnit()));
        }
        if (request.getPrice() != null) {
            if (request.getPrice() < 0) throw new BadRequestException("Price must be zero or greater");
            rule.setPrice(request.getPrice());
        }
        if (request.getLabel() != null)            rule.setLabel(request.getLabel());
        if (request.getDayOfWeek() != null)        rule.setDayOfWeek(request.getDayOfWeek());
        if (request.getStartsAt() != null)         rule.setStartsAt(request.getStartsAt());
        if (request.getEndsAt() != null)           rule.setEndsAt(request.getEndsAt());
        if (request.getAdjustmentType() != null)   rule.setAdjustmentType(parseAdjustmentType(request.getAdjustmentType()));
        if (request.getAdjustmentValue() != null)  rule.setAdjustmentValue(request.getAdjustmentValue());
        if (request.getValidFrom() != null)        rule.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null)       rule.setValidUntil(request.getValidUntil());
        if (request.getLastMinuteMinutes() != null) rule.setLastMinuteMinutes(request.getLastMinuteMinutes());
        if (request.getPriority() != null)         rule.setPriority(request.getPriority());
        if (request.getActive() != null)           rule.setActive(request.getActive());
        return toResponse(pricingRuleRepository.save(rule));
    }

    @Override
    public void updateActive(Long id, Boolean active) {
        if (active == null) {
            throw new BadRequestException("Pricing rule active flag is required");
        }
        ResourcePricingRule rule = getPricingRuleForService(id);
        rule.setActive(active);
        pricingRuleRepository.save(rule);
    }

    @Override
    public void deletePricingRule(Long id) {
        pricingRuleRepository.delete(getPricingRuleForService(id));
    }

    private ResourcePricingRule getPricingRuleForService(Long id) {
        if (id == null) {
            throw new BadRequestException("Pricing rule id is required");
        }
        return pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource pricing rule with id " + id + " not found"));
    }

    private ResourcePricingRuleResponse toResponse(ResourcePricingRule rule) {
        return ResourcePricingRuleResponse.builder()
                .id(rule.getId())
                .resourceCode(rule.getResource().getCode())
                .bookingUnit(rule.getResourceBookingUnit() == null ? null : rule.getResourceBookingUnit().name())
                .price(rule.getPrice())
                .label(rule.getLabel())
                .dayOfWeek(rule.getDayOfWeek())
                .startsAt(rule.getStartsAt())
                .endsAt(rule.getEndsAt())
                .adjustmentType(rule.getAdjustmentType() == null ? null : rule.getAdjustmentType().name())
                .adjustmentValue(rule.getAdjustmentValue())
                .validFrom(rule.getValidFrom())
                .validUntil(rule.getValidUntil())
                .lastMinuteMinutes(rule.getLastMinuteMinutes())
                .priority(rule.getPriority())
                .active(rule.getActive())
                .build();
    }

    private void validateAgainstPolicy(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt) {
        ResourcePolicy policy = resource.getResourcePolicy();
        if (policy == null) {
            return;
        }
        long durationMinutes = java.time.Duration.between(startedAt, endedAt).toMinutes();
        if (policy.getMinBookingDurationMinutes() != null && durationMinutes < policy.getMinBookingDurationMinutes()) {
            throw new BadRequestException("Booking duration (" + durationMinutes + " min) is below the minimum allowed (" + policy.getMinBookingDurationMinutes() + " min)");
        }
        if (policy.getMaxBookingDurationMinutes() != null && durationMinutes > policy.getMaxBookingDurationMinutes()) {
            throw new BadRequestException("Booking duration (" + durationMinutes + " min) exceeds the maximum allowed (" + policy.getMaxBookingDurationMinutes() + " min)");
        }
        if (policy.getMinBookingNoticeMinutes() != null && policy.getMinBookingNoticeMinutes() > 0) {
            long noticeMinutes = java.time.Duration.between(LocalDateTime.now(), startedAt).toMinutes();
            if (noticeMinutes < policy.getMinBookingNoticeMinutes()) {
                throw new BadRequestException("Booking requires at least " + policy.getMinBookingNoticeMinutes() + " minutes advance notice");
            }
        }
    }

    private ResourceBookingUnit parseUnit(String value) {
        try {
            return ResourceBookingUnit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid booking unit: " + value, ex);
        }
    }

    private ResourcePriceAdjustmentType parseAdjustmentType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ResourcePriceAdjustmentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid adjustment type: " + value, ex);
        }
    }

    private boolean appliesLastMinute(ResourcePricingRule rule, LocalDateTime startedAt) {
        return rule.getLastMinuteMinutes() == null || !LocalDateTime.now().plusMinutes(rule.getLastMinuteMinutes()).isBefore(startedAt);
    }

    private Integer applyAdjustment(Integer basePrice, ResourcePricingRule applied) {
        if (applied.getAdjustmentType() == null) {
            return applied.getPrice();
        }
        Integer adjustment = applied.getAdjustmentValue() == null ? 0 : applied.getAdjustmentValue();
        return switch (applied.getAdjustmentType()) {
            case FIXED_PRICE -> adjustment;
            case AMOUNT_DELTA -> Math.max(0, basePrice + adjustment);
            case PERCENT_DELTA -> Math.max(0, basePrice + (basePrice * adjustment / 100));
        };
    }

    private long quantityFor(ResourceBookingUnit unit, LocalDateTime startedAt, LocalDateTime endedAt) {
        long minutes = ChronoUnit.MINUTES.between(startedAt, endedAt);
        return switch (unit) {
            case HOUR     -> Math.max(1, (long) Math.ceil(minutes / 60.0));
            case HALF_DAY -> Math.max(1, (long) Math.ceil(minutes / (60.0 * 12)));
            case DAY      -> Math.max(1, (long) Math.ceil(minutes / (60.0 * 24)));
            case WEEK     -> Math.max(1, (long) Math.ceil(minutes / (60.0 * 24 * 7)));
            case MONTH    -> Math.max(1, ChronoUnit.MONTHS.between(startedAt, endedAt)
                    + (startedAt.plusMonths(ChronoUnit.MONTHS.between(startedAt, endedAt)).isBefore(endedAt) ? 1 : 0));
        };
    }

    private List<String> validateRequest(CreateResourcePricingRuleRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Invalid resource pricing rule request, the request body is required");
            return errors;
        }
        if (!StringUtils.hasText(request.getResourceCode())) {
            errors.add("Invalid resource pricing rule request, the resource code is required");
        }
        if (!StringUtils.hasText(request.getBookingUnit())) {
            errors.add("Invalid resource pricing rule request, the booking unit is required");
        }
        if (request.getPrice() == null || request.getPrice() < 0) {
            errors.add("Invalid resource pricing rule request, the price must be zero or greater");
        }
        if (request.getDayOfWeek() != null && (request.getDayOfWeek() < 1 || request.getDayOfWeek() > 7)) {
            errors.add("Invalid resource pricing rule request, dayOfWeek must be between 1 and 7");
        }
        if (request.getStartsAt() != null && request.getEndsAt() != null && !request.getEndsAt().isAfter(request.getStartsAt())) {
            errors.add("Invalid resource pricing rule request, endsAt must be after startsAt");
        }
        if (request.getValidFrom() != null && request.getValidUntil() != null && request.getValidUntil().isBefore(request.getValidFrom())) {
            errors.add("Invalid resource pricing rule request, validUntil must be after validFrom");
        }
        return errors;
    }
}
