package com.sni.bokaticowork.features.inventory.stock;

import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie la requete native de reconciliation du lot 0 contre un PostgreSQL reel.
 *
 * <p>Une requete de cette taille, avec union, agregat et jointure externe complete, ne se teste pas
 * avec un bouchon : seule la base sait si elle dit vrai.</p>
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockReconciliationQueryIT extends PostgresIntegrationTestBase {

    private static final AtomicLong SEQUENCE = new AtomicLong(9_950_000L);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    private JdbcTemplate jdbc;
    private String itemCode;
    private long itemId;
    private long locationId;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        itemId = SEQUENCE.incrementAndGet();
        locationId = SEQUENCE.incrementAndGet();
        itemCode = "INV-REC-" + itemId;
        seedItemAndLocation();
    }

    @Test
    void reportsNothingWhenTheLevelMatchesTheSumOfMovements() {
        movement("IN", 10, null, locationId);
        movement("OUT", 3, locationId, null);
        stockLevel(new BigDecimal("7"));

        assertEquals(List.of(), stockLevelRepository.findReconciliationDivergences(itemCode, null));
    }

    @Test
    void reportsTheGapWhenTheLevelHasDriftedUpward() {
        movement("IN", 10, null, locationId);
        movement("OUT", 3, locationId, null);
        stockLevel(new BigDecimal("9"));

        List<Object[]> divergences = stockLevelRepository.findReconciliationDivergences(itemCode, null);

        assertEquals(1, divergences.size());
        assertEquals(0, new BigDecimal("9").compareTo((BigDecimal) divergences.get(0)[4]));
        assertEquals(0, new BigDecimal("7").compareTo((BigDecimal) divergences.get(0)[5]));
        assertEquals(0, new BigDecimal("2").compareTo((BigDecimal) divergences.get(0)[6]));
    }

    @Test
    void countsATransferOnBothSides() {
        long otherLocationId = SEQUENCE.incrementAndGet();
        insertLocation(otherLocationId, "LOC-REC-B-" + otherLocationId);

        movement("IN", 10, null, locationId);
        movement("TRANSFER", 4, locationId, otherLocationId);

        // Source a 6, destination a 4. On ne declare que la source, la destination doit ressortir.
        stockLevel(new BigDecimal("6"));

        List<Object[]> divergences = stockLevelRepository.findReconciliationDivergences(itemCode, null);

        assertEquals(1, divergences.size());
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) divergences.get(0)[4]));
        assertEquals(0, new BigDecimal("4").compareTo((BigDecimal) divergences.get(0)[5]));
    }

    /**
     * Point de conception du lot 0 : la contre-passation cree un mouvement compensatoire distinct.
     * Exclure le mouvement d'origine de la somme compterait la correction deux fois.
     */
    @Test
    void keepsReversedMovementsInTheSumAlongsideTheirCompensation() {
        movement("IN", 10, null, locationId);
        long reversedId = movement("OUT", 4, locationId, null);
        jdbc.update("UPDATE stock_movement SET reversed = TRUE WHERE id = ?", reversedId);
        movement("ADJUSTMENT_IN", 4, null, locationId);

        stockLevel(new BigDecimal("10"));

        assertEquals(List.of(), stockLevelRepository.findReconciliationDivergences(itemCode, null));
    }

    @Test
    void reportsALevelRowThatHasNoMovementAtAll() {
        stockLevel(new BigDecimal("5"));

        List<Object[]> divergences = stockLevelRepository.findReconciliationDivergences(itemCode, null);

        assertEquals(1, divergences.size());
        assertEquals(0, new BigDecimal("5").compareTo((BigDecimal) divergences.get(0)[6]));
    }

    @Test
    void countsThePairsItExamines() {
        movement("IN", 10, null, locationId);
        stockLevel(new BigDecimal("10"));

        assertTrue(stockLevelRepository.countReconciliationPairs() > 0);
    }

    private long movement(String type, int quantity, Long from, Long to) {
        long id = SEQUENCE.incrementAndGet();
        jdbc.update("""
                INSERT INTO stock_movement
                    (id, movement_code, item_id, location_from_id, location_to_id, movement_type,
                     quantity, allow_negative_override, reversed, performed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, FALSE, now())
                """, id, "MOV-REC-" + id, itemId, from, to, type, quantity);
        return id;
    }

    private void stockLevel(BigDecimal quantityOnHand) {
        long id = SEQUENCE.incrementAndGet();
        jdbc.update("""
                INSERT INTO stock_level
                    (id, item_id, location_id, quantity_on_hand, quantity_reserved, quantity_available,
                     version, created_at, updated_at)
                VALUES (?, ?, ?, ?, 0, ?, 0, now(), now())
                """, id, itemId, locationId, quantityOnHand, quantityOnHand);
    }

    private void seedItemAndLocation() {
        insertLocation(locationId, "LOC-REC-A-" + locationId);
        jdbc.update("""
                INSERT INTO inventory_item
                    (id, item_code, name, item_type, tracking_type, lifecycle_status,
                     taxable, allow_negative_stock, requires_expiry_date, requires_lot_number,
                     requires_serial_number, stackable, active, created_at, updated_at)
                VALUES (?, ?, 'Article reconciliation', 'CONSUMABLE', 'QUANTITY', 'ACTIVE',
                        FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, now(), now())
                """, itemId, itemCode);
    }

    private void insertLocation(long id, String code) {
        jdbc.update("""
                INSERT INTO inventory_location
                    (id, location_code, name, location_type, active, created_at, updated_at)
                VALUES (?, ?, 'Emplacement reconciliation', 'WAREHOUSE', TRUE, now(), now())
                """, id, code);
    }
}
