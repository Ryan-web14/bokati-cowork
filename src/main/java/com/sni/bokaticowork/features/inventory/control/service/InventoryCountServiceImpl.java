package com.sni.bokaticowork.features.inventory.control.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.control.dto.request.InventoryCountCreateRequest;
import com.sni.bokaticowork.features.inventory.control.dto.request.InventoryCountLineRequest;
import com.sni.bokaticowork.features.inventory.control.dto.response.InventoryCountResponse;
import com.sni.bokaticowork.features.inventory.control.enums.InventoryCountStatus;
import com.sni.bokaticowork.features.inventory.control.mapper.InventoryCountMapper;
import com.sni.bokaticowork.features.inventory.control.model.InventoryCount;
import com.sni.bokaticowork.features.inventory.control.model.InventoryCountItem;
import com.sni.bokaticowork.features.inventory.control.repository.InventoryCountItemRepository;
import com.sni.bokaticowork.features.inventory.control.repository.InventoryCountRepository;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockAdjustmentRequest;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryCountServiceImpl implements InventoryCountService {

    private final InventoryCountRepository countRepository;
    private final InventoryCountItemRepository itemRepository;
    private final StockLevelRepository stockLevelRepository;
    private final InventoryLocationService locationService;
    private final InventoryItemLookupService itemLookupService;
    private final StockService stockService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final InventoryCountMapper mapper;
    private final OutboxService outboxService;

    @Override
    public InventoryCountResponse create(InventoryCountCreateRequest request) {
        InventoryLocation location = locationService.findByLocationCodeOrThrow(request.getLocationCode());
        InventoryCount count = InventoryCount.builder()
                .countCode(generateCountCode())
                .location(location)
                .status(InventoryCountStatus.DRAFT)
                .createdBy(trimToNull(request.getCreatedBy()))
                .notes(trimToNull(request.getNotes()))
                .build();
        return mapper.toResponse(countRepository.save(count));
    }

    @Override
    public InventoryCountResponse start(String countCode) {
        InventoryCount count = findForUpdate(countCode);
        requireStatus(count, InventoryCountStatus.DRAFT);
        count.setStatus(InventoryCountStatus.IN_PROGRESS);
        count.setStartedAt(Instant.now());
        for (StockLevel level : stockLevelRepository.findAllByLocation(count.getLocation())) {
            InventoryCountItem item = InventoryCountItem.builder()
                    .inventoryCount(count)
                    .item(level.getItem())
                    .expectedQuantity(level.getQuantityOnHand())
                    .build();
            count.getItems().add(item);
        }
        return mapper.toResponse(countRepository.save(count));
    }

    @Override
    public InventoryCountResponse recordLine(String countCode, InventoryCountLineRequest request) {
        InventoryCount count = findForUpdate(countCode);
        requireStatus(count, InventoryCountStatus.IN_PROGRESS);
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        InventoryCountItem line = itemRepository.findByInventoryCountAndItem(count, item)
                .orElseGet(() -> InventoryCountItem.builder()
                        .inventoryCount(count)
                        .item(item)
                        .expectedQuantity(stockLevelRepository.findByItemAndLocationReadOnly(item, count.getLocation())
                                .map(StockLevel::getQuantityOnHand)
                                .orElse(BigDecimal.ZERO))
                        .build());
        line.setCountedQuantity(request.getCountedQuantity());
        line.setNotes(trimToNull(request.getNotes()));
        line.recalculateVariance();
        if (line.getId() == null) {
            count.getItems().add(line);
        }
        itemRepository.save(line);
        return mapper.toResponse(countRepository.save(count));
    }

    @Override
    public InventoryCountResponse sendToReview(String countCode) {
        InventoryCount count = findForUpdate(countCode);
        requireStatus(count, InventoryCountStatus.IN_PROGRESS);
        boolean hasUncounted = count.getItems().stream().anyMatch(item -> item.getCountedQuantity() == null);
        if (hasUncounted) {
            throw new BadRequestException("All inventory count lines must be counted before review");
        }
        count.setStatus(InventoryCountStatus.REVIEW);
        count.setReviewedAt(Instant.now());
        return mapper.toResponse(countRepository.save(count));
    }

    @Override
    public InventoryCountResponse validate(String countCode, String performedBy) {
        InventoryCount count = findForUpdate(countCode);
        requireStatus(count, InventoryCountStatus.REVIEW);
        for (InventoryCountItem item : count.getItems()) {
            item.recalculateVariance();
            if (item.getVarianceQuantity() == null || item.getVarianceQuantity().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
            adjustment.setItemCode(item.getItem().getItemCode());
            adjustment.setLocationCode(count.getLocation().getLocationCode());
            adjustment.setQuantityDelta(item.getVarianceQuantity());
            adjustment.setReferenceType(StockReferenceType.INVENTORY_COUNT);
            adjustment.setReferenceCode(count.getCountCode());
            adjustment.setReasonCode(StockOutReasonCode.CONSUMPTION);
            adjustment.setReasonDetails("Inventory count variance validation");
            adjustment.setAllowNegativeOverride(true);
            adjustment.setPerformedBy(performedBy);
            stockService.adjust(adjustment);
        }
        count.setStatus(InventoryCountStatus.VALIDATED);
        count.setValidatedAt(Instant.now());
        InventoryCount saved = countRepository.save(count);
        outboxService.publish("inventory.count.validated", "INVENTORY_COUNT", saved.getCountCode(),
                java.util.Map.of("countCode", saved.getCountCode(), "locationCode", saved.getLocation().getLocationCode()));
        return mapper.toResponse(saved);
    }

    @Override
    public InventoryCountResponse cancel(String countCode) {
        InventoryCount count = findForUpdate(countCode);
        if (count.getStatus() == InventoryCountStatus.VALIDATED) {
            throw new BadRequestException("Validated inventory count cannot be cancelled");
        }
        count.setStatus(InventoryCountStatus.CANCELLED);
        return mapper.toResponse(countRepository.save(count));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryCountResponse get(String countCode) {
        return mapper.toResponse(countRepository.findByCountCode(normalizeCode(countCode))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory count not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryCountResponse> list(InventoryCountStatus status, Pageable pageable) {
        Page<InventoryCount> counts = status == null ? countRepository.findAll(pageable) : countRepository.findAllByStatus(status, pageable);
        return counts.map(mapper::toResponse);
    }

    private InventoryCount findForUpdate(String countCode) {
        return countRepository.findByCountCodeForUpdate(normalizeCode(countCode))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory count not found"));
    }

    private void requireStatus(InventoryCount count, InventoryCountStatus status) {
        if (count.getStatus() != status) {
            throw new BadRequestException("Inventory count must be " + status);
        }
    }

    private String generateCountCode() {
        String code;
        do {
            code = sequenceGenerator.next("inventory_count", LocalDate.now()) + "-" + System.currentTimeMillis();
        } while (countRepository.existsByCountCode(code));
        return code;
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) throw new BadRequestException("Code is required");
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
