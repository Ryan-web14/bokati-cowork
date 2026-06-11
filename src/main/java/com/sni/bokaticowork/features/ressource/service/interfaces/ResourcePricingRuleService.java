package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ResourcePricingRuleService {

    void createPricingRule(CreateResourcePricingRuleRequest request);

    ResourcePricingRuleResponse getPricingRule(Long id);

    PaginatedResponse<ResourcePricingRuleResponse> list(Pageable pageable);

    PaginatedResponse<ResourcePricingRuleResponse> listByResource(String resourceCode, Pageable pageable);

    ResourcePriceQuoteResponse quote(String resourceCode, String bookingUnit, LocalDateTime startedAt, LocalDateTime endedAt);

    ResourcePricingRuleResponse updatePricingRule(Long id, UpdateResourcePricingRuleRequest request);

    void updateActive(Long id, Boolean active);

    void deletePricingRule(Long id);
}
