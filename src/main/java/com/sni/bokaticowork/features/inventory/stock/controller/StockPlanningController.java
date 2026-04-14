package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryUnitConversionRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryUnitConversionResponse;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnitConversion;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryUnitConversionRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockPickingSuggestionResponse;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/stock/planning")
public class StockPlanningController {

    private final InventoryItemLookupService itemLookupService;
    private final StockLevelRepository levelRepository;
    private final InventoryUnitConversionRepository conversionRepository;

    @GetMapping("/picking")
    public ResponseEntity<StockPickingSuggestionResponse> picking(@RequestParam String itemCode,
                                                                  @RequestParam BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        BigDecimal remaining = quantity;
        BigDecimal suggested = BigDecimal.ZERO;
        List<StockPickingSuggestionResponse.Line> lines = new ArrayList<>();
        for (StockLevel level : levelRepository.findAllByItemAndQuantityAvailableGreaterThanOrderByQuantityAvailableDesc(item, BigDecimal.ZERO)) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal pick = level.getQuantityAvailable().min(remaining);
            suggested = suggested.add(pick);
            remaining = remaining.subtract(pick);
            lines.add(StockPickingSuggestionResponse.Line.builder()
                    .locationCode(level.getLocation().getLocationCode())
                    .locationName(level.getLocation().getName())
                    .availableQuantity(level.getQuantityAvailable())
                    .pickQuantity(pick)
                    .build());
        }
        return ResponseEntity.ok(StockPickingSuggestionResponse.builder()
                .itemCode(item.getItemCode())
                .requestedQuantity(quantity)
                .suggestedQuantity(suggested)
                .fullyCovered(remaining.compareTo(BigDecimal.ZERO) <= 0)
                .lines(lines)
                .build());
    }

    @PostMapping("/unit-conversions")
    public ResponseEntity<InventoryUnitConversionResponse> createConversion(@Valid @RequestBody InventoryUnitConversionRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        String from = normalize(request.getFromUnitCode());
        String to = normalize(request.getToUnitCode());
        if (from.equals(to)) {
            throw new BadRequestException("Conversion units must be different");
        }
        InventoryUnitConversion conversion = conversionRepository
                .findByItemAndFromUnitCodeAndToUnitCodeAndActiveTrue(item, from, to)
                .orElseGet(InventoryUnitConversion::new);
        conversion.setItem(item);
        conversion.setFromUnitCode(from);
        conversion.setToUnitCode(to);
        conversion.setFactor(request.getFactor());
        conversion.setActive(true);
        return ResponseEntity.ok(toResponse(conversionRepository.save(conversion)));
    }

    @GetMapping("/unit-conversions/{itemCode}")
    public ResponseEntity<List<InventoryUnitConversionResponse>> conversions(@PathVariable String itemCode) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        return ResponseEntity.ok(conversionRepository.findAllByItemAndActiveTrue(item).stream().map(this::toResponse).toList());
    }

    @GetMapping("/unit-conversions/{itemCode}/convert")
    public ResponseEntity<BigDecimal> convert(@PathVariable String itemCode,
                                              @RequestParam String fromUnitCode,
                                              @RequestParam String toUnitCode,
                                              @RequestParam BigDecimal quantity) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        InventoryUnitConversion conversion = conversionRepository
                .findByItemAndFromUnitCodeAndToUnitCodeAndActiveTrue(item, normalize(fromUnitCode), normalize(toUnitCode))
                .orElseThrow(() -> new BadRequestException("Unit conversion not configured"));
        return ResponseEntity.ok(quantity.multiply(conversion.getFactor()));
    }

    private InventoryUnitConversionResponse toResponse(InventoryUnitConversion conversion) {
        return InventoryUnitConversionResponse.builder()
                .itemCode(conversion.getItem().getItemCode())
                .fromUnitCode(conversion.getFromUnitCode())
                .toUnitCode(conversion.getToUnitCode())
                .factor(conversion.getFactor())
                .build();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) throw new BadRequestException("Unit code is required");
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }
}
