package com.sni.bokaticowork.features.subscription.subscription.service.implementation;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateEntitlementDefinitionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanVersionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.UpdatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementDefinitionResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanVersionResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionCatalogMapper;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionPlanRepository;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.EntitlementDefinitionSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PlanSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.specification.PlanSpecification;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionCatalogService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.catalog.EntitlementDefinitionCatalogOperator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.catalog.PlanCatalogOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionCatalogServiceImpl implements SubscriptionCatalogService {

    private final SubscriptionPlanRepository planRepository;
    private final EntitlementDefinitionRepository entitlementDefinitionRepository;
    private final SubscriptionCatalogMapper responseMapper;
    private final PlanCatalogOperator planCatalogOperator;
    private final EntitlementDefinitionCatalogOperator entitlementDefinitionCatalogOperator;

    @Override
    public PlanResponse createPlan(CreatePlanRequest request) {
        return responseMapper.toPlanResponse(planCatalogOperator.createPlan(request), true);
    }

    @Override
    public PlanResponse updatePlan(String planCode, UpdatePlanRequest request) {
        return responseMapper.toPlanResponse(planCatalogOperator.updatePlan(planCode, request), true);
    }

    @Override
    public void deletePlan(String planCode) {
        planCatalogOperator.deletePlan(planCode);
    }

    @Override
    public PlanVersionResponse createVersion(String planCode, CreatePlanVersionRequest request) {
        return responseMapper.toPlanVersionResponse(planCatalogOperator.createVersion(planCode, request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanVersionResponse> listVersions(String planCode) {
        return planCatalogOperator.listVersionsForService(planCode).stream()
                .map(responseMapper::toPlanVersionResponse)
                .toList();
    }

    @Override
    public PlanVersionResponse publishVersion(String planCode, Long versionId) {
        return responseMapper.toPlanVersionResponse(planCatalogOperator.publishVersion(planCode, versionId));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanVersionResponse getVersion(String planCode, Long versionId) {
        return responseMapper.toPlanVersionResponse(planCatalogOperator.getVersionForService(planCode, versionId));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlan(String planCode) {
        return responseMapper.toPlanResponse(planCatalogOperator.getPlanForService(planCode), true);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PlanResponse> listPlans(PlanSearchCriteria criteria, Pageable pageable) {
        return new PaginatedResponse<>(planRepository.findAll(PlanSpecification.search(criteria), pageable)
                .map(plan -> responseMapper.toPlanResponse(plan, false)));
    }

    @Override
    public EntitlementDefinitionResponse createEntitlementDefinition(CreateEntitlementDefinitionRequest request) {
        return responseMapper.toEntitlementDefinitionResponse(entitlementDefinitionCatalogOperator.create(request));
    }

    @Override
    public EntitlementDefinitionResponse updateEntitlementDefinition(String code, CreateEntitlementDefinitionRequest request) {
        return responseMapper.toEntitlementDefinitionResponse(entitlementDefinitionCatalogOperator.update(code, request));
    }

    @Override
    @Transactional(readOnly = true)
    public EntitlementDefinitionResponse getEntitlementDefinition(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ResourceNotFoundException("Entitlement definition code is required");
        }
        return entitlementDefinitionRepository.findByCodeIgnoreCase(code.trim())
                .map(responseMapper::toEntitlementDefinitionResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement definition with code " + code + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntitlementDefinitionResponse> basicSearchEntitlementDefinitions(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return entitlementDefinitionRepository.basicSearch(query.trim()).stream()
                .map(responseMapper::toEntitlementDefinitionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<EntitlementDefinitionResponse> listEntitlementDefinitions(EntitlementDefinitionSearchCriteria criteria, Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<EntitlementDefinitionResponse> page = entitlementDefinitionRepository.nativeSearch(
                        trim(criteria == null ? null : criteria.getQuery()),
                        trim(criteria == null ? null : criteria.getCode()),
                        trim(criteria == null ? null : criteria.getName()),
                        criteria == null || criteria.getEntitlementType() == null ? null : criteria.getEntitlementType().name(),
                        criteria == null || criteria.getUnit() == null ? null : criteria.getUnit().name(),
                        criteria == null || criteria.getConsumptionMode() == null ? null : criteria.getConsumptionMode().name(),
                        criteria == null || criteria.getResetPolicy() == null ? null : criteria.getResetPolicy().name(),
                        trim(criteria == null ? null : criteria.getResourceTypeCode()),
                        trim(criteria == null ? null : criteria.getResourceGroupCode()),
                        criteria == null ? null : criteria.getStackable(),
                        criteria == null ? null : criteria.getTransferable(),
                        criteria == null ? null : criteria.getActive(),
                        unsortedPageable
                )
                .map(responseMapper::toEntitlementDefinitionResponse);
        return new PaginatedResponse<>(page);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
