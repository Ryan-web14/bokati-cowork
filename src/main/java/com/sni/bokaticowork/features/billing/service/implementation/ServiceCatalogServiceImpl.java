package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateServiceCatalogItemRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateServiceCatalogItemRequest;
import com.sni.bokaticowork.features.billing.dto.response.CatalogLookupItemResponse;
import com.sni.bokaticowork.features.billing.dto.response.ServiceCatalogItemResponse;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.ServiceCatalogService;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private static final List<String> ALL_SOURCES = List.of("SERVICE_CATALOG", "INVENTORY_ITEM", "RESOURCE");

    private final ServiceCatalogItemRepository catalogItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ResourceRepository resourceRepository;
    private final ResourcePricingRuleRepository pricingRuleRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    @Transactional
    public ServiceCatalogItemResponse create(CreateServiceCatalogItemRequest request) {
        String itemCode = sequenceGenerator.next("service_catalog_item");
        ServiceCatalogItem item = ServiceCatalogItem.builder()
                .itemCode(itemCode)
                .name(request.name())
                .description(request.description())
                .category(request.category())
                .unit(request.unit())
                .unitPrice(request.unitPrice())
                .currency(request.currency() != null ? request.currency() : "XAF")
                .taxRuleCode(request.taxRuleCode())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .active(true)
                .build();
        return toResponse(catalogItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceCatalogItemResponse> list(Boolean active, String category, String searchText, Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return catalogItemRepository.search(active, category, searchText, unsortedPageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ServiceCatalogItemResponse update(String itemCode, UpdateServiceCatalogItemRequest request) {
        ServiceCatalogItem item = findByCode(itemCode);
        if (request.name() != null) item.setName(request.name());
        if (request.description() != null) item.setDescription(request.description());
        if (request.category() != null) item.setCategory(request.category());
        if (request.unit() != null) item.setUnit(request.unit());
        if (request.unitPrice() != null) item.setUnitPrice(request.unitPrice());
        if (request.currency() != null) item.setCurrency(request.currency());
        if (request.taxRuleCode() != null) item.setTaxRuleCode(request.taxRuleCode());
        if (request.displayOrder() != null) item.setDisplayOrder(request.displayOrder());
        item.setUpdatedAt(Instant.now());
        return toResponse(catalogItemRepository.save(item));
    }

    @Override
    @Transactional
    public ServiceCatalogItemResponse activate(String itemCode) {
        ServiceCatalogItem item = findByCode(itemCode);
        item.setActive(true);
        item.setUpdatedAt(Instant.now());
        return toResponse(catalogItemRepository.save(item));
    }

    @Override
    @Transactional
    public ServiceCatalogItemResponse deactivate(String itemCode) {
        ServiceCatalogItem item = findByCode(itemCode);
        item.setActive(false);
        item.setUpdatedAt(Instant.now());
        return toResponse(catalogItemRepository.save(item));
    }

    @Override
    @Transactional
    public void delete(String itemCode) {
        ServiceCatalogItem item = findByCode(itemCode);
        catalogItemRepository.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogLookupItemResponse> lookup(String q, List<String> sources, String category) {
        List<String> activeSources = (sources != null && !sources.isEmpty()) ? sources : ALL_SOURCES;
        List<CatalogLookupItemResponse> results = new ArrayList<>();

        if (activeSources.contains("SERVICE_CATALOG")) {
            catalogItemRepository
                    .search(true, category, q, PageRequest.of(0, 50))
                    .forEach(item -> results.add(fromServiceCatalogItem(item)));
        }

        if (activeSources.contains("INVENTORY_ITEM")) {
            inventoryItemRepository
                    .nativeSearch(q, category, null, true, PageRequest.of(0, 50))
                    .forEach(item -> results.add(fromInventoryItem(item)));
        }

        if (activeSources.contains("RESOURCE")) {
            List<Resource> resources;
            if (q != null && !q.isBlank()) {
                resources = resourceRepository.basicSearch(q);
            } else {
                resources = resourceRepository.findAll(PageRequest.of(0, 50)).getContent();
            }
            resources.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getActive()) && Boolean.TRUE.equals(r.getBookingEnabled()))
                    .forEach(r -> results.add(fromResource(r)));
        }

        return results;
    }

    private ServiceCatalogItem findByCode(String itemCode) {
        return catalogItemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Service catalog item not found: " + itemCode));
    }

    private ServiceCatalogItemResponse toResponse(ServiceCatalogItem item) {
        return new ServiceCatalogItemResponse(
                item.getItemCode(),
                item.getName(),
                item.getDescription(),
                item.getCategory(),
                item.getUnit(),
                item.getUnitPrice(),
                item.getCurrency(),
                item.getTaxRuleCode(),
                item.getActive(),
                item.getDisplayOrder(),
                item.getCreatedAt()
        );
    }

    private CatalogLookupItemResponse fromServiceCatalogItem(ServiceCatalogItem item) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("displayOrder", item.getDisplayOrder());
        return new CatalogLookupItemResponse(
                "SERVICE_CATALOG",
                item.getItemCode(),
                item.getName(),
                item.getDescription(),
                item.getCategory(),
                item.getUnit(),
                item.getUnitPrice(),
                item.getCurrency(),
                item.getTaxRuleCode(),
                meta
        );
    }

    private CatalogLookupItemResponse fromInventoryItem(InventoryItem item) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("itemType", item.getItemType() != null ? item.getItemType().name() : null);
        meta.put("trackingType", item.getTrackingType() != null ? item.getTrackingType().name() : null);
        meta.put("taxable", item.getTaxable());
        meta.put("psku", item.getPsku());

        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        String unitName = item.getUnit() != null ? item.getUnit().getName() : null;
        BigDecimal price = item.getSalePrice() != null
                ? BigDecimal.valueOf(item.getSalePrice())
                : null;

        return new CatalogLookupItemResponse(
                "INVENTORY_ITEM",
                item.getItemCode(),
                item.getName(),
                item.getDescription(),
                categoryName,
                unitName,
                price,
                null,
                null,
                meta
        );
    }

    private CatalogLookupItemResponse fromResource(Resource resource) {
        Optional<ResourcePricingRule> rule = pricingRuleRepository.findLatestActiveByResourceId(resource.getId());

        Map<String, Object> meta = new HashMap<>();
        meta.put("capacity", resource.getCapacity());
        meta.put("zone", resource.getZone());
        meta.put("locationLabel", resource.getLocationLabel());
        meta.put("resourceType", resource.getResourceType() != null ? resource.getResourceType().getName() : null);
        rule.ifPresent(r -> {
            meta.put("pricingUnit", r.getResourceBookingUnit() != null ? r.getResourceBookingUnit().name() : null);
            meta.put("pricingLabel", r.getLabel());
        });

        BigDecimal unitPrice = rule.map(r -> r.getPrice() != null ? BigDecimal.valueOf(r.getPrice()) : null)
                .orElse(null);
        String unit = rule.map(r -> r.getResourceBookingUnit() != null ? r.getResourceBookingUnit().name() : null)
                .orElse(null);

        return new CatalogLookupItemResponse(
                "RESOURCE",
                resource.getCode(),
                resource.getName(),
                resource.getDescription(),
                resource.getResourceType() != null ? resource.getResourceType().getName() : null,
                unit,
                unitPrice,
                null,
                null,
                meta
        );
    }
}
