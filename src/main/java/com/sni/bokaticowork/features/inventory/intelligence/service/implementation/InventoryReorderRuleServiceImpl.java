package com.sni.bokaticowork.features.inventory.intelligence.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.intelligence.dto.request.InventoryReorderRuleRequest;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryReorderRuleResponse;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.ReorderSuggestionResponse;
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
        validate(request);
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        InventoryLocation location = StringUtils.hasText(request.getLocationCode())
                ? locationService.findByLocationCodeOrThrow(request.getLocationCode())
                : null;
        InventoryReorderRule rule = repository.findByItemAndLocation(item, location).orElseGet(InventoryReorderRule::new);
        rule.setItem(item);
        rule.setLocation(location);
        rule.setMinQuantity(request.getMinQuantity());
        rule.setMaxQuantity(request.getMaxQuantity());
        rule.setReorderQuantity(request.getReorderQuantity());
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
    public List<ReorderSuggestionResponse> suggestions() {
        return repository.findAllByActiveTrueOrderByIdAsc().stream()
                .flatMap(rule -> stockLevelRepository.findByItemAndLocation(rule.getItem(), rule.getLocation()).stream()
                        .filter(level -> level.getQuantityAvailable().compareTo(rule.getMinQuantity()) <= 0)
                        .map(level -> toSuggestion(rule, level)))
                .toList();
    }

    private ReorderSuggestionResponse toSuggestion(InventoryReorderRule rule, StockLevel level) {
        BigDecimal quantity = rule.getReorderQuantity();
        if (rule.getMaxQuantity() != null) {
            BigDecimal toMax = rule.getMaxQuantity().subtract(level.getQuantityAvailable());
            if (toMax.compareTo(BigDecimal.ZERO) > 0) {
                quantity = toMax.max(rule.getReorderQuantity());
            }
        }
        return ReorderSuggestionResponse.builder()
                .itemCode(rule.getItem().getItemCode())
                .itemName(rule.getItem().getName())
                .locationCode(rule.getLocation() == null ? null : rule.getLocation().getLocationCode())
                .locationName(rule.getLocation() == null ? null : rule.getLocation().getName())
                .currentQuantity(level.getQuantityAvailable())
                .minQuantity(rule.getMinQuantity())
                .maxQuantity(rule.getMaxQuantity())
                .reorderQuantity(quantity)
                .preferredSupplierCode(rule.getPreferredSupplierCode())
                .build();
    }

    private void validate(InventoryReorderRuleRequest request) {
        if (request.getMinQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum quantity must be positive");
        }
        if (request.getMaxQuantity() != null && request.getMaxQuantity().compareTo(request.getMinQuantity()) < 0) {
            throw new BadRequestException("Maximum quantity must be greater than minimum quantity");
        }
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toUpperCase(Locale.ROOT) : null;
    }
}
