package com.sni.bokaticowork.features.inventory.intelligence.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.intelligence.dto.request.InventoryReorderRuleRequest;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryReorderRuleResponse;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.ReorderSuggestionResponse;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionReason;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionSeverity;
import com.sni.bokaticowork.features.inventory.intelligence.mapper.interfaces.InventoryIntelligenceMapper;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryReorderRule;
import com.sni.bokaticowork.features.inventory.intelligence.repository.InventoryReorderRuleRepository;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryReorderRuleService;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryReorderRuleServiceImpl implements InventoryReorderRuleService {

    private final InventoryReorderRuleRepository repository;
    private final StockLevelRepository stockLevelRepository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryLocationService locationService;
    private final InventoryIntelligenceMapper mapper;

    @Override
    public InventoryReorderRuleResponse createOrUpdate(InventoryReorderRuleRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        InventoryLocation location = StringUtils.hasText(request.getLocationCode())
                ? locationService.findByLocationCodeOrThrow(request.getLocationCode())
                : null;
        BigDecimal minQuantity = defaultMinQuantity(request);
        BigDecimal maxQuantity = request.getMaxQuantity();
        BigDecimal reorderQuantity = defaultReorderQuantity(minQuantity, maxQuantity, request);
        validate(minQuantity, maxQuantity, reorderQuantity);
        InventoryReorderRule rule = repository.findByItemAndLocation(item, location).orElseGet(InventoryReorderRule::new);
        rule.setItem(item);
        rule.setLocation(location);
        rule.setMinQuantity(minQuantity);
        rule.setMaxQuantity(maxQuantity);
        rule.setReorderQuantity(reorderQuantity);
        rule.setPreferredSupplierCode(normalizeOptionalCode(request.getPreferredSupplierCode()));
        rule.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        return mapper.toRuleResponse(repository.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReorderRuleResponse> list() {
        return repository.findAll().stream().map(mapper::toRuleResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReorderSuggestionResponse> suggestions(String itemCode, String locationCode, ReorderSuggestionSeverity severity) {
        String normalizedItemCode = normalizeOptionalCode(itemCode);
        String normalizedLocationCode = normalizeOptionalCode(locationCode);
        return repository.findAllActiveRules().stream()
                .filter(rule -> normalizedItemCode == null || rule.getItem().getItemCode().equalsIgnoreCase(normalizedItemCode))
                .filter(rule -> normalizedLocationCode == null
                        || (rule.getLocation() != null && rule.getLocation().getLocationCode().equalsIgnoreCase(normalizedLocationCode)))
                .flatMap(rule -> suggestionsForRule(rule).stream())
                .filter(suggestion -> severity == null || suggestion.getSeverity() == severity)
                .sorted(Comparator.comparing(ReorderSuggestionResponse::getPriorityScore, Comparator.reverseOrder())
                        .thenComparing(ReorderSuggestionResponse::getItemName)
                        .thenComparing(response -> response.getLocationName() == null ? "" : response.getLocationName()))
                .toList();
    }

    private List<ReorderSuggestionResponse> suggestionsForRule(InventoryReorderRule rule) {
        if (rule.getLocation() != null) {
            return stockLevelRepository.findByItemAndLocation(rule.getItem(), rule.getLocation())
                    .map(level -> level.getQuantityAvailable().compareTo(rule.getMinQuantity()) <= 0
                            ? List.of(toSuggestion(rule, level))
                            : List.<ReorderSuggestionResponse>of())
                    .orElseGet(() -> List.of(toMissingStockSuggestion(rule, rule.getLocation())));
        }
        List<StockLevel> levels = stockLevelRepository.findAllByItemIdOrderByQuantityAvailableAsc(rule.getItem().getId());
        if (levels.isEmpty()) {
            return List.of(toMissingStockSuggestion(rule, null));
        }
        return levels.stream()
                .filter(level -> level.getQuantityAvailable().compareTo(rule.getMinQuantity()) <= 0)
                .map(level -> toSuggestion(rule, level))
                .toList();
    }

    private ReorderSuggestionResponse toMissingStockSuggestion(InventoryReorderRule rule, InventoryLocation location) {
        return toSuggestion(rule, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, location, ReorderSuggestionReason.NO_STOCK_LEVEL);
    }

    private ReorderSuggestionResponse toSuggestion(InventoryReorderRule rule, StockLevel level) {
        ReorderSuggestionReason reason = reasonFor(level.getQuantityAvailable(), rule.getMinQuantity());
        return toSuggestion(rule, level.getQuantityAvailable(), level.getQuantityOnHand(), level.getQuantityReserved(), level.getLocation(), reason);
    }

    private ReorderSuggestionResponse toSuggestion(InventoryReorderRule rule,
                                                   BigDecimal currentQuantity,
                                                   BigDecimal quantityOnHand,
                                                   BigDecimal quantityReserved,
                                                   InventoryLocation location,
                                                   ReorderSuggestionReason reasonCode) {
        BigDecimal targetQuantity = targetQuantity(rule, currentQuantity);
        BigDecimal quantity = targetQuantity.subtract(currentQuantity).max(rule.getReorderQuantity());
        BigDecimal shortageQuantity = rule.getMinQuantity().subtract(currentQuantity).max(BigDecimal.ZERO);
        ReorderSuggestionSeverity severity = severityFor(reasonCode, currentQuantity, rule.getMinQuantity());
        int priorityScore = priorityScore(severity, shortageQuantity, rule.getMinQuantity(), reasonCode);
        Long estimatedOrderCost = estimatedOrderCost(rule, quantity);
        String reason = reasonText(reasonCode, currentQuantity, rule.getMinQuantity(), targetQuantity);
        InventoryLocation responseLocation = location;
        if (responseLocation == null) {
            responseLocation = rule.getLocation();
        }
        var item = rule.getItem();
        var category = item.getCategory();
        var unit = item.getUnit();
        return ReorderSuggestionResponse.builder()
                .itemCode(item.getItemCode())
                .itemName(item.getName())
                .itemType(item.getItemType())
                .categoryCode(category == null ? null : category.getCode())
                .categoryName(category == null ? null : category.getName())
                .unitCode(unit == null ? null : unit.getCode())
                .unitName(unit == null ? null : unit.getName())
                .locationCode(responseLocation == null ? null : responseLocation.getLocationCode())
                .locationName(responseLocation == null ? null : responseLocation.getName())
                .ruleScope(rule.getLocation() == null ? "GLOBAL" : "LOCATION")
                .currentQuantity(currentQuantity)
                .quantityOnHand(quantityOnHand)
                .quantityReserved(quantityReserved)
                .minQuantity(rule.getMinQuantity())
                .maxQuantity(rule.getMaxQuantity())
                .reorderQuantity(quantity)
                .targetQuantity(targetQuantity)
                .shortageQuantity(shortageQuantity)
                .estimatedUnitCost(item.getDefaultCost())
                .estimatedOrderCost(estimatedOrderCost)
                .preferredSupplierCode(rule.getPreferredSupplierCode())
                .severity(severity)
                .reasonCode(reasonCode)
                .reason(reason)
                .priorityScore(priorityScore)
                .build();
    }

    private BigDecimal targetQuantity(InventoryReorderRule rule, BigDecimal currentQuantity) {
        if (rule.getMaxQuantity() != null) {
            return rule.getMaxQuantity();
        }
        return currentQuantity.add(rule.getReorderQuantity());
    }

    private ReorderSuggestionReason reasonFor(BigDecimal currentQuantity, BigDecimal minQuantity) {
        if (currentQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return ReorderSuggestionReason.OUT_OF_STOCK;
        }
        if (currentQuantity.compareTo(minQuantity) < 0) {
            return ReorderSuggestionReason.BELOW_MINIMUM;
        }
        if (currentQuantity.compareTo(minQuantity) == 0) {
            return ReorderSuggestionReason.AT_MINIMUM;
        }
        return ReorderSuggestionReason.BELOW_TARGET_MAX;
    }

    private ReorderSuggestionSeverity severityFor(ReorderSuggestionReason reasonCode, BigDecimal currentQuantity, BigDecimal minQuantity) {
        if (reasonCode == ReorderSuggestionReason.NO_STOCK_LEVEL || reasonCode == ReorderSuggestionReason.OUT_OF_STOCK) {
            return ReorderSuggestionSeverity.CRITICAL;
        }
        if (reasonCode == ReorderSuggestionReason.AT_MINIMUM) {
            return ReorderSuggestionSeverity.MEDIUM;
        }
        if (minQuantity.signum() <= 0) {
            return ReorderSuggestionSeverity.LOW;
        }
        BigDecimal ratio = currentQuantity.divide(minQuantity, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.25")) <= 0) {
            return ReorderSuggestionSeverity.CRITICAL;
        }
        if (ratio.compareTo(new BigDecimal("0.50")) <= 0) {
            return ReorderSuggestionSeverity.HIGH;
        }
        return ReorderSuggestionSeverity.MEDIUM;
    }

    private int priorityScore(ReorderSuggestionSeverity severity, BigDecimal shortageQuantity, BigDecimal minQuantity, ReorderSuggestionReason reasonCode) {
        int base = switch (severity) {
            case CRITICAL -> 100;
            case HIGH -> 75;
            case MEDIUM -> 50;
            case LOW -> 25;
        };
        if (reasonCode == ReorderSuggestionReason.NO_STOCK_LEVEL) {
            return base + 10;
        }
        if (minQuantity.signum() <= 0) {
            return base;
        }
        BigDecimal shortageRatio = shortageQuantity.divide(minQuantity, 2, RoundingMode.HALF_UP);
        return base + shortageRatio.multiply(BigDecimal.TEN).intValue();
    }

    private Long estimatedOrderCost(InventoryReorderRule rule, BigDecimal reorderQuantity) {
        Long unitCost = rule.getItem().getDefaultCost();
        if (unitCost == null || reorderQuantity == null) {
            return null;
        }
        return reorderQuantity.multiply(BigDecimal.valueOf(unitCost)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String reasonText(ReorderSuggestionReason reasonCode, BigDecimal currentQuantity, BigDecimal minQuantity, BigDecimal targetQuantity) {
        return switch (reasonCode) {
            case NO_STOCK_LEVEL -> "Aucun niveau de stock n'existe encore pour cette regle. Creation de stock initial recommandee.";
            case OUT_OF_STOCK -> "Stock disponible nul. Reapprovisionnement prioritaire recommande.";
            case BELOW_MINIMUM -> "Stock disponible sous le seuil minimum " + minQuantity + ". Objectif propose: " + targetQuantity + ".";
            case AT_MINIMUM -> "Stock disponible exactement au seuil minimum " + minQuantity + ". Reapprovisionnement preventif recommande.";
            case BELOW_TARGET_MAX -> "Stock sous le niveau cible. Reapprovisionnement recommande.";
        };
    }

    private BigDecimal defaultMinQuantity(InventoryReorderRuleRequest request) {
        return request.getMinQuantity() == null ? BigDecimal.ZERO : request.getMinQuantity();
    }

    private BigDecimal defaultReorderQuantity(BigDecimal minQuantity, BigDecimal maxQuantity, InventoryReorderRuleRequest request) {
        if (request.getReorderQuantity() != null) {
            return request.getReorderQuantity();
        }
        if (maxQuantity != null && maxQuantity.compareTo(minQuantity) > 0) {
            return maxQuantity.subtract(minQuantity);
        }
        if (minQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return minQuantity;
        }
        return BigDecimal.ONE;
    }

    private void validate(BigDecimal minQuantity, BigDecimal maxQuantity, BigDecimal reorderQuantity) {
        if (minQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum quantity must be positive");
        }
        if (maxQuantity != null && maxQuantity.compareTo(minQuantity) < 0) {
            throw new BadRequestException("Maximum quantity must be greater than minimum quantity");
        }
        if (reorderQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Reorder quantity must be positive");
        }
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toUpperCase(Locale.ROOT) : null;
    }
}
