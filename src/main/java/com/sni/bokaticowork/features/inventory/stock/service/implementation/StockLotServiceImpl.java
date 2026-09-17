package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockLotCreateRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockLotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockLotServiceImpl implements StockLotService {

    private final StockLotRepository repository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryLocationService locationService;
    private final com.sni.bokaticowork.features.inventory.stock.mapper.decorator.StockLotResponseFactory lotResponseFactory;

    @Override
    public Page<StockLotResponse> search(String itemCode, String locationCode, String lotNumber,
                                         LocalDate expiringBefore, Boolean active, Boolean remainingOnly,
                                         Pageable pageable) {
        String lotNumberPattern = StringUtils.hasText(lotNumber)
                ? "%" + lotNumber.trim().toUpperCase(Locale.ROOT) + "%"
                : null;
        return repository.search(normalizeOptionalCode(itemCode), normalizeOptionalCode(locationCode),
                lotNumberPattern, expiringBefore, active, remainingOnly, pageable).map(this::toResponse);
    }

    @Override
    public List<StockLotResponse> getLotsForItem(String itemCode, String locationCode, Boolean activeOnly) {
        if (!StringUtils.hasText(itemCode)) {
            throw new BadRequestException("itemCode is required");
        }
        return repository.findLotsForItem(
                normalizeCode(itemCode),
                normalizeOptionalCode(locationCode),
                activeOnly
        ).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public StockLotResponse createLot(String itemCode, StockLotCreateRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        InventoryLocation location = locationService.findByLocationCodeOrThrow(request.getLocationCode());

        String lotNumber = request.getLotNumber().trim().toUpperCase(Locale.ROOT);
        if (repository.findMatchingLot(item, location, lotNumber, request.getExpiryDate()).isPresent()) {
            throw new BadRequestException("A lot with this number and expiry date already exists for this item/location");
        }

        StockLot lot = StockLot.builder()
                .item(item)
                .location(location)
                .lotNumber(lotNumber)
                .expiryDate(request.getExpiryDate())
                .receivedAt(Instant.now())
                .initialQuantity(request.getInitialQuantity())
                .remainingQuantity(request.getInitialQuantity())
                .active(Boolean.TRUE)
                .quarantined(Boolean.FALSE)
                .ownershipType(request.getOwnershipType())
                .ownerCode(normalizeOptionalCode(request.getOwnerCode()))
                .build();
        return toResponse(repository.save(lot));
    }

    private StockLotResponse toResponse(StockLot lot) {
        return lotResponseFactory.toResponse(lot);
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) throw new BadRequestException("Code is required");
        return normalizeOptionalCode(value);
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toUpperCase(Locale.ROOT) : null;
    }
}
