package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetRepository;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockAdjustmentRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockOutRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockTransferRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLevelResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovementLot;
import com.sni.bokaticowork.features.inventory.stock.model.StockReservation;
import com.sni.bokaticowork.features.inventory.stock.mapper.interfaces.StockMapper;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryAutomationService;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventorySerialResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.InventorySerialStatus;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryItemSerial;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryItemSerialRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementLotRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockReservationRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class  StockServiceImpl implements StockService {

    private final InventoryItemLookupService itemLookupService;
    private final InventoryLocationService locationService;
    private final StockLevelRepository stockLevelRepository;
    private final StockLotRepository stockLotRepository;
    private final StockMovementRepository movementRepository;
    private final StockMovementLotRepository movementLotRepository;
    private final StockReservationRepository reservationRepository;
    private final AssetRepository assetRepository;
    private final InventoryItemSerialRepository serialRepository;
    private final StockMapper mapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final InventoryAutomationService automationService;
    private final StockValuationService valuationService;

    @Override
    public StockMovementResponse receive(StockInRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        ensureLifecycleAllowsReceipt(item);
        InventoryLocation location = locationService.findByLocationCodeOrThrow(request.getLocationCode());
        StockLevel level = getOrCreateLevelForUpdate(item, location);

        BigDecimal previousQuantity = level.getQuantityOnHand();
        level.setQuantityOnHand(previousQuantity.add(request.getQuantity()));
        level.setLastMovementAt(Instant.now());
        level.recalculateAvailable();
        String movementCode = generateMovementCode(StockMovementType.IN);
        Long effectiveUnitCost = valuationService.recordEntry(item, location, level, previousQuantity,
                request.getQuantity(), request.getUnitCost(), movementCode, request.getLotNumber());
        stockLevelRepository.save(level);
        receiveLotIfNeeded(item, location, request);

        StockMovement movement = saveMovement(movementCode, item, null, location, StockMovementType.IN, request.getQuantity(),
                effectiveUnitCost, request.getReferenceType(), request.getReferenceCode(), null, request.getReason(),
                false, request.getPerformedBy());
        createAssetsFromReceiptIfNeeded(item, location, request);
        receiveSerialNumbersIfNeeded(item, location, request);
        automationService.afterStockMovement(movement, level);
        return mapper.toMovementResponse(movement);
    }

    @Override
    public StockMovementResponse issue(StockOutRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        ensureLifecycleAllowsIssue(item);
        InventoryLocation location = locationService.findByLocationCodeOrThrow(request.getLocationCode());
        StockLevel level = getOrCreateLevelForUpdate(item, location);

        boolean override = Boolean.TRUE.equals(request.getAllowNegativeOverride());
        ensureCanDecrease(item, level, request.getQuantity(), override);
        validateLotForConsumption(item, location, request.getLotNumber(), Boolean.TRUE.equals(request.getConfirmCreateLot()));
        List<LotConsumption> consumedLots = consumeLotsIfTracked(item, location, request.getQuantity(),
                request.getLotNumber(), override);
        level.setQuantityOnHand(level.getQuantityOnHand().subtract(request.getQuantity()));
        level.setLastMovementAt(Instant.now());
        level.recalculateAvailable();
        stockLevelRepository.save(level);

        issueSerialNumbersIfNeeded(item, location, request.getSerialNumbers());
        // Les sorties n'etaient pas valorisees avant le lot 2 : leur cout unitaire etait toujours nul,
        // ce qui vidait de sens la colonne valeur des rapports de mouvements.
        Long exitUnitCost = valuationService.recordExit(item, location, level, request.getQuantity(), override);
        stockLevelRepository.save(level);
        StockMovement movement = saveMovement(item, location, null, StockMovementType.OUT, request.getQuantity(),
                exitUnitCost, request.getReferenceType(), request.getReferenceCode(), request.getReasonCode(),
                request.getReasonDetails(), override, request.getPerformedBy());
        saveMovementLots(movement, consumedLots);
        automationService.afterStockMovement(movement, level);
        return mapper.toMovementResponse(movement);
    }

    @Override
    public StockMovementResponse transfer(StockTransferRequest request) {
        if (request.getFromLocationCode().equalsIgnoreCase(request.getToLocationCode())) {
            throw new BadRequestException("Transfer source and destination must be different");
        }
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        ensureLifecycleAllowsIssue(item);
        InventoryLocation from = locationService.findByLocationCodeOrThrow(request.getFromLocationCode());
        InventoryLocation to = locationService.findByLocationCodeOrThrow(request.getToLocationCode());
        StockLevel fromLevel = getOrCreateLevelForUpdate(item, from);
        StockLevel toLevel = getOrCreateLevelForUpdate(item, to);

        boolean override = Boolean.TRUE.equals(request.getAllowNegativeOverride());
        ensureCanDecrease(item, fromLevel, request.getQuantity(), override);
        List<LotConsumption> consumedLots = consumeLotsIfTracked(item, from, request.getQuantity(), null, override);
        moveConsumedLotsToDestination(item, to, consumedLots);
        fromLevel.setQuantityOnHand(fromLevel.getQuantityOnHand().subtract(request.getQuantity()));
        fromLevel.setLastMovementAt(Instant.now());
        fromLevel.recalculateAvailable();
        toLevel.setQuantityOnHand(toLevel.getQuantityOnHand().add(request.getQuantity()));
        toLevel.setLastMovementAt(Instant.now());
        toLevel.recalculateAvailable();

        String movementCode = generateMovementCode(StockMovementType.TRANSFER);
        Long transferUnitCost = valuationService.recordTransfer(item, from, to, fromLevel, toLevel,
                request.getQuantity(), movementCode, override);
        stockLevelRepository.save(fromLevel);
        stockLevelRepository.save(toLevel);

        transferSerialNumbersIfNeeded(item, from, to, request.getSerialNumbers());
        StockMovement movement = saveMovement(movementCode, item, from, to, StockMovementType.TRANSFER, request.getQuantity(),
                transferUnitCost, request.getReferenceType(), request.getReferenceCode(),
                request.getReasonCode(), request.getReasonDetails(), override, request.getPerformedBy());
        saveMovementLots(movement, consumedLots);
        automationService.afterStockMovement(movement, fromLevel, toLevel);
        return mapper.toMovementResponse(movement);
    }

    @Override
    public StockMovementResponse adjust(StockAdjustmentRequest request) {
        if (request.getQuantityDelta().compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Adjustment quantity delta cannot be zero");
        }
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        InventoryLocation location = locationService.findByLocationCodeOrThrow(request.getLocationCode());
        StockLevel level = getOrCreateLevelForUpdate(item, location);
        boolean positive = request.getQuantityDelta().compareTo(BigDecimal.ZERO) > 0;
        BigDecimal absQuantity = request.getQuantityDelta().abs();
        boolean override = Boolean.TRUE.equals(request.getAllowNegativeOverride());

        if (!positive) {
            ensureCanDecrease(item, level, absQuantity, override);
            List<LotConsumption> consumedLots = consumeLotsIfTracked(item, location, absQuantity, null, override);
            level.setQuantityOnHand(level.getQuantityOnHand().add(request.getQuantityDelta()));
            level.setLastMovementAt(Instant.now());
            level.recalculateAvailable();
            // Les couches sont consommees dans tous les cas, sinon elles divergeraient de la
            // quantite en stock. Un cout impose ne sert qu'a valoriser le mouvement.
            Long computedOutCost = valuationService.recordExit(item, location, level, absQuantity, true);
            Long adjustmentOutCost = request.getUnitCost() != null ? request.getUnitCost() : computedOutCost;
            stockLevelRepository.save(level);

            StockMovement movement = saveMovement(item, location, null, StockMovementType.ADJUSTMENT_OUT,
                    absQuantity, adjustmentOutCost, defaultReferenceType(request.getReferenceType()),
                    request.getReferenceCode(), request.getReasonCode(), request.getReasonDetails(), override, request.getPerformedBy());
            saveMovementLots(movement, consumedLots);
            automationService.afterStockMovement(movement, level);
            return mapper.toMovementResponse(movement);
        } else {
            receiveAdjustmentLotIfProvided(item, location, request, absQuantity);
        }
        BigDecimal quantityBeforeAdjustment = level.getQuantityOnHand();
        level.setQuantityOnHand(quantityBeforeAdjustment.add(request.getQuantityDelta()));
        level.setLastMovementAt(Instant.now());
        level.recalculateAvailable();

        String adjustmentCode = generateMovementCode(StockMovementType.ADJUSTMENT_IN);
        Long adjustmentInCost = valuationService.recordEntry(item, location, level, quantityBeforeAdjustment,
                absQuantity, request.getUnitCost(), adjustmentCode, request.getLotNumber());
        stockLevelRepository.save(level);

        StockMovement movement = saveMovement(adjustmentCode, item, null, location, StockMovementType.ADJUSTMENT_IN,
                absQuantity, adjustmentInCost, defaultReferenceType(request.getReferenceType()),
                request.getReferenceCode(), null, request.getReasonDetails(), override, request.getPerformedBy());
        automationService.afterStockMovement(movement, level);
        return mapper.toMovementResponse(movement);
    }

    @Override
    public StockReservationResponse reserve(StockReservationRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        InventoryLocation location = locationService.findByLocationCodeOrThrow(request.getLocationCode());
        StockLevel level = getOrCreateLevelForUpdate(item, location);
        ensureCanDecrease(item, level, request.getQuantity(), false);

        level.setQuantityReserved(level.getQuantityReserved().add(request.getQuantity()));
        level.setLastMovementAt(Instant.now());
        level.recalculateAvailable();
        stockLevelRepository.save(level);

        StockReservation reservation = StockReservation.builder()
                .reservationCode(generateReservationCode())
                .item(item)
                .location(location)
                .quantity(request.getQuantity())
                .status(StockReservationStatus.ACTIVE)
                .referenceType(request.getReferenceType())
                .referenceCode(normalizeOptionalCode(request.getReferenceCode()))
                .reservedBy(trimToNull(request.getReservedBy()))
                .expiresAt(request.getExpiresAt())
                .build();
        return mapper.toReservationResponse(reservationRepository.save(reservation));
    }

    @Override
    public StockReservationResponse releaseReservation(String reservationCode) {
        StockReservation reservation = activeReservationForUpdate(reservationCode);
        StockLevel level = getOrCreateLevelForUpdate(reservation.getItem(), reservation.getLocation());
        level.setQuantityReserved(level.getQuantityReserved().subtract(reservation.getQuantity()).max(BigDecimal.ZERO));
        level.setLastMovementAt(Instant.now());
        level.recalculateAvailable();
        stockLevelRepository.save(level);

        reservation.setStatus(StockReservationStatus.RELEASED);
        reservation.setClosedAt(Instant.now());
        return mapper.toReservationResponse(reservationRepository.save(reservation));
    }

    @Override
    public StockMovementResponse consumeReservation(String reservationCode, String performedBy) {
        StockReservation reservation = activeReservationForUpdate(reservationCode);
        StockLevel level = getOrCreateLevelForUpdate(reservation.getItem(), reservation.getLocation());
        BigDecimal quantity = reservation.getQuantity();
        boolean override = Boolean.TRUE.equals(reservation.getItem().getAllowNegativeStock());
        ensureCanConsumeReserved(reservation.getItem(), level, quantity, override);
        List<LotConsumption> consumedLots = consumeLotsIfTracked(reservation.getItem(), reservation.getLocation(), quantity, null, override);

        level.setQuantityReserved(level.getQuantityReserved().subtract(quantity).max(BigDecimal.ZERO));
        level.setQuantityOnHand(level.getQuantityOnHand().subtract(quantity));
        level.setLastMovementAt(Instant.now());
        level.recalculateAvailable();
        stockLevelRepository.save(level);

        reservation.setStatus(StockReservationStatus.CONSUMED);
        reservation.setClosedAt(Instant.now());
        reservationRepository.save(reservation);

        StockMovement movement = saveMovement(reservation.getItem(), reservation.getLocation(), null, StockMovementType.OUT,
                quantity, null, reservation.getReferenceType(), reservation.getReferenceCode(),
                StockOutReasonCode.CONSUMPTION, "Consumption from reservation " + reservation.getReservationCode(),
                override, performedBy);
        saveMovementLots(movement, consumedLots);
        automationService.afterStockMovement(movement, level);
        return mapper.toMovementResponse(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockReservationResponse> searchReservations(StockReservationStatus status, Pageable pageable) {
        Page<StockReservation> reservations = status == null
                ? reservationRepository.findAll(pageable)
                : reservationRepository.findAllByStatus(status, pageable);
        return reservations.map(mapper::toReservationResponse);
    }

    @Override
    public StockMovementResponse reverseMovement(String movementCode, String performedBy, String reason) {
        StockMovement original = movementRepository.findByMovementCode(normalizeCode(movementCode))
                .orElseThrow(() -> new ResourceNotFoundException("Stock movement not found"));
        if (Boolean.TRUE.equals(original.getReversed())) {
            throw new BadRequestException("Stock movement is already reversed");
        }
        validateReversalAllowed(original);
        StockMovement reversal = switch (original.getMovementType()) {
            case IN, ADJUSTMENT_IN -> reverseIncrease(original, performedBy, reason);
            case OUT, ADJUSTMENT_OUT -> reverseDecrease(original, performedBy, reason);
            case TRANSFER -> reverseTransfer(original, performedBy, reason);
        };
        original.setReversed(Boolean.TRUE);
        original.setReversedAt(Instant.now());
        original.setReversedBy(trimToNull(performedBy));
        original.setReversalReason(reverseReason(reason, original));
        movementRepository.save(original);
        return mapper.toMovementResponse(reversal);
    }

    private void validateReversalAllowed(StockMovement movement) {
        if (movement.getReversalOfMovement() != null) {
            throw new BadRequestException("A reversal movement cannot be reversed");
        }
        if (movement.getReferenceType() == StockReferenceType.INVENTORY_COUNT) {
            throw new BadRequestException("Inventory count movements cannot be reversed directly. Reopen/correct the inventory count process.");
        }
        if (movement.getReferenceType() == StockReferenceType.GOODS_RECEIPT) {
            throw new BadRequestException("Goods receipt movements cannot be reversed directly. Use supplier return or goods receipt correction.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventorySerialResponse> getSerials(String itemCode, String locationCode, InventorySerialStatus status) {
        if (!StringUtils.hasText(itemCode)) throw new BadRequestException("itemCode is required");
        return serialRepository.findSerialsForItem(normalizeCode(itemCode), normalizeOptionalCode(locationCode), status)
                .stream()
                .map(s -> InventorySerialResponse.builder()
                        .itemCode(s.getItem().getItemCode())
                        .locationCode(s.getLocation().getLocationCode())
                        .serialNumber(s.getSerialNumber())
                        .status(s.getStatus())
                        .lotNumber(s.getLot() == null ? null : s.getLot().getLotNumber())
                        .receivedAt(s.getReceivedAt())
                        .issuedAt(s.getIssuedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelResponse> searchLevels(String itemCode, String locationCode, String categoryCode,
                                                 Boolean availableOnly, Boolean lowStock, Pageable pageable) {
        return stockLevelRepository.search(normalizeOptionalCode(itemCode), normalizeOptionalCode(locationCode),
                normalizeOptionalCode(categoryCode), availableOnly, lowStock, pageable).map(mapper::toLevelResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> searchMovements(String itemCode, String locationCode, StockMovementType movementType,
                                                       StockReferenceType referenceType, String referenceCode,
                                                       Instant fromDate, Instant toDate, Pageable pageable) {
        return movementRepository.search(normalizeOptionalCode(itemCode), normalizeOptionalCode(locationCode),
                movementType == null ? null : movementType.name(), referenceType == null ? null : referenceType.name(),
                normalizeOptionalCode(referenceCode), fromDate, toDate, unsortedPage(pageable)).map(mapper::toMovementResponse);
    }

    /**
     * Refuse une entree en stock sur un article qui ne se reapprovisionne plus.
     *
     * <p>L'ajustement reste autorise quel que soit le statut : c'est le seul chemin de correction,
     * le bloquer rendrait un stock errone impossible a remettre d'aplomb.</p>
     */
    private void ensureLifecycleAllowsReceipt(InventoryItem item) {
        if (!item.canReceiveStock()) {
            throw new BadRequestException("Item " + item.getItemCode() + " is in lifecycle status "
                    + item.getLifecycleStatus() + " and cannot receive stock");
        }
    }

    private void ensureLifecycleAllowsIssue(InventoryItem item) {
        if (!item.canIssueStock()) {
            throw new BadRequestException("Item " + item.getItemCode() + " is in lifecycle status "
                    + item.getLifecycleStatus() + " and cannot be issued or transferred");
        }
    }

    private StockLevel getOrCreateLevelForUpdate(InventoryItem item, InventoryLocation location) {
        return stockLevelRepository.findByItemAndLocationForUpdate(item, location)
                .orElseGet(() -> stockLevelRepository.save(StockLevel.builder()
                        .item(item)
                        .location(location)
                        .quantityOnHand(BigDecimal.ZERO)
                        .quantityReserved(BigDecimal.ZERO)
                        .quantityAvailable(BigDecimal.ZERO)
                        .build()));
    }

    private void ensureCanDecrease(InventoryItem item, StockLevel level, BigDecimal quantity, boolean override) {
        if (level.getQuantityAvailable().compareTo(quantity) < 0
                && !Boolean.TRUE.equals(item.getAllowNegativeStock())
                && !override) {
            throw new BadRequestException("Insufficient stock available");
        }
    }

    private void receiveLotIfNeeded(InventoryItem item, InventoryLocation location, StockInRequest request) {
        boolean tracked = isLotOrExpiryTracked(item);
        if (!tracked && !StringUtils.hasText(request.getLotNumber()) && request.getExpiryDate() == null) {
            return;
        }
        validateLotInput(item, request.getLotNumber(), request.getExpiryDate());
        upsertLot(item, location, normalizeOptionalCode(request.getLotNumber()), request.getExpiryDate(), request.getQuantity(),
                Boolean.TRUE.equals(request.getQuarantined()), request.getQuarantineReason(), request.getOwnershipType(), request.getOwnerCode());
    }

    private void receiveAdjustmentLotIfProvided(InventoryItem item, InventoryLocation location,
                                                StockAdjustmentRequest request, BigDecimal quantity) {
        if (!StringUtils.hasText(request.getLotNumber()) && request.getExpiryDate() == null) {
            if (isLotOrExpiryTracked(item)) {
                throw new BadRequestException("Lot number or expiry date is required for tracked inventory adjustment");
            }
            return;
        }
        validateLotInput(item, request.getLotNumber(), request.getExpiryDate());
        upsertLot(item, location, normalizeOptionalCode(request.getLotNumber()), request.getExpiryDate(), quantity,
                false, null, null, null);
    }

    private StockLot upsertLot(InventoryItem item, InventoryLocation location, String lotNumber, LocalDate expiryDate, BigDecimal quantity) {
        return upsertLot(item, location, lotNumber, expiryDate, quantity, false, null, null, null);
    }

    private StockLot upsertLot(InventoryItem item, InventoryLocation location, String lotNumber, LocalDate expiryDate,
                               BigDecimal quantity, boolean quarantined, String quarantineReason,
                               com.sni.bokaticowork.features.inventory.stock.enums.StockOwnershipType ownershipType,
                               String ownerCode) {
        String effectiveLotNumber = StringUtils.hasText(lotNumber) ? lotNumber : "NO-LOT-" + item.getItemCode();
        StockLot lot = stockLotRepository.findMatchingLot(item, location, effectiveLotNumber, expiryDate)
                .orElseGet(() -> StockLot.builder()
                        .item(item)
                        .location(location)
                        .lotNumber(effectiveLotNumber)
                        .expiryDate(expiryDate)
                        .initialQuantity(BigDecimal.ZERO)
                        .remainingQuantity(BigDecimal.ZERO)
                        .active(Boolean.TRUE)
                        .build());
        lot.setInitialQuantity(lot.getInitialQuantity().add(quantity));
        lot.setRemainingQuantity(lot.getRemainingQuantity().add(quantity));
        lot.setQuarantined(quarantined);
        lot.setQuarantineReason(trimToNull(quarantineReason));
        lot.setOwnershipType(ownershipType);
        lot.setOwnerCode(normalizeOptionalCode(ownerCode));
        return stockLotRepository.save(lot);
    }

    private void validateLotForConsumption(InventoryItem item, InventoryLocation location,
                                           String requestedLotNumber, boolean confirmCreate) {
        if (!isLotOrExpiryTracked(item) || !StringUtils.hasText(requestedLotNumber)) {
            return;
        }
        String normalized = normalizeOptionalCode(requestedLotNumber);
        boolean exists = stockLotRepository.findActiveLotForUpdate(item, location, normalized).isPresent();
        if (!exists && !confirmCreate) {
            throw new ConflictException("LOT_NOT_FOUND",
                    "Lot '" + normalized + "' does not exist at this location. "
                    + "To create it, set confirmCreateLot=true in your request.");
        }
    }

    private List<LotConsumption> consumeLotsIfTracked(InventoryItem item, InventoryLocation location,
                                                      BigDecimal quantity, String requestedLotNumber, boolean override) {
        if (!isLotOrExpiryTracked(item)) {
            return List.of();
        }
        BigDecimal remaining = quantity;
        List<LotConsumption> consumed = new ArrayList<>();
        List<StockLot> lots = stockLotRepository.findConsumableLotsForUpdate(item, location);
        if (StringUtils.hasText(requestedLotNumber)) {
            String normalized = normalizeOptionalCode(requestedLotNumber);
            lots = lots.stream().filter(l -> normalized.equalsIgnoreCase(l.getLotNumber())).toList();
        }
        for (StockLot lot : lots) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal consumedQuantity = lot.getRemainingQuantity().min(remaining);
            lot.setRemainingQuantity(lot.getRemainingQuantity().subtract(consumedQuantity));
            stockLotRepository.save(lot);
            consumed.add(new LotConsumption(lot, lot.getLotNumber(), lot.getExpiryDate(), consumedQuantity));
            remaining = remaining.subtract(consumedQuantity);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && !override) {
            throw new BadRequestException("Insufficient lot quantity available");
        }
        return consumed;
    }

    private void moveConsumedLotsToDestination(InventoryItem item, InventoryLocation to, List<LotConsumption> consumedLots) {
        for (LotConsumption consumed : consumedLots) {
            upsertLot(item, to, consumed.lotNumber(), consumed.expiryDate(), consumed.quantity());
        }
    }

    private void saveMovementLots(StockMovement movement, List<LotConsumption> consumedLots) {
        for (LotConsumption consumed : consumedLots) {
            movementLotRepository.save(StockMovementLot.builder()
                    .movement(movement)
                    .stockLot(consumed.stockLot())
                    .lotNumber(consumed.lotNumber())
                    .expiryDate(consumed.expiryDate())
                    .quantity(consumed.quantity())
                    .build());
        }
    }

    private StockMovement reverseIncrease(StockMovement original, String performedBy, String reason) {
        InventoryLocation location = original.getLocationTo();
        StockLevel level = getOrCreateLevelForUpdate(original.getItem(), location);
        ensureCanDecrease(original.getItem(), level, original.getQuantity(), false);
        List<LotConsumption> consumedLots = consumeLotsIfTracked(original.getItem(), location, original.getQuantity(), null, false);
        level.setQuantityOnHand(level.getQuantityOnHand().subtract(original.getQuantity()));
        level.recalculateAvailable();
        stockLevelRepository.save(level);
        StockMovement reversal = saveMovement(original.getItem(), location, null, StockMovementType.ADJUSTMENT_OUT,
                original.getQuantity(), original.getUnitCost(), original.getReferenceType(), original.getReferenceCode(),
                null, reverseReason(reason, original), false, performedBy);
        reversal.setReversalOfMovement(original);
        reversal = movementRepository.save(reversal);
        saveMovementLots(reversal, consumedLots);
        automationService.afterStockMovement(reversal, level);
        return reversal;
    }

    private StockMovement reverseDecrease(StockMovement original, String performedBy, String reason) {
        InventoryLocation location = original.getLocationFrom();
        StockLevel level = getOrCreateLevelForUpdate(original.getItem(), location);
        for (StockMovementLot movementLot : movementLotRepository.findAllByMovement(original)) {
            upsertLot(original.getItem(), location, movementLot.getLotNumber(), movementLot.getExpiryDate(), movementLot.getQuantity());
        }
        level.setQuantityOnHand(level.getQuantityOnHand().add(original.getQuantity()));
        level.recalculateAvailable();
        stockLevelRepository.save(level);
        StockMovement reversal = saveMovement(original.getItem(), null, location, StockMovementType.ADJUSTMENT_IN,
                original.getQuantity(), original.getUnitCost(), original.getReferenceType(), original.getReferenceCode(),
                null, reverseReason(reason, original), false, performedBy);
        reversal.setReversalOfMovement(original);
        reversal = movementRepository.save(reversal);
        automationService.afterStockMovement(reversal, level);
        return reversal;
    }

    private StockMovement reverseTransfer(StockMovement original, String performedBy, String reason) {
        StockLevel sourceLevel = getOrCreateLevelForUpdate(original.getItem(), original.getLocationTo());
        StockLevel destinationLevel = getOrCreateLevelForUpdate(original.getItem(), original.getLocationFrom());
        ensureCanDecrease(original.getItem(), sourceLevel, original.getQuantity(), false);
        List<LotConsumption> consumedLots = consumeLotsIfTracked(original.getItem(), original.getLocationTo(), original.getQuantity(), null, false);
        moveConsumedLotsToDestination(original.getItem(), original.getLocationFrom(), consumedLots);
        sourceLevel.setQuantityOnHand(sourceLevel.getQuantityOnHand().subtract(original.getQuantity()));
        destinationLevel.setQuantityOnHand(destinationLevel.getQuantityOnHand().add(original.getQuantity()));
        sourceLevel.recalculateAvailable();
        destinationLevel.recalculateAvailable();
        stockLevelRepository.save(sourceLevel);
        stockLevelRepository.save(destinationLevel);
        StockMovement reversal = saveMovement(original.getItem(), original.getLocationTo(), original.getLocationFrom(), StockMovementType.TRANSFER,
                original.getQuantity(), original.getUnitCost(), original.getReferenceType(), original.getReferenceCode(),
                null, reverseReason(reason, original), false, performedBy);
        reversal.setReversalOfMovement(original);
        reversal = movementRepository.save(reversal);
        saveMovementLots(reversal, consumedLots);
        automationService.afterStockMovement(reversal, sourceLevel, destinationLevel);
        return reversal;
    }

    private String reverseReason(String reason, StockMovement original) {
        return StringUtils.hasText(reason) ? reason.trim() : "Reversal of movement " + original.getMovementCode();
    }

    private boolean isLotOrExpiryTracked(InventoryItem item) {
        return Boolean.TRUE.equals(item.getRequiresLotNumber())
                || Boolean.TRUE.equals(item.getRequiresExpiryDate())
                || item.getTrackingType() == InventoryTrackingType.LOT
                || item.getTrackingType() == InventoryTrackingType.EXPIRY;
    }

    private void validateLotInput(InventoryItem item, String lotNumber, LocalDate expiryDate) {
        if ((Boolean.TRUE.equals(item.getRequiresLotNumber()) || item.getTrackingType() == InventoryTrackingType.LOT)
                && !StringUtils.hasText(lotNumber)) {
            throw new BadRequestException("Lot number is required for this inventory item");
        }
        if ((Boolean.TRUE.equals(item.getRequiresExpiryDate()) || item.getTrackingType() == InventoryTrackingType.EXPIRY)
                && expiryDate == null) {
            throw new BadRequestException("Expiry date is required for this inventory item");
        }
    }

    private boolean requiresSerialTracking(InventoryItem item) {
        return Boolean.TRUE.equals(item.getRequiresSerialNumber())
                && item.getItemType() != InventoryItemType.ASSET;
    }

    private void receiveSerialNumbersIfNeeded(InventoryItem item, InventoryLocation location, StockInRequest request) {
        if (!requiresSerialTracking(item)) {
            return;
        }
        List<String> serials = request.getAssetSerialNumbers() == null ? List.of() : request.getAssetSerialNumbers();
        int count = toWholeUnitCount(request.getQuantity());
        if (serials.isEmpty()) {
            throw new BadRequestException("Serial numbers are required for this item: expected " + count);
        }
        if (serials.size() != count) {
            throw new BadRequestException("Serial numbers count (" + serials.size() + ") must match received quantity (" + count + ")");
        }
        for (String serial : serials) {
            String normalized = trimToNull(serial);
            if (normalized == null) throw new BadRequestException("Serial number must not be blank");
            if (serialRepository.existsByItemAndSerialNumber(item, normalized)) {
                throw new BadRequestException("Serial number already registered: " + normalized);
            }
            serialRepository.save(InventoryItemSerial.builder()
                    .item(item)
                    .location(location)
                    .serialNumber(normalized)
                    .status(InventorySerialStatus.AVAILABLE)
                    .receivedAt(Instant.now())
                    .build());
        }
    }

    private void issueSerialNumbersIfNeeded(InventoryItem item, InventoryLocation location, List<String> serialNumbers) {
        if (!requiresSerialTracking(item)) {
            return;
        }
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            throw new BadRequestException("Serial numbers are required for this item");
        }
        for (String serial : serialNumbers) {
            String normalized = trimToNull(serial);
            if (normalized == null) throw new BadRequestException("Serial number must not be blank");
            InventoryItemSerial tracked = serialRepository
                    .findByItemAndSerialNumberAndStatus(item, normalized, InventorySerialStatus.AVAILABLE)
                    .orElseThrow(() -> new BadRequestException("Serial number not available for issuance: " + normalized));
            if (!tracked.getLocation().getId().equals(location.getId())) {
                throw new BadRequestException("Serial number " + normalized + " is not at the requested location");
            }
            tracked.setStatus(InventorySerialStatus.ISSUED);
            tracked.setIssuedAt(Instant.now());
            serialRepository.save(tracked);
        }
    }

    private void transferSerialNumbersIfNeeded(InventoryItem item, InventoryLocation from, InventoryLocation to, List<String> serialNumbers) {
        if (!requiresSerialTracking(item)) {
            return;
        }
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            throw new BadRequestException("Serial numbers are required for this item");
        }
        for (String serial : serialNumbers) {
            String normalized = trimToNull(serial);
            if (normalized == null) throw new BadRequestException("Serial number must not be blank");
            InventoryItemSerial tracked = serialRepository
                    .findByItemAndSerialNumberAndStatus(item, normalized, InventorySerialStatus.AVAILABLE)
                    .orElseThrow(() -> new BadRequestException("Serial number not available for transfer: " + normalized));
            if (!tracked.getLocation().getId().equals(from.getId())) {
                throw new BadRequestException("Serial number " + normalized + " is not at the source location");
            }
            tracked.setLocation(to);
            tracked.setStatus(InventorySerialStatus.AVAILABLE);
            serialRepository.save(tracked);
        }
    }

    private void createAssetsFromReceiptIfNeeded(InventoryItem item, InventoryLocation location, StockInRequest request) {
        if (item.getItemType() != InventoryItemType.ASSET) {
            return;
        }
        int count = toWholeUnitCount(request.getQuantity());
        List<String> serials = request.getAssetSerialNumbers() == null ? List.of() : request.getAssetSerialNumbers();
        List<String> tags = request.getAssetTags() == null ? List.of() : request.getAssetTags();
        if (!serials.isEmpty() && serials.size() != count) {
            throw new BadRequestException("Asset serial numbers count must match received quantity");
        }
        if (!tags.isEmpty() && tags.size() != count) {
            throw new BadRequestException("Asset tags count must match received quantity");
        }
        for (int i = 0; i < count; i++) {
            String assetCode = generateAssetCode();
            String serial = i < serials.size() ? normalizeOptionalCode(serials.get(i)) : null;
            String tag = i < tags.size() ? normalizeOptionalCode(tags.get(i)) : assetCode;
            if (assetRepository.existsConflictingCodes(null, serial, tag)) {
                throw new BadRequestException("Asset serial number or tag already exists");
            }
            Asset asset = Asset.builder()
                    .assetCode(assetCode)
                    .item(item)
                    .serialNumber(serial)
                    .assetTag(tag)
                    .status(AssetStatus.AVAILABLE)
                    .condition(AssetCondition.GOOD)
                    .location(location)
                    .purchaseCost(request.getUnitCost())
                    .notes("Auto-created from stock receipt " + trimToNull(request.getReferenceCode()))
                    .build();
            assetRepository.save(asset);
            automationService.publishAssetEvent("inventory.asset.created", assetCode, java.util.Map.of(
                    "assetCode", assetCode,
                    "itemCode", item.getItemCode(),
                    "locationCode", location.getLocationCode()
            ));
        }
    }

    private int toWholeUnitCount(BigDecimal quantity) {
        try {
            return quantity.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Asset stock receipt quantity must be a whole number");
        }
    }

    private StockReservation activeReservationForUpdate(String reservationCode) {
        StockReservation reservation = reservationRepository.findByReservationCodeForUpdate(normalizeCode(reservationCode))
                .orElseThrow(() -> new ResourceNotFoundException("Stock reservation not found"));
        if (reservation.getStatus() != StockReservationStatus.ACTIVE) {
            throw new BadRequestException("Stock reservation is not active");
        }
        if (reservation.getExpiresAt() != null && reservation.getExpiresAt().isBefore(Instant.now())) {
            expireReservation(reservation);
            throw new BadRequestException("Stock reservation has expired");
        }
        return reservation;
    }

    private void ensureCanConsumeReserved(InventoryItem item, StockLevel level, BigDecimal quantity, boolean override) {
        if (level.getQuantityOnHand().compareTo(quantity) < 0
                && !Boolean.TRUE.equals(item.getAllowNegativeStock())
                && !override) {
            throw new BadRequestException("Insufficient stock on hand");
        }
    }

    private void expireReservation(StockReservation reservation) {
        StockLevel level = getOrCreateLevelForUpdate(reservation.getItem(), reservation.getLocation());
        level.setQuantityReserved(level.getQuantityReserved().subtract(reservation.getQuantity()).max(BigDecimal.ZERO));
        level.recalculateAvailable();
        stockLevelRepository.save(level);
        reservation.setStatus(StockReservationStatus.EXPIRED);
        reservation.setClosedAt(Instant.now());
        reservationRepository.save(reservation);
    }

    private StockMovement saveMovement(InventoryItem item, InventoryLocation from, InventoryLocation to,
                                       StockMovementType movementType, BigDecimal quantity, Long unitCost,
                                       StockReferenceType referenceType, String referenceCode,
                                       StockOutReasonCode reasonCode, String reason,
                                       boolean override, String performedBy) {
        return saveMovement(generateMovementCode(movementType), item, from, to, movementType, quantity, unitCost,
                referenceType, referenceCode, reasonCode, reason, override, performedBy);
    }

    /**
     * Variante avec code impose, utilisee lorsque le code doit etre connu avant l'enregistrement,
     * par exemple pour rattacher une couche de cout au mouvement qui la cree.
     */
    private StockMovement saveMovement(String movementCode,
                                       InventoryItem item, InventoryLocation from, InventoryLocation to,
                                       StockMovementType movementType, BigDecimal quantity, Long unitCost,
                                       StockReferenceType referenceType, String referenceCode,
                                       StockOutReasonCode reasonCode, String reason,
                                       boolean override, String performedBy) {
        StockMovement movement = StockMovement.builder()
                .movementCode(movementCode)
                .item(item)
                .locationFrom(from)
                .locationTo(to)
                .movementType(movementType)
                .quantity(quantity)
                .unitCost(unitCost)
                .referenceType(referenceType)
                .referenceCode(normalizeOptionalCode(referenceCode))
                .reasonCode(reasonCode)
                .reason(trimToNull(reason))
                .allowNegativeOverride(override)
                .performedBy(trimToNull(performedBy))
                .performedAt(Instant.now())
                .build();
        return movementRepository.save(movement);
    }

    private String generateMovementCode(StockMovementType type) {
        String code;
        do {
            String sequence = type == StockMovementType.ADJUSTMENT_IN || type == StockMovementType.ADJUSTMENT_OUT
                    ? "stock_adjustment"
                    : "stock_movement";
            code = sequenceGenerator.next(sequence, java.time.LocalDate.now()) + "-" + System.currentTimeMillis();
        } while (movementRepository.existsByMovementCode(code));
        return code;
    }

    private String generateReservationCode() {
        String code;
        do {
            code = sequenceGenerator.next("stock_reservation", LocalDate.now()) + "-" + System.currentTimeMillis();
        } while (reservationRepository.existsByReservationCode(code));
        return code;
    }

    private String generateAssetCode() {
        String code;
        do {
            code = sequenceGenerator.next("asset", LocalDate.now()) + "-" + System.currentTimeMillis();
        } while (assetRepository.existsByAssetCode(code));
        return code;
    }

    private StockReferenceType defaultReferenceType(StockReferenceType referenceType) {
        return referenceType == null ? StockReferenceType.MANUAL_ADJUSTMENT : referenceType;
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) throw new BadRequestException("Code is required");
        return normalizeOptionalCode(value);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Pageable unsortedPage(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private record LotConsumption(StockLot stockLot, String lotNumber, LocalDate expiryDate, BigDecimal quantity) {
    }
}
