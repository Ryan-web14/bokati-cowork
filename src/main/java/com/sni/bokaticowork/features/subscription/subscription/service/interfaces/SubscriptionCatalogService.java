package com.sni.bokaticowork.features.subscription.subscription.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateEntitlementDefinitionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanVersionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.UpdatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementDefinitionResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanVersionResponse;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.EntitlementDefinitionSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PlanSearchCriteria;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubscriptionCatalogService {

    PlanResponse createPlan(CreatePlanRequest request);

    PlanResponse updatePlan(String planCode, UpdatePlanRequest request);

    void deletePlan(String planCode);

    PlanVersionResponse createVersion(String planCode, CreatePlanVersionRequest request);

    List<PlanVersionResponse> listVersions(String planCode);

    PlanVersionResponse publishVersion(String planCode, Long versionId);

    PlanVersionResponse getVersion(String planCode, Long versionId);

    PlanResponse getPlan(String planCode);

    PaginatedResponse<PlanResponse> listPlans(PlanSearchCriteria criteria, Pageable pageable);

    EntitlementDefinitionResponse createEntitlementDefinition(CreateEntitlementDefinitionRequest request);

    EntitlementDefinitionResponse updateEntitlementDefinition(String code, CreateEntitlementDefinitionRequest request);

    EntitlementDefinitionResponse getEntitlementDefinition(String code);

    List<EntitlementDefinitionResponse> basicSearchEntitlementDefinitions(String query);

    PaginatedResponse<EntitlementDefinitionResponse> listEntitlementDefinitions(EntitlementDefinitionSearchCriteria criteria, Pageable pageable);
}
