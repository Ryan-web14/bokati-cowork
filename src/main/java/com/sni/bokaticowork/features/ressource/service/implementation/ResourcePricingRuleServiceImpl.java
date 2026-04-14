package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
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
                .active(rule.getActive())
                .build();
    }

    private ResourceBookingUnit parseUnit(String value) {
        try {
            return ResourceBookingUnit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid booking unit: " + value, ex);
        }
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
        return errors;
    }
}
