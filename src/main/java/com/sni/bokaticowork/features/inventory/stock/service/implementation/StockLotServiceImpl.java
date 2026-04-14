package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockLotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockLotServiceImpl implements StockLotService {

    private final StockLotRepository repository;

    @Override
    public Page<StockLotResponse> search(String itemCode, String locationCode, String lotNumber,
                                         LocalDate expiringBefore, Boolean active, Boolean remainingOnly,
                                         Pageable pageable) {
        return repository.search(normalizeOptionalCode(itemCode), normalizeOptionalCode(locationCode),
                StringUtils.hasText(lotNumber) ? lotNumber.trim().toUpperCase(Locale.ROOT) : null,
                expiringBefore, active, remainingOnly, pageable).map(this::toResponse);
    }

    private StockLotResponse toResponse(StockLot lot) {
        return StockLotResponse.builder()
                .itemCode(lot.getItem().getItemCode())
                .itemName(lot.getItem().getName())
                .locationCode(lot.getLocation().getLocationCode())
                .locationName(lot.getLocation().getName())
                .lotNumber(lot.getLotNumber())
                .expiryDate(lot.getExpiryDate())
                .receivedAt(lot.getReceivedAt())
                .initialQuantity(lot.getInitialQuantity())
                .remainingQuantity(lot.getRemainingQuantity())
                .quarantined(lot.getQuarantined())
                .quarantineReason(lot.getQuarantineReason())
                .ownershipType(lot.getOwnershipType())
                .ownerCode(lot.getOwnerCode())
                .active(lot.getActive())
                .build();
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toUpperCase(Locale.ROOT) : null;
    }
}
