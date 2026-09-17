package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotBlockRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.ProductRecallRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.ProductRecallResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.LotBlockReasonType;
import com.sni.bokaticowork.features.inventory.stock.enums.ProductRecallStatus;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.model.ProductRecall;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovementLot;
import com.sni.bokaticowork.features.inventory.stock.repository.ProductRecallRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementLotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.ProductRecallService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockQuarantineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductRecallServiceImpl implements ProductRecallService {

    private static final String RECALL_SEQUENCE = "product_recall";

    private final ProductRecallRepository recallRepository;
    private final StockMovementLotRepository movementLotRepository;
    private final InventoryItemLookupService itemLookupService;
    private final StockQuarantineService quarantineService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public ProductRecallResponse create(ProductRecallRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());

        ProductRecall recall = ProductRecall.builder()
                .recallCode(sequenceGenerator.next(RECALL_SEQUENCE, LocalDate.now()))
                .item(item)
                .lotNumberFrom(normalizeOptional(request.getLotNumberFrom()))
                .lotNumberTo(normalizeOptional(request.getLotNumberTo()))
                .status(ProductRecallStatus.DRAFT)
                .reason(request.getReason().trim())
                .build();

        return toResponse(recallRepository.save(recall), false);
    }

    @Override
    public ProductRecallResponse launch(String recallCode, String launchedBy) {
        ProductRecall recall = findOrThrow(recallCode);
        if (recall.getStatus() != ProductRecallStatus.DRAFT) {
            throw new BadRequestException("Recall " + recallCode + " is not in draft state");
        }

        List<StockLot> lots = affectedLots(recall);
        if (lots.isEmpty()) {
            throw new BadRequestException("Recall " + recallCode + " matches no lot. Check the lot number range.");
        }

        // Geler d abord, communiquer ensuite : tant que les lots ne sont pas bloques, la marchandise
        // rappelee peut continuer de sortir.
        BigDecimal inStock = BigDecimal.ZERO;
        int frozen = 0;
        for (StockLot lot : lots) {
            inStock = inStock.add(lot.getRemainingQuantity() == null ? BigDecimal.ZERO : lot.getRemainingQuantity());
            if (Boolean.TRUE.equals(lot.getBlocked())) {
                continue;
            }
            LotBlockRequest block = new LotBlockRequest();
            block.setReasonType(LotBlockReasonType.RECALL);
            block.setReason("Rappel " + recall.getRecallCode() + " : " + recall.getReason());
            block.setBlockedBy(launchedBy);
            quarantineService.block(lot.getId(), block);
            frozen++;
        }

        List<ProductRecallResponse.Holder> holders = holdersOf(lots);
        BigDecimal issued = holders.stream()
                .map(ProductRecallResponse.Holder::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        recall.setStatus(ProductRecallStatus.ACTIVE);
        recall.setFrozenLotCount(frozen);
        recall.setQuantityInStock(inStock);
        recall.setQuantityIssued(issued);
        recall.setLaunchedBy(launchedBy);
        recall.setLaunchedAt(Instant.now());

        log.warn("Product recall {} launched by {} : {} lots frozen, {} still in stock, {} already issued",
                recall.getRecallCode(), launchedBy, frozen, inStock, issued);
        return toResponse(recallRepository.save(recall), true);
    }

    @Override
    public ProductRecallResponse recordRecovery(String recallCode, BigDecimal quantity) {
        ProductRecall recall = findOrThrow(recallCode);
        if (recall.getStatus() != ProductRecallStatus.ACTIVE) {
            throw new BadRequestException("Recall " + recallCode + " is not active");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Recovered quantity must be positive");
        }

        recall.setQuantityRecovered(recall.getQuantityRecovered().add(quantity));
        return toResponse(recallRepository.save(recall), false);
    }

    @Override
    public ProductRecallResponse close(String recallCode, String closedBy) {
        ProductRecall recall = findOrThrow(recallCode);
        if (recall.getStatus() == ProductRecallStatus.CLOSED) {
            throw new BadRequestException("Recall " + recallCode + " is already closed");
        }

        recall.setStatus(ProductRecallStatus.CLOSED);
        recall.setClosedBy(closedBy);
        recall.setClosedAt(Instant.now());
        return toResponse(recallRepository.save(recall), false);
    }

    @Override
    public ProductRecallResponse cancel(String recallCode, String cancelledBy) {
        ProductRecall recall = findOrThrow(recallCode);
        if (recall.getStatus() == ProductRecallStatus.CLOSED) {
            throw new BadRequestException("A closed recall cannot be cancelled");
        }

        // Annuler un rappel libere les lots : les laisser bloques sans rappel actif immobiliserait
        // du stock sans motif lisible.
        for (StockLot lot : affectedLots(recall)) {
            if (Boolean.TRUE.equals(lot.getBlocked())
                    && lot.getBlockReasonType() == LotBlockReasonType.RECALL) {
                quarantineService.unblock(lot.getId(), cancelledBy);
            }
        }

        recall.setStatus(ProductRecallStatus.CANCELLED);
        recall.setClosedBy(cancelledBy);
        recall.setClosedAt(Instant.now());
        return toResponse(recallRepository.save(recall), false);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRecallResponse get(String recallCode) {
        return toResponse(findOrThrow(recallCode), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductRecallResponse> list() {
        return recallRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(recall -> toResponse(recall, false))
                .toList();
    }

    private List<StockLot> affectedLots(ProductRecall recall) {
        return recallRepository.findLotsInRange(recall.getItem(),
                recall.getLotNumberFrom(), recall.getLotNumberTo());
    }

    /**
     * Reconstitue les detenteurs depuis la tracabilite descendante : toute sortie d un lot rappele
     * designe quelqu un qui detient encore de la marchandise.
     */
    private List<ProductRecallResponse.Holder> holdersOf(List<StockLot> lots) {
        List<ProductRecallResponse.Holder> holders = new ArrayList<>();
        for (StockLot lot : lots) {
            for (StockMovementLot line : movementLotRepository.findAllByLotNumber(lot.getLotNumber())) {
                StockMovement movement = line.getMovement();
                if (movement.getMovementType() == StockMovementType.IN
                        || movement.getMovementType() == StockMovementType.ADJUSTMENT_IN) {
                    continue;
                }
                if (Boolean.TRUE.equals(movement.getReversed())) {
                    continue;
                }
                holders.add(ProductRecallResponse.Holder.builder()
                        .lotNumber(line.getLotNumber())
                        .movementCode(movement.getMovementCode())
                        .locationCode(movement.getLocationFrom() == null
                                ? null : movement.getLocationFrom().getLocationCode())
                        .quantity(line.getQuantity())
                        .referenceType(movement.getReferenceType() == null
                                ? null : movement.getReferenceType().name())
                        .referenceCode(movement.getReferenceCode())
                        .issuedAt(movement.getPerformedAt())
                        .build());
            }
        }
        return holders;
    }

    private ProductRecall findOrThrow(String recallCode) {
        return recallRepository.findByRecallCode(normalize(recallCode))
                .orElseThrow(() -> new ResourceNotFoundException("Product recall not found: " + recallCode));
    }

    private ProductRecallResponse toResponse(ProductRecall recall, boolean withDetail) {
        List<StockLot> lots = withDetail ? affectedLots(recall) : List.of();

        return ProductRecallResponse.builder()
                .recallCode(recall.getRecallCode())
                .itemCode(recall.getItem().getItemCode())
                .lotNumberFrom(recall.getLotNumberFrom())
                .lotNumberTo(recall.getLotNumberTo())
                .status(recall.getStatus())
                .reason(recall.getReason())
                .frozenLotCount(recall.getFrozenLotCount())
                .quantityInStock(recall.getQuantityInStock())
                .quantityIssued(recall.getQuantityIssued())
                .quantityRecovered(recall.getQuantityRecovered())
                .recoveryRate(recall.recoveryRate())
                .launchedBy(recall.getLaunchedBy())
                .launchedAt(recall.getLaunchedAt())
                .closedBy(recall.getClosedBy())
                .closedAt(recall.getClosedAt())
                .affectedLotNumbers(lots.stream().map(StockLot::getLotNumber).distinct().toList())
                .holders(withDetail ? holdersOf(lots) : List.of())
                .build();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Recall code is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }
}
