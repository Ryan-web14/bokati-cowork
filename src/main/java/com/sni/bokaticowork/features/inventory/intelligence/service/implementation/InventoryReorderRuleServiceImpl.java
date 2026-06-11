package com.sni.bokaticowork.features.inventory.intelligence.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.intelligence.dto.request.InventoryReorderRuleRequest;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryReorderRuleResponse;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.ReorderSuggestionResponse;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ConsumptionTrend;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionReason;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionSeverity;
import com.sni.bokaticowork.features.inventory.intelligence.mapper.interfaces.InventoryIntelligenceMapper;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryReorderRule;
import com.sni.bokaticowork.features.inventory.intelligence.repository.InventoryReorderRuleRepository;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryReorderRuleService;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseOrderStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.SupplierItem;
import com.sni.bokaticowork.features.inventory.procurement.repository.PurchaseOrderRepository;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierItemRepository;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryReorderRuleServiceImpl implements InventoryReorderRuleService {

    private static final int CONSUMPTION_WINDOW_DAYS = 30;
    private static final List<PurchaseOrderStatus> ACTIVE_PO_STATUSES = List.of(
            PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.APPROVED,
            PurchaseOrderStatus.ORDERED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

    private final InventoryReorderRuleRepository repository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository movementRepository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryLocationService locationService;
    private final InventoryIntelligenceMapper mapper;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierItemRepository supplierItemRepository;

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
        BigDecimal shortageQuantity = rule.getMinQuantity().subtract(currentQuantity).max(BigDecimal.ZERO);

        Long locationId = location != null ? location.getId() : (rule.getLocation() != null ? rule.getLocation().getId() : null);
        Instant now = Instant.now();
        Instant windowEnd = now.minus(CONSUMPTION_WINDOW_DAYS, ChronoUnit.DAYS);
        Instant prevWindowEnd = now.minus(2L * CONSUMPTION_WINDOW_DAYS, ChronoUnit.DAYS);

        BigDecimal consumption30 = orZero(movementRepository.consumptionSince(rule.getItem().getId(), locationId, windowEnd));
        BigDecimal consumptionPrev30 = orZero(movementRepository.consumptionBetween(rule.getItem().getId(), locationId, prevWindowEnd, windowEnd));

        BigDecimal dailyRate = consumption30.signum() > 0
                ? consumption30.divide(BigDecimal.valueOf(CONSUMPTION_WINDOW_DAYS), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Integer daysRemaining = dailyRate.signum() > 0
                ? currentQuantity.divide(dailyRate, 0, RoundingMode.CEILING).intValue()
                : null;

        ConsumptionTrend trend = consumptionTrend(consumption30, consumptionPrev30);
        Integer trendPercent = consumptionTrendPercent(consumption30, consumptionPrev30);

        BigDecimal quantityOnOrder = orZero(purchaseOrderRepository.quantityOnOrderForItem(
                rule.getItem().getId(), ACTIVE_PO_STATUSES));
        BigDecimal netShortage = shortageQuantity.subtract(quantityOnOrder).max(BigDecimal.ZERO);
        BigDecimal reorderQty = targetQuantity.subtract(currentQuantity).subtract(quantityOnOrder).max(rule.getReorderQuantity());
        if (trend == ConsumptionTrend.RISING && trendPercent != null && trendPercent > 20) {
            reorderQty = reorderQty.multiply(BigDecimal.valueOf(1.2)).setScale(0, RoundingMode.CEILING);
        }

        ReorderSuggestionSeverity severity = severityFor(reasonCode, currentQuantity, rule.getMinQuantity());
        severity = adjustSeverityByVelocity(severity, daysRemaining);
        int priorityScore = priorityScore(severity, netShortage, rule.getMinQuantity(), reasonCode, trend);
        Long estimatedOrderCost = estimatedOrderCost(rule, reorderQty);
        String reason = reasonText(reasonCode, currentQuantity, rule.getMinQuantity(), targetQuantity);

        List<SupplierItem> supplierItems = supplierItemRepository.findActiveByItemOrderByPriceAndLeadTime(rule.getItem());
        SupplierItem bestSupplier = supplierItems.isEmpty() ? null : supplierItems.get(0);
        int effectiveLeadTimeDays = effectiveLeadTimeDays(rule, bestSupplier);
        Instant recommendedOrderByDate = recommendedOrderByDate(daysRemaining, effectiveLeadTimeDays, now);

        InventoryLocation responseLocation = location != null ? location : rule.getLocation();
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
                .reorderQuantity(reorderQty)
                .targetQuantity(targetQuantity)
                .shortageQuantity(netShortage)
                .estimatedUnitCost(item.getDefaultCost())
                .estimatedOrderCost(estimatedOrderCost)
                .preferredSupplierCode(rule.getPreferredSupplierCode())
                .severity(severity)
                .reasonCode(reasonCode)
                .reason(reason)
                .priorityScore(priorityScore)
                .consumptionRateLast30Days(dailyRate.signum() > 0 ? dailyRate : null)
                .consumptionRatePrev30Days(consumptionPrev30.signum() > 0
                        ? consumptionPrev30.divide(BigDecimal.valueOf(CONSUMPTION_WINDOW_DAYS), 4, RoundingMode.HALF_UP)
                        : null)
                .consumptionTrend(trend)
                .consumptionTrendPercent(trendPercent)
                .daysOfStockRemaining(daysRemaining)
                .quantityOnOrder(quantityOnOrder.signum() > 0 ? quantityOnOrder : null)
                .recommendedOrderByDate(recommendedOrderByDate)
                .bestSupplierCode(bestSupplier == null ? null : bestSupplier.getSupplier().getSupplierCode())
                .bestSupplierPrice(bestSupplier == null ? null : bestSupplier.getUnitPrice())
                .bestSupplierLeadTimeDays(bestSupplier == null ? null : bestSupplier.getLeadTimeDays())
                .build();
    }

    private ReorderSuggestionSeverity adjustSeverityByVelocity(ReorderSuggestionSeverity current, Integer daysRemaining) {
        if (daysRemaining == null) return current;
        if (daysRemaining <= 2) return ReorderSuggestionSeverity.CRITICAL;
        if (daysRemaining <= 7 && current.ordinal() < ReorderSuggestionSeverity.HIGH.ordinal()) {
            return ReorderSuggestionSeverity.HIGH;
        }
        return current;
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

    private int priorityScore(ReorderSuggestionSeverity severity, BigDecimal shortageQuantity, BigDecimal minQuantity,
                              ReorderSuggestionReason reasonCode, ConsumptionTrend trend) {
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
        int score = base + shortageRatio.multiply(BigDecimal.TEN).intValue();
        if (trend == ConsumptionTrend.RISING) score += 5;
        return score;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static ConsumptionTrend consumptionTrend(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.signum() == 0) return ConsumptionTrend.STABLE;
        BigDecimal change = current.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP);
        if (change.compareTo(new BigDecimal("0.10")) > 0) return ConsumptionTrend.RISING;
        if (change.compareTo(new BigDecimal("-0.10")) < 0) return ConsumptionTrend.FALLING;
        return ConsumptionTrend.STABLE;
    }

    private static Integer consumptionTrendPercent(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.signum() == 0) return null;
        return current.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static int effectiveLeadTimeDays(InventoryReorderRule rule, SupplierItem bestSupplier) {
        if (bestSupplier != null && bestSupplier.getLeadTimeDays() != null) {
            return bestSupplier.getLeadTimeDays();
        }
        return 7;
    }

    private static Instant recommendedOrderByDate(Integer daysRemaining, int leadTimeDays, Instant now) {
        if (daysRemaining == null) return now;
        int buffer = Math.max(daysRemaining - leadTimeDays - 1, 0);
        return now.plus(buffer, ChronoUnit.DAYS);
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
