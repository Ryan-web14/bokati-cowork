package com.sni.bokaticowork.features.ressource.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.BulkCreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.BulkResourceOperationResponse;
import com.sni.bokaticowork.features.ressource.dto.response.PricingRuleConflictPreviewResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ResourcePricingRuleService {

    void createPricingRule(CreateResourcePricingRuleRequest request);

    /**
     * Pose la meme regle sur plusieurs ressources · une transaction par ressource, compte rendu
     * plutot que tout-ou-rien.
     */
    BulkResourceOperationResponse createPricingRuleBulk(BulkCreateResourcePricingRuleRequest request);

    /**
     * Montre, sans rien ecrire, ce que la regle viendrait concurrencer sur chaque ressource.
     * Un chevauchement est normal et arbitre par priorite · c'est le doublon strict qui merite
     * un regard.
     */
    PricingRuleConflictPreviewResponse previewPricingRuleBulk(BulkCreateResourcePricingRuleRequest request);

    ResourcePricingRuleResponse getPricingRule(Long id);

    PaginatedResponse<ResourcePricingRuleResponse> list(Pageable pageable);

    PaginatedResponse<ResourcePricingRuleResponse> listByResource(String resourceCode, Pageable pageable);

    ResourcePriceQuoteResponse quote(String resourceCode, String bookingUnit, LocalDateTime startedAt, LocalDateTime endedAt);

    ResourcePricingRuleResponse updatePricingRule(Long id, UpdateResourcePricingRuleRequest request);

    void updateActive(Long id, Boolean active);

    void deletePricingRule(Long id);
}
