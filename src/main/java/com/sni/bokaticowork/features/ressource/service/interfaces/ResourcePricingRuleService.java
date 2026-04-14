package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import org.springframework.data.domain.Pageable;

public interface ResourcePricingRuleService {

    void createPricingRule(CreateResourcePricingRuleRequest request);

    ResourcePricingRuleResponse getPricingRule(Long id);

    PaginatedResponse<ResourcePricingRuleResponse> list(Pageable pageable);

    PaginatedResponse<ResourcePricingRuleResponse> listByResource(String resourceCode, Pageable pageable);

    void updateActive(Long id, Boolean active);

    void deletePricingRule(Long id);
}
