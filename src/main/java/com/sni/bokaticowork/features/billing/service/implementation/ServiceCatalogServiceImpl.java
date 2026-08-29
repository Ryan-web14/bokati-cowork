package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ServiceCatalogItemResponse create(CreateServiceCatalogItemRequest request) {
        validateBounds(request.minQuantity(), request.maxQuantity(),
                request.floorPrice(), request.unitPrice());
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
                .costPrice(request.costPrice())
                .floorPrice(request.floorPrice())
                .minMarginRate(request.minMarginRate())
                .maxDiscountRate(request.maxDiscountRate())
                .discountPolicy(normalizePolicy(request.discountPolicy()))
                .detailedDescription(request.detailedDescription())
                .includedItems(toJson(request.includedItems()))
                .imageUrl(request.imageUrl())
                .billingMode(normalizeBillingMode(request.billingMode()))
                .defaultQuantity(request.defaultQuantity())
                .minQuantity(request.minQuantity())
                .maxQuantity(request.maxQuantity())
                .taxableByDefault(request.taxableByDefault())
                .subcategory(request.subcategory())
                .tags(toJson(request.tags()))
                .externalReference(request.externalReference())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
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

        if (request.costPrice() != null) item.setCostPrice(request.costPrice());
        if (request.floorPrice() != null) item.setFloorPrice(request.floorPrice());
        if (request.minMarginRate() != null) item.setMinMarginRate(request.minMarginRate());
        if (request.maxDiscountRate() != null) item.setMaxDiscountRate(request.maxDiscountRate());
        if (request.discountPolicy() != null) item.setDiscountPolicy(normalizePolicy(request.discountPolicy()));
        if (request.detailedDescription() != null) item.setDetailedDescription(request.detailedDescription());
        if (request.includedItems() != null) item.setIncludedItems(toJson(request.includedItems()));
        if (request.imageUrl() != null) item.setImageUrl(request.imageUrl());
        if (request.billingMode() != null) item.setBillingMode(normalizeBillingMode(request.billingMode()));
        if (request.defaultQuantity() != null) item.setDefaultQuantity(request.defaultQuantity());
        if (request.minQuantity() != null) item.setMinQuantity(request.minQuantity());
        if (request.maxQuantity() != null) item.setMaxQuantity(request.maxQuantity());
        if (request.taxableByDefault() != null) item.setTaxableByDefault(request.taxableByDefault());
        if (request.subcategory() != null) item.setSubcategory(request.subcategory());
        if (request.tags() != null) item.setTags(toJson(request.tags()));
        if (request.externalReference() != null) item.setExternalReference(request.externalReference());
        if (request.validFrom() != null) item.setValidFrom(request.validFrom());
        if (request.validUntil() != null) item.setValidUntil(request.validUntil());

        // Un champ absent est conserve · sans ce mecanisme, un plancher pose par erreur ne
        // pourrait plus jamais etre retire par l'API.
        applyClearFields(item, request.clearFields());

        validateBounds(item.getMinQuantity(), item.getMaxQuantity(),
                item.getFloorPrice(), item.getUnitPrice());
        item.setUpdatedAt(Instant.now());
        return toResponse(catalogItemRepository.save(item));
    }

    private void applyClearFields(ServiceCatalogItem item, List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (String field : fields) {
            switch (field == null ? "" : field.trim()) {
                case "costPrice" -> item.setCostPrice(null);
                case "floorPrice" -> item.setFloorPrice(null);
                case "minMarginRate" -> item.setMinMarginRate(null);
                case "maxDiscountRate" -> item.setMaxDiscountRate(null);
                case "detailedDescription" -> item.setDetailedDescription(null);
                case "includedItems" -> item.setIncludedItems(null);
                case "imageUrl" -> item.setImageUrl(null);
                case "billingMode" -> item.setBillingMode(null);
                case "defaultQuantity" -> item.setDefaultQuantity(null);
                case "minQuantity" -> item.setMinQuantity(null);
                case "maxQuantity" -> item.setMaxQuantity(null);
                case "taxableByDefault" -> item.setTaxableByDefault(null);
                case "subcategory" -> item.setSubcategory(null);
                case "tags" -> item.setTags(null);
                case "externalReference" -> item.setExternalReference(null);
                case "validFrom" -> item.setValidFrom(null);
                case "validUntil" -> item.setValidUntil(null);
                default -> throw new BadRequestException(
                        "Champ non effacable : " + field + ". Champs admis : costPrice, floorPrice, "
                                + "minMarginRate, maxDiscountRate, detailedDescription, includedItems, imageUrl, "
                                + "billingMode, defaultQuantity, minQuantity, maxQuantity, taxableByDefault, "
                                + "subcategory, tags, externalReference, validFrom, validUntil");
            }
        }
    }

    /**
     * Coherence des bornes. Un plancher au-dessus du prix de vente rendrait tout article
     * invendable des la premiere ligne : autant le refuser a la saisie.
     */
    private void validateBounds(BigDecimal minQuantity, BigDecimal maxQuantity,
                                BigDecimal floorPrice, BigDecimal unitPrice) {
        if (minQuantity != null && maxQuantity != null && minQuantity.compareTo(maxQuantity) > 0) {
            throw new BadRequestException("La quantite minimale ne peut pas depasser la quantite maximale");
        }
        if (floorPrice != null && unitPrice != null && floorPrice.compareTo(unitPrice) > 0) {
            throw new BadRequestException(
                    "Le prix plancher (" + floorPrice.toPlainString() + ") depasse le prix de vente ("
                            + unitPrice.toPlainString() + ") · l'article serait invendable");
        }
    }

    private String normalizePolicy(String policy) {
        if (policy == null || policy.isBlank()) {
            return "NONE";
        }
        String normalized = policy.trim().toUpperCase(Locale.ROOT);
        if (!List.of("NONE", "WARN", "BLOCK").contains(normalized)) {
            throw new BadRequestException("discountPolicy doit valoir NONE, WARN ou BLOCK · recu : " + policy);
        }
        return normalized;
    }

    private String normalizeBillingMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return null;
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        if (!List.of("UNIT", "HOURLY", "DAILY", "MONTHLY", "FIXED").contains(normalized)) {
            throw new BadRequestException(
                    "billingMode doit valoir UNIT, HOURLY, DAILY, MONTHLY ou FIXED · recu : " + mode);
        }
        return normalized;
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return null;
        }
        List<String> cleaned = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Liste illisible : " + ex.getOriginalMessage());
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            // Une valeur illisible en base ne doit pas rendre l'article inconsultable.
            return List.of();
        }
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
                item.getCreatedAt(),

                item.getCostPrice(),
                item.getFloorPrice(),
                item.getMinMarginRate(),
                item.getMaxDiscountRate(),
                item.getDiscountPolicy(),
                item.effectiveFloorPrice(),
                floorPriceOrigin(item),

                item.getDetailedDescription(),
                fromJson(item.getIncludedItems()),
                item.getImageUrl(),

                item.getBillingMode(),
                item.getDefaultQuantity(),
                item.getMinQuantity(),
                item.getMaxQuantity(),
                item.getTaxableByDefault(),

                item.getSubcategory(),
                fromJson(item.getTags()),
                item.getExternalReference(),
                item.getValidFrom(),
                item.getValidUntil(),
                currentlyValid(item)
        );
    }

    /**
     * D'ou vient le plancher · sert a expliquer un refus plutot qu'a opposer un nombre nu.
     * Suit exactement l'ordre de resolution de {@link ServiceCatalogItem#effectiveFloorPrice()}.
     */
    private String floorPriceOrigin(ServiceCatalogItem item) {
        if (item.getFloorPrice() != null) {
            return "EXPLICIT";
        }
        if (item.getCostPrice() == null) {
            return "NONE";
        }
        return item.getMinMarginRate() != null && item.getMinMarginRate().signum() > 0
                ? "DERIVED_FROM_MARGIN"
                : "COST_PRICE";
    }

    private Boolean currentlyValid(ServiceCatalogItem item) {
        LocalDate today = LocalDate.now();
        boolean started = item.getValidFrom() == null || !today.isBefore(item.getValidFrom());
        boolean notEnded = item.getValidUntil() == null || !today.isAfter(item.getValidUntil());
        return started && notEnded;
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
