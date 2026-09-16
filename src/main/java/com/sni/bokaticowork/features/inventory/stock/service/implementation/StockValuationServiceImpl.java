package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.ValuationMethod;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockCostLayer;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.StockCostLayerRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockValuationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockValuationServiceImpl implements StockValuationService {

    private final StockCostLayerRepository costLayerRepository;
    private final StockLevelRepository stockLevelRepository;

    /** Ce qu'une operation a preleve sur une couche donnee. */
    private record LayerConsumption(StockCostLayer layer, BigDecimal taken) {
    }

    @Override
    public Long recordEntry(InventoryItem item,
                            InventoryLocation location,
                            StockLevel level,
                            BigDecimal previousQuantity,
                            BigDecimal quantity,
                            Long unitCost,
                            String movementCode,
                            String lotNumber) {

        // Le cout moyen est maintenu quelle que soit la methode : le tableau de bord et la valeur
        // totale du stock s'appuient dessus et ne doivent pas changer de comportement.
        level.setAverageCost(weightedAverage(previousQuantity, level.getAverageCost(), quantity, unitCost));

        if (!item.usesCostLayers()) {
            return unitCost;
        }

        Long layerCost = resolveLayerCost(item, level, unitCost);
        costLayerRepository.save(StockCostLayer.builder()
                .item(item)
                .location(location)
                .initialQuantity(quantity)
                .remainingQuantity(quantity)
                .unitCost(layerCost)
                .receivedAt(Instant.now())
                .sourceMovementCode(movementCode)
                .lotNumber(lotNumber)
                .seeded(Boolean.FALSE)
                .build());

        return layerCost;
    }

    @Override
    public Long recordExit(InventoryItem item,
                           InventoryLocation location,
                           StockLevel level,
                           BigDecimal quantity,
                           boolean allowShortfall) {

        if (!item.usesCostLayers()) {
            // En cout moyen pondere, la sortie est valorisee au cout moyen courant et ne le modifie pas.
            return level.getAverageCost();
        }

        List<LayerConsumption> consumed = consume(item, location, quantity, allowShortfall);
        Long unitCost = weightedCostOf(consumed, quantity, level.getAverageCost());
        refreshAverageCostFromLayers(item, location, level);
        return unitCost;
    }

    @Override
    public Long recordTransfer(InventoryItem item,
                               InventoryLocation from,
                               InventoryLocation to,
                               StockLevel fromLevel,
                               StockLevel toLevel,
                               BigDecimal quantity,
                               String movementCode,
                               boolean allowShortfall) {

        if (!item.usesCostLayers()) {
            // Comportement historique conserve a l'identique.
            toLevel.setAverageCost(fromLevel.getAverageCost());
            return fromLevel.getAverageCost();
        }

        List<LayerConsumption> consumed = consume(item, from, quantity, allowShortfall);

        // Les couches sont recreees a destination avec leur cout et leur date d'origine : un simple
        // deplacement ne doit ni rajeunir le stock, ni en changer la valeur.
        for (LayerConsumption consumption : consumed) {
            costLayerRepository.save(StockCostLayer.builder()
                    .item(item)
                    .location(to)
                    .initialQuantity(consumption.taken())
                    .remainingQuantity(consumption.taken())
                    .unitCost(consumption.layer().getUnitCost())
                    .receivedAt(consumption.layer().getReceivedAt())
                    .sourceMovementCode(movementCode)
                    .lotNumber(consumption.layer().getLotNumber())
                    .seeded(consumption.layer().getSeeded())
                    .build());
        }

        Long unitCost = weightedCostOf(consumed, quantity, fromLevel.getAverageCost());
        refreshAverageCostFromLayers(item, from, fromLevel);
        refreshAverageCostFromLayers(item, to, toLevel);
        return unitCost;
    }

    @Override
    public int switchValuationMethod(InventoryItem item, ValuationMethod target) {
        ValuationMethod current = item.effectiveValuationMethod();
        item.setValuationMethod(target);
        item.setValuationMethodSince(Instant.now());

        if (current == target || !target.isLayered()) {
            return 0;
        }

        // Amorcage : une couche par emplacement portant du stock, au cout moyen du moment.
        int seeded = 0;
        for (StockLevel level : stockLevelRepository.findAllByItemIdOrderByQuantityAvailableAsc(item.getId())) {
            if (level.getQuantityOnHand() == null || level.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (costLayerRepository.existsByItemAndLocation(item, level.getLocation())) {
                continue;
            }
            costLayerRepository.save(StockCostLayer.builder()
                    .item(item)
                    .location(level.getLocation())
                    .initialQuantity(level.getQuantityOnHand())
                    .remainingQuantity(level.getQuantityOnHand())
                    .unitCost(level.getAverageCost() == null ? 0L : level.getAverageCost())
                    .receivedAt(Instant.now())
                    .seeded(Boolean.TRUE)
                    .build());
            seeded++;
        }

        log.info("Item {} switched to {} valuation, {} seeded cost layers", item.getItemCode(), target, seeded);
        return seeded;
    }

    /**
     * Consomme les couches les plus anciennes a hauteur de la quantite demandee, et rend le detail
     * de ce qui a ete preleve sur chacune.
     *
     * <p>Le prelevement doit etre rendu explicitement : une couche peut avoir ete partiellement
     * consommee auparavant, sa quantite restante ne suffit donc pas a retrouver la part qui vient
     * d'en sortir.</p>
     */
    private List<LayerConsumption> consume(InventoryItem item,
                                           InventoryLocation location,
                                           BigDecimal quantity,
                                           boolean allowShortfall) {

        List<LayerConsumption> consumed = new ArrayList<>();
        BigDecimal remaining = quantity;

        for (StockCostLayer layer : costLayerRepository.findConsumableForUpdate(item, location)) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal taken = layer.getRemainingQuantity().min(remaining);
            layer.setRemainingQuantity(layer.getRemainingQuantity().subtract(taken));
            costLayerRepository.save(layer);
            consumed.add(new LayerConsumption(layer, taken));
            remaining = remaining.subtract(taken);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // Sortie non couverte par les couches : stock amorce partiellement, ou forcage de stock
            // negatif. Le solde sera valorise au cout moyen plutot que de bloquer le mouvement.
            log.warn("Item {} at {} issued {} units beyond available cost layers, valued at average cost",
                    item.getItemCode(), location.getLocationCode(), remaining);
        }
        return consumed;
    }

    /**
     * Cout unitaire d'une sortie : moyenne ponderee de ce qui a ete preleve sur les couches, le
     * solde eventuellement non couvert etant valorise au cout moyen.
     */
    private Long weightedCostOf(List<LayerConsumption> consumed, BigDecimal quantity, Long fallback) {
        if (consumed.isEmpty() || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return fallback;
        }

        BigDecimal value = BigDecimal.ZERO;
        BigDecimal covered = BigDecimal.ZERO;
        for (LayerConsumption consumption : consumed) {
            value = value.add(consumption.taken().multiply(BigDecimal.valueOf(consumption.layer().getUnitCost())));
            covered = covered.add(consumption.taken());
        }

        if (covered.compareTo(BigDecimal.ZERO) <= 0) {
            return fallback;
        }

        BigDecimal shortfall = quantity.subtract(covered);
        if (shortfall.compareTo(BigDecimal.ZERO) > 0 && fallback != null) {
            value = value.add(shortfall.multiply(BigDecimal.valueOf(fallback)));
            covered = quantity;
        }
        return value.divide(covered, 0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * Recale le cout moyen sur la valeur reelle des couches restantes.
     *
     * <p>C'est ce qui permet au tableau de bord et a {@code totalStockValue}, tous deux bases sur
     * {@code averageCost}, de rester justes pour un article en FIFO.</p>
     */
    private void refreshAverageCostFromLayers(InventoryItem item, InventoryLocation location, StockLevel level) {
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal value = BigDecimal.ZERO;
        for (StockCostLayer layer : costLayerRepository.findConsumableReadOnly(item, location)) {
            quantity = quantity.add(layer.getRemainingQuantity());
            value = value.add(layer.remainingValue());
        }
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            level.setAverageCost(value.divide(quantity, 0, RoundingMode.HALF_UP).longValue());
        }
    }

    private Long resolveLayerCost(InventoryItem item, StockLevel level, Long unitCost) {
        if (unitCost != null) {
            return unitCost;
        }
        if (level.getAverageCost() != null) {
            return level.getAverageCost();
        }
        return item.getDefaultCost() == null ? 0L : item.getDefaultCost();
    }

    /**
     * Cout moyen pondere, repris a l'identique de l'implementation anterieure au lot 2.
     */
    private Long weightedAverage(BigDecimal oldQuantity, Long oldCost, BigDecimal inQuantity, Long inCost) {
        if (inCost == null) {
            return oldCost;
        }
        BigDecimal safeOldQuantity = oldQuantity == null ? BigDecimal.ZERO : oldQuantity;
        BigDecimal safeOldCost = oldCost == null ? BigDecimal.ZERO : BigDecimal.valueOf(oldCost);
        BigDecimal totalQuantity = safeOldQuantity.add(inQuantity);
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return inCost;
        }
        BigDecimal oldValue = safeOldQuantity.multiply(safeOldCost);
        BigDecimal inValue = inQuantity.multiply(BigDecimal.valueOf(inCost));
        return oldValue.add(inValue).divide(totalQuantity, 0, RoundingMode.HALF_UP).longValue();
    }
}
