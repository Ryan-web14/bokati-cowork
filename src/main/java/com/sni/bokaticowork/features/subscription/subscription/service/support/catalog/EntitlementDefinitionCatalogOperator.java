package com.sni.bokaticowork.features.subscription.subscription.service.support.catalog;

import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceGroupRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceTypeRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateEntitlementDefinitionRequest;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class EntitlementDefinitionCatalogOperator {

    private final EntitlementDefinitionRepository entitlementDefinitionRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final ResourceGroupRepository resourceGroupRepository;
    private final SubscriptionCodeFactory codeFactory;

    public EntitlementDefinition create(CreateEntitlementDefinitionRequest request) {
        String code = codeFactory.nextEntitlementDefinitionCode(request);
        if (entitlementDefinitionRepository.existsByCodeIgnoreCase(code)) {
            throw new ResourceAlreadyExistException("An entitlement definition with code " + code + " already exists");
        }

        ResourceType resourceType = StringUtils.hasText(request.resourceTypeCode())
                ? resourceTypeRepository.findByCode(request.resourceTypeCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Resource type not found"))
                : null;
        ResourceGroup resourceGroup = StringUtils.hasText(request.resourceGroupCode())
                ? resourceGroupRepository.findByCode(request.resourceGroupCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Resource group not found"))
                : null;

        return entitlementDefinitionRepository.save(EntitlementDefinition.builder()
                .code(code)
                .name(request.name().trim())
                .description(trim(request.description()))
                .entitlementType(request.entitlementType())
                .unit(request.unit())
                .consumptionMode(request.consumptionMode())
                .resetPolicy(request.resetPolicy())
                .stackable(request.stackable() == null || request.stackable())
                .transferable(Boolean.TRUE.equals(request.transferable()))
                .resourceType(resourceType)
                .resourceGroup(resourceGroup)
                .metadataJson(trim(request.metadataJson()))
                .active(request.active() == null || request.active())
                .build());
    }

    public EntitlementDefinition update(String code, CreateEntitlementDefinitionRequest request) {
        EntitlementDefinition definition = entitlementDefinitionRepository.findByCodeIgnoreCase(trim(code))
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement definition with code " + code + " not found"));

        ResourceType resourceType = StringUtils.hasText(request.resourceTypeCode())
                ? resourceTypeRepository.findByCode(request.resourceTypeCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Resource type not found"))
                : null;
        ResourceGroup resourceGroup = StringUtils.hasText(request.resourceGroupCode())
                ? resourceGroupRepository.findByCode(request.resourceGroupCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Resource group not found"))
                : null;

        definition.setName(request.name().trim());
        definition.setDescription(trim(request.description()));
        definition.setEntitlementType(request.entitlementType());
        definition.setUnit(request.unit());
        definition.setConsumptionMode(request.consumptionMode());
        definition.setResetPolicy(request.resetPolicy());
        definition.setStackable(request.stackable() == null || request.stackable());
        definition.setTransferable(Boolean.TRUE.equals(request.transferable()));
        definition.setResourceType(resourceType);
        definition.setResourceGroup(resourceGroup);
        definition.setMetadataJson(trim(request.metadataJson()));
        definition.setActive(request.active() == null || request.active());

        return entitlementDefinitionRepository.save(definition);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
