package com.sni.bokaticowork.features.inventory.stock;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryLocationRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie que le verrou pessimiste de {@code findByItemAndLocationForUpdate} tient sous charge.
 *
 * <p>C'etait le dernier controle du lot 0 reste manuel. Sans ce verrou, deux sorties simultanees
 * sur le meme couple article et emplacement lisent la meme quantite, en soustraient chacune la
 * leur, et la derniere ecriture ecrase la premiere : le stock est faux sans qu'aucune erreur ne
 * soit levee.</p>
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockLevelConcurrencyIT extends PostgresIntegrationTestBase {

    private static final AtomicLong SEQUENCE = new AtomicLong(9_970_000L);
    private static final int THREADS = 16;
    private static final int DECREMENTS_PER_THREAD = 5;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private StockLevelRepository stockLevelRepository;
    @Autowired
    private InventoryItemRepository itemRepository;
    @Autowired
    private InventoryLocationRepository locationRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;
    private InventoryItem item;
    private InventoryLocation location;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        long itemId = SEQUENCE.incrementAndGet();
        long locationId = SEQUENCE.incrementAndGet();
        String itemCode = "INV-CONC-" + itemId;
        String locationCode = "LOC-CONC-" + locationId;

        jdbc.update("""
                INSERT INTO inventory_location
                    (id, location_code, name, location_type, active, created_at, updated_at)
                VALUES (?, ?, 'Emplacement concurrence', 'WAREHOUSE', TRUE, now(), now())
                """, locationId, locationCode);
        jdbc.update("""
                INSERT INTO inventory_item
                    (id, item_code, name, item_type, tracking_type, lifecycle_status,
                     taxable, allow_negative_stock, requires_expiry_date, requires_lot_number,
                     requires_serial_number, stackable, active, created_at, updated_at)
                VALUES (?, ?, 'Article concurrence', 'CONSUMABLE', 'QUANTITY', 'ACTIVE',
                        FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, now(), now())
                """, itemId, itemCode);
        jdbc.update("""
                INSERT INTO stock_level
                    (id, item_id, location_id, quantity_on_hand, quantity_reserved, quantity_available,
                     version, created_at, updated_at)
                VALUES (?, ?, ?, 1000, 0, 1000, 0, now(), now())
                """, SEQUENCE.incrementAndGet(), itemId, locationId);

        item = itemRepository.findByItemCode(itemCode).orElseThrow();
        location = locationRepository.findByLocationCode(locationCode).orElseThrow();
    }

    @Test
    void concurrentDecrementsThroughTheLockedFinderLoseNothing() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger failures = new AtomicInteger();
        List<String> errors = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int thread = 0; thread < THREADS; thread++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < DECREMENTS_PER_THREAD; i++) {
                            transactionTemplate.executeWithoutResult(status -> {
                                StockLevel level = stockLevelRepository
                                        .findByItemAndLocationForUpdate(item, location)
                                        .orElseThrow();
                                level.setQuantityOnHand(level.getQuantityOnHand().subtract(BigDecimal.ONE));
                                level.recalculateAvailable();
                                stockLevelRepository.save(level);
                            });
                        }
                    } catch (Exception exception) {
                        failures.incrementAndGet();
                        synchronized (errors) {
                            errors.add(String.valueOf(exception.getMessage()));
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(120, TimeUnit.SECONDS), "Les threads n'ont pas termine a temps");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, failures.get(), "Echecs concurrents : " + errors);

        BigDecimal expected = new BigDecimal(1000 - THREADS * DECREMENTS_PER_THREAD);
        BigDecimal onHand = jdbc.queryForObject(
                "SELECT quantity_on_hand FROM stock_level WHERE item_id = ? AND location_id = ?",
                BigDecimal.class, item.getId(), location.getId());

        // 80 decrements concurrents doivent en retirer exactement 80.
        assertEquals(0, expected.compareTo(onHand),
                "Attendu " + expected + ", obtenu " + onHand + " : des ecritures se sont ecrasees");
    }

    @Test
    void theAvailableQuantityStaysConsistentWithTheQuantityOnHand() throws Exception {
        concurrentDecrementsThroughTheLockedFinderLoseNothing();

        BigDecimal onHand = jdbc.queryForObject(
                "SELECT quantity_on_hand FROM stock_level WHERE item_id = ? AND location_id = ?",
                BigDecimal.class, item.getId(), location.getId());
        BigDecimal available = jdbc.queryForObject(
                "SELECT quantity_available FROM stock_level WHERE item_id = ? AND location_id = ?",
                BigDecimal.class, item.getId(), location.getId());

        assertEquals(0, onHand.compareTo(available));
    }
}
