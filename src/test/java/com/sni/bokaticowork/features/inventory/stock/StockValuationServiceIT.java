package com.sni.bokaticowork.features.inventory.stock;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.enums.ValuationMethod;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockCostLayer;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryLocationRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockCostLayerRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.service.implementation.StockValuationServiceImpl;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie que le cout moyen pondere et le FIFO coexistent, et que le FIFO ne s'active que pour les
 * articles qui le declarent.
 *
 * <p>C'est le controle central de la promesse de non-regression du lot 2 : un article qui ne declare
 * rien ne doit creer aucune couche de cout et se comporter exactement comme avant.</p>
 */
@Import(StockValuationServiceImpl.class)
class StockValuationServiceIT extends PostgresIntegrationTestBase {

    private static final AtomicLong SEQUENCE = new AtomicLong(9_960_000L);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private StockValuationServiceImpl valuationService;
    @Autowired
    private StockCostLayerRepository costLayerRepository;
    @Autowired
    private StockLevelRepository stockLevelRepository;
    @Autowired
    private InventoryItemRepository itemRepository;
    @Autowired
    private InventoryLocationRepository locationRepository;

    private JdbcTemplate jdbc;
    private InventoryLocation depot;
    private InventoryLocation site;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        depot = newLocation("DEPOT");
        site = newLocation("SITE");
    }

    @Test
    void weightedAverageCreatesNoCostLayerAtAll() {
        InventoryItem item = newItem(null);
        StockLevel level = newLevel(item, depot);

        valuationService.recordEntry(item, depot, level, BigDecimal.ZERO, new BigDecimal("100"), 5000L, "MOV-1", null);

        assertFalse(costLayerRepository.existsByItem(item),
                "Un article en cout moyen ne doit creer aucune couche");
        assertEquals(5000L, level.getAverageCost());
    }

    @Test
    void weightedAverageSmoothsTwoPurchasesAtDifferentPrices() {
        InventoryItem item = newItem(null);
        StockLevel level = newLevel(item, depot);

        valuationService.recordEntry(item, depot, level, BigDecimal.ZERO, new BigDecimal("100"), 5000L, "MOV-1", null);
        valuationService.recordEntry(item, depot, level, new BigDecimal("100"), new BigDecimal("100"), 7000L, "MOV-2", null);

        assertEquals(6000L, level.getAverageCost());

        Long exitCost = valuationService.recordExit(item, depot, level, new BigDecimal("100"), false);
        assertEquals(6000L, exitCost, "En cout moyen, la sortie vaut le cout moyen");
        assertEquals(6000L, level.getAverageCost(), "Une sortie ne modifie pas le cout moyen");
    }

    @Test
    void fifoValuesTheExitAtTheCostOfTheOldestEntries() {
        InventoryItem item = newItem(ValuationMethod.FIFO);
        StockLevel level = newLevel(item, depot);

        valuationService.recordEntry(item, depot, level, BigDecimal.ZERO, new BigDecimal("100"), 5000L, "MOV-1", null);
        valuationService.recordEntry(item, depot, level, new BigDecimal("100"), new BigDecimal("100"), 7000L, "MOV-2", null);

        Long exitCost = valuationService.recordExit(item, depot, level, new BigDecimal("100"), false);

        // Les cent premiers sacs sont les moins chers : la sortie coute 5000, pas la moyenne de 6000.
        assertEquals(5000L, exitCost);
        // Et le stock restant vaut les entrees recentes, donc 7000 et non 6000.
        assertEquals(7000L, level.getAverageCost());
    }

    @Test
    void fifoStraddlesTwoLayersWhenTheExitExceedsTheOldest() {
        InventoryItem item = newItem(ValuationMethod.FIFO);
        StockLevel level = newLevel(item, depot);

        valuationService.recordEntry(item, depot, level, BigDecimal.ZERO, new BigDecimal("100"), 5000L, "MOV-1", null);
        valuationService.recordEntry(item, depot, level, new BigDecimal("100"), new BigDecimal("100"), 7000L, "MOV-2", null);

        // 150 unites : 100 a 5000 puis 50 a 7000, soit 850 000 pour 150, donc 5667 arrondi.
        Long exitCost = valuationService.recordExit(item, depot, level, new BigDecimal("150"), false);

        assertEquals(5667L, exitCost);
    }

    @Test
    void fifoExhaustsLayersInOrderAndLeavesTheRemainder() {
        InventoryItem item = newItem(ValuationMethod.FIFO);
        StockLevel level = newLevel(item, depot);

        valuationService.recordEntry(item, depot, level, BigDecimal.ZERO, new BigDecimal("100"), 5000L, "MOV-1", null);
        valuationService.recordEntry(item, depot, level, new BigDecimal("100"), new BigDecimal("100"), 7000L, "MOV-2", null);
        valuationService.recordExit(item, depot, level, new BigDecimal("120"), false);

        List<StockCostLayer> remaining = costLayerRepository.findConsumableReadOnly(item, depot);

        assertEquals(1, remaining.size(), "La premiere couche doit etre epuisee");
        assertEquals(0, new BigDecimal("80").compareTo(remaining.get(0).getRemainingQuantity()));
        assertEquals(7000L, remaining.get(0).getUnitCost());
    }

    /**
     * Le point le plus risque du lot : un transfert doit deplacer la valeur sans la modifier, et
     * sans rajeunir le stock.
     */
    @Test
    void fifoTransferCarriesTheLayersWithTheirCostAndTheirDate() {
        InventoryItem item = newItem(ValuationMethod.FIFO);
        StockLevel fromLevel = newLevel(item, depot);
        StockLevel toLevel = newLevel(item, site);

        valuationService.recordEntry(item, depot, fromLevel, BigDecimal.ZERO, new BigDecimal("100"), 5000L, "MOV-1", null);
        valuationService.recordEntry(item, depot, fromLevel, new BigDecimal("100"), new BigDecimal("100"), 7000L, "MOV-2", null);

        StockCostLayer oldest = costLayerRepository.findConsumableReadOnly(item, depot).get(0);

        valuationService.recordTransfer(item, depot, site, fromLevel, toLevel, new BigDecimal("100"), "MOV-3", false);

        List<StockCostLayer> atSite = costLayerRepository.findConsumableReadOnly(item, site);
        assertEquals(1, atSite.size());
        assertEquals(5000L, atSite.get(0).getUnitCost(), "Le cout suit la marchandise");
        assertEquals(oldest.getReceivedAt(), atSite.get(0).getReceivedAt(),
                "Un transfert ne doit pas rajeunir le stock");

        List<StockCostLayer> atDepot = costLayerRepository.findConsumableReadOnly(item, depot);
        assertEquals(1, atDepot.size());
        assertEquals(7000L, atDepot.get(0).getUnitCost());
    }

    @Test
    void fifoTransferOnlyMovesWhatItTookFromAPartiallyConsumedLayer() {
        InventoryItem item = newItem(ValuationMethod.FIFO);
        StockLevel fromLevel = newLevel(item, depot);
        StockLevel toLevel = newLevel(item, site);

        valuationService.recordEntry(item, depot, fromLevel, BigDecimal.ZERO, new BigDecimal("100"), 5000L, "MOV-1", null);
        valuationService.recordExit(item, depot, fromLevel, new BigDecimal("30"), false);
        valuationService.recordTransfer(item, depot, site, fromLevel, toLevel, new BigDecimal("20"), "MOV-3", false);

        List<StockCostLayer> atSite = costLayerRepository.findConsumableReadOnly(item, site);
        assertEquals(1, atSite.size());
        // Seules les 20 unites transferees doivent partir, pas les 30 deja sorties auparavant.
        assertEquals(0, new BigDecimal("20").compareTo(atSite.get(0).getRemainingQuantity()));
    }

    @Test
    void switchingToFifoSeedsOneLayerPerLocationHoldingStock() {
        InventoryItem item = newItem(null);
        StockLevel depotLevel = newLevel(item, depot);
        depotLevel.setQuantityOnHand(new BigDecimal("40"));
        depotLevel.setAverageCost(6000L);
        stockLevelRepository.saveAndFlush(depotLevel);

        int seeded = valuationService.switchValuationMethod(item, ValuationMethod.FIFO);

        assertEquals(1, seeded);
        List<StockCostLayer> layers = costLayerRepository.findConsumableReadOnly(item, depot);
        assertEquals(1, layers.size());
        assertEquals(6000L, layers.get(0).getUnitCost());
        assertTrue(layers.get(0).getSeeded(), "Une couche d'amorcage doit se distinguer d'un vrai cout d'achat");
    }

    @Test
    void switchingBackToWeightedAverageSeedsNothing() {
        InventoryItem item = newItem(ValuationMethod.FIFO);

        assertEquals(0, valuationService.switchValuationMethod(item, ValuationMethod.WEIGHTED_AVERAGE));
        assertEquals(ValuationMethod.WEIGHTED_AVERAGE, item.effectiveValuationMethod());
    }

    private InventoryItem newItem(ValuationMethod method) {
        long id = SEQUENCE.incrementAndGet();
        jdbc.update("""
                INSERT INTO inventory_item
                    (id, item_code, name, item_type, tracking_type, lifecycle_status, valuation_method,
                     taxable, allow_negative_stock, requires_expiry_date, requires_lot_number,
                     requires_serial_number, stackable, active, created_at, updated_at)
                VALUES (?, ?, 'Article valorisation', 'CONSUMABLE', 'QUANTITY', 'ACTIVE', ?,
                        FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, now(), now())
                """, id, "INV-VAL-" + id, method == null ? null : method.name());
        return itemRepository.findById(id).orElseThrow();
    }

    private InventoryLocation newLocation(String prefix) {
        long id = SEQUENCE.incrementAndGet();
        jdbc.update("""
                INSERT INTO inventory_location
                    (id, location_code, name, location_type, active, created_at, updated_at)
                VALUES (?, ?, 'Emplacement valorisation', 'WAREHOUSE', TRUE, now(), now())
                """, id, "LOC-VAL-" + prefix + "-" + id);
        return locationRepository.findById(id).orElseThrow();
    }

    private StockLevel newLevel(InventoryItem item, InventoryLocation location) {
        long id = SEQUENCE.incrementAndGet();
        jdbc.update("""
                INSERT INTO stock_level
                    (id, item_id, location_id, quantity_on_hand, quantity_reserved, quantity_available,
                     version, created_at, updated_at)
                VALUES (?, ?, ?, 0, 0, 0, 0, now(), now())
                """, id, item.getId(), location.getId());
        return stockLevelRepository.findById(id).orElseThrow();
    }
}
