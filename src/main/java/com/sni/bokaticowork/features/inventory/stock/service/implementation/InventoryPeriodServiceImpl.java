package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.control.enums.InventoryCountStatus;
import com.sni.bokaticowork.features.inventory.control.repository.InventoryCountRepository;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryPeriodReopenRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryPeriodRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryPeriodResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockValuationSnapshotResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.InventoryPeriodStatus;
import com.sni.bokaticowork.features.inventory.stock.enums.ValuationMethod;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryPeriod;
import com.sni.bokaticowork.features.inventory.stock.model.StockCostLayer;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.model.StockValuationSnapshot;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryPeriodRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockCostLayerRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockValuationSnapshotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryPeriodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryPeriodServiceImpl implements InventoryPeriodService {

    private static final DateTimeFormatter PERIOD_CODE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final InventoryPeriodRepository periodRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockCostLayerRepository costLayerRepository;
    private final StockValuationSnapshotRepository snapshotRepository;
    private final InventoryCountRepository countRepository;

    @Override
    public InventoryPeriodResponse create(InventoryPeriodRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Period end date must not precede its start date");
        }

        String code = StringUtils.hasText(request.getPeriodCode())
                ? request.getPeriodCode().trim().toUpperCase(Locale.ROOT)
                : PERIOD_CODE_FORMAT.format(request.getStartDate());

        if (periodRepository.existsByPeriodCode(code)) {
            throw new ResourceAlreadyExistException("Period already exists: " + code);
        }

        List<InventoryPeriod> overlapping =
                periodRepository.findOverlapping(request.getStartDate(), request.getEndDate(), null);
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("Period overlaps with " + overlapping.get(0).getPeriodCode()
                    + ". Two periods cannot cover the same day.");
        }

        InventoryPeriod period = InventoryPeriod.builder()
                .periodCode(code)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(InventoryPeriodStatus.OPEN)
                .build();

        return toResponse(periodRepository.save(period));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryPeriodResponse get(String periodCode) {
        return toResponse(findOrThrow(periodCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryPeriodResponse> list() {
        return periodRepository.findAllByOrderByStartDateDesc().stream().map(this::toResponse).toList();
    }

    @Override
    public InventoryPeriodResponse close(String periodCode, String closedBy) {
        InventoryPeriod period = findOrThrow(periodCode);
        if (period.getStatus() == InventoryPeriodStatus.CLOSED) {
            throw new BadRequestException("Period " + periodCode + " is already closed");
        }

        // Un inventaire physique non valide fausserait la valeur figee a la cloture.
        long pendingCounts = countRepository.countByStatusInAndCreatedAtBetween(
                List.of(InventoryCountStatus.DRAFT, InventoryCountStatus.IN_PROGRESS, InventoryCountStatus.REVIEW),
                period.getStartDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                period.getEndDate().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        if (pendingCounts > 0) {
            throw new BadRequestException("Period " + periodCode + " has " + pendingCounts
                    + " inventory count(s) still open. Validate or cancel them before closing.");
        }

        List<StockValuationSnapshotResponse> snapshot = takeSnapshot(period.getEndDate(), period.getPeriodCode());
        long closingValue = snapshot.stream().mapToLong(StockValuationSnapshotResponse::getTotalValue).sum();

        period.setStatus(InventoryPeriodStatus.CLOSED);
        period.setClosedBy(closedBy);
        period.setClosedAt(Instant.now());
        period.setClosingStockValue(closingValue);

        log.info("Inventory period {} closed by {}, stock value {}", periodCode, closedBy, closingValue);
        return toResponse(periodRepository.save(period));
    }

    @Override
    public InventoryPeriodResponse reopen(String periodCode, InventoryPeriodReopenRequest request) {
        InventoryPeriod period = findOrThrow(periodCode);
        if (period.getStatus() == InventoryPeriodStatus.OPEN) {
            throw new BadRequestException("Period " + periodCode + " is already open");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new BadRequestException("Reopening a closed period requires a reason");
        }

        period.setStatus(InventoryPeriodStatus.OPEN);
        period.setReopenedBy(request.getReopenedBy());
        period.setReopenedAt(Instant.now());
        period.setReopenReason(request.getReason().trim());

        log.warn("Inventory period {} reopened by {} : {}", periodCode, request.getReopenedBy(), request.getReason());
        return toResponse(periodRepository.save(period));
    }

    @Override
    @Transactional(readOnly = true)
    public void assertMovementAllowed(LocalDate movementDate) {
        periodRepository.findCovering(movementDate).ifPresent(period -> {
            if (!period.getStatus().acceptsMovements()) {
                throw new BadRequestException("Period " + period.getPeriodCode() + " is "
                        + period.getStatus() + " : no movement can be dated in it.");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public String periodCodeFor(LocalDate date) {
        return periodRepository.findCovering(date).map(InventoryPeriod::getPeriodCode).orElse(null);
    }

    @Override
    public List<StockValuationSnapshotResponse> takeSnapshot(LocalDate snapshotDate, String periodCode) {
        snapshotRepository.deleteBySnapshotDate(snapshotDate);

        List<StockValuationSnapshotResponse> result = new ArrayList<>();
        for (StockLevel level : stockLevelRepository.findAll()) {
            if (level.getQuantityOnHand() == null || level.getQuantityOnHand().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            ValuationMethod method = level.getItem().effectiveValuationMethod();
            Long unitCost = level.getAverageCost();
            long totalValue = unitCost == null
                    ? 0L
                    : level.getQuantityOnHand().multiply(BigDecimal.valueOf(unitCost)).longValue();

            StockValuationSnapshot snapshot = StockValuationSnapshot.builder()
                    .snapshotDate(snapshotDate)
                    .periodCode(periodCode)
                    .itemCode(level.getItem().getItemCode())
                    .locationCode(level.getLocation().getLocationCode())
                    .quantityOnHand(level.getQuantityOnHand())
                    .unitCost(unitCost)
                    .totalValue(totalValue)
                    .valuationMethod(method)
                    .oldestLayerAgeDays(oldestLayerAge(level, method, snapshotDate))
                    .build();

            result.add(toResponse(snapshotRepository.save(snapshot)));
        }

        log.info("Valuation snapshot at {} : {} lines", snapshotDate, result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockValuationSnapshotResponse> snapshotAt(LocalDate snapshotDate) {
        return snapshotRepository.findAllBySnapshotDateOrderByItemCodeAsc(snapshotDate)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Anciennete de la plus vieille couche restante. Null hors FIFO : sans couches, l'information de
     * cout par age n'existe pas, et l'inventer serait trompeur.
     */
    private Integer oldestLayerAge(StockLevel level, ValuationMethod method, LocalDate snapshotDate) {
        if (!method.isLayered()) {
            return null;
        }
        List<StockCostLayer> layers =
                costLayerRepository.findConsumableReadOnly(level.getItem(), level.getLocation());
        if (layers.isEmpty()) {
            return null;
        }
        Instant oldest = layers.get(0).getReceivedAt();
        Instant reference = snapshotDate.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        long days = Duration.between(oldest, reference).toDays();
        return (int) Math.max(0, days);
    }

    private InventoryPeriod findOrThrow(String periodCode) {
        String code = periodCode == null ? null : periodCode.trim().toUpperCase(Locale.ROOT);
        return periodRepository.findByPeriodCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory period not found: " + periodCode));
    }

    private InventoryPeriodResponse toResponse(InventoryPeriod period) {
        return InventoryPeriodResponse.builder()
                .periodCode(period.getPeriodCode())
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .status(period.getStatus())
                .closedBy(period.getClosedBy())
                .closedAt(period.getClosedAt())
                .reopenedBy(period.getReopenedBy())
                .reopenedAt(period.getReopenedAt())
                .reopenReason(period.getReopenReason())
                .closingStockValue(period.getClosingStockValue())
                .build();
    }

    private StockValuationSnapshotResponse toResponse(StockValuationSnapshot snapshot) {
        return StockValuationSnapshotResponse.builder()
                .snapshotDate(snapshot.getSnapshotDate())
                .periodCode(snapshot.getPeriodCode())
                .itemCode(snapshot.getItemCode())
                .locationCode(snapshot.getLocationCode())
                .quantityOnHand(snapshot.getQuantityOnHand())
                .unitCost(snapshot.getUnitCost())
                .totalValue(snapshot.getTotalValue())
                .valuationMethod(snapshot.getValuationMethod())
                .oldestLayerAgeDays(snapshot.getOldestLayerAgeDays())
                .build();
    }
}
