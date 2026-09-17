package com.sni.bokaticowork.features.inventory.stock;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotBlockRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotQuarantineRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotReleaseRequest;
import com.sni.bokaticowork.features.inventory.stock.enums.LotBlockReasonType;
import com.sni.bokaticowork.features.inventory.stock.mapper.decorator.StockLotResponseFactory;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.implementation.StockQuarantineServiceImpl;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie que l immobilisation d un lot retire bien sa quantite du stock disponible, et que la
 * levee de quarantaine exige un second regard.
 */
@Import({StockQuarantineServiceImpl.class, StockLotResponseFactory.class})
class StockQuarantineServiceIT extends PostgresIntegrationTestBase {

    private static final AtomicLong SEQUENCE = new AtomicLong(9_930_000L);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private StockQuarantineServiceImpl quarantineService;
    @Autowired
    private StockLotRepository lotRepository;
    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private JdbcTemplate jdbc;
    private long itemId;
    private long locationId;
    private long levelId;
    private StockLot lot;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        itemId = SEQUENCE.incrementAndGet();
        locationId = SEQUENCE.incrementAndGet();
        levelId = SEQUENCE.incrementAndGet();
        long lotId = SEQUENCE.incrementAndGet();

        jdbc.update("""
                INSERT INTO inventory_location (id, location_code, name, location_type, active, created_at, updated_at)
                VALUES (?, ?, 'Depot quarantaine', 'WAREHOUSE', TRUE, now(), now())
                """, locationId, "LOC-QUA-" + locationId);
        jdbc.update("""
                INSERT INTO inventory_item
                    (id, item_code, name, item_type, tracking_type, lifecycle_status, taxable,
                     allow_negative_stock, requires_expiry_date, requires_lot_number,
                     requires_serial_number, stackable, active, created_at, updated_at)
                VALUES (?, ?, 'Article quarantaine', 'CONSUMABLE', 'LOT', 'ACTIVE', FALSE,
                        FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, now(), now())
                """, itemId, "INV-QUA-" + itemId);
        jdbc.update("""
                INSERT INTO stock_level
                    (id, item_id, location_id, quantity_on_hand, quantity_reserved, quantity_available,
                     quantity_quarantined, version, created_at, updated_at)
                VALUES (?, ?, ?, 100, 0, 100, 0, 0, now(), now())
                """, levelId, itemId, locationId);
        jdbc.update("""
                INSERT INTO stock_lot
                    (id, item_id, location_id, lot_number, initial_quantity, remaining_quantity,
                     received_at, active, quarantined, blocked)
                VALUES (?, ?, ?, 'LOT-QUA-1', 40, 40, now(), TRUE, FALSE, FALSE)
                """, lotId, itemId, locationId);

        lot = lotRepository.findById(lotId).orElseThrow();
    }

    @Test
    void quarantiningALotRemovesItsQuantityFromTheAvailableStock() {
        quarantineService.quarantine(lot.getId(), quarantineRequest("Controle qualite en attente"));

        assertEquals(0, new BigDecimal("40").compareTo(quarantinedQuantity()));
        // 100 en stock, 40 immobilises : il reste 60 disponibles.
        assertEquals(0, new BigDecimal("60").compareTo(availableQuantity()));
    }

    @Test
    void releasingTheLotGivesTheQuantityBack() {
        quarantineService.quarantine(lot.getId(), quarantineRequest("Controle qualite en attente"));

        LotReleaseRequest release = new LotReleaseRequest();
        release.setRequestedBy("magasinier");
        release.setApprovedBy("responsable-qualite");
        quarantineService.release(lot.getId(), release);

        assertEquals(0, BigDecimal.ZERO.compareTo(quarantinedQuantity()));
        assertEquals(0, new BigDecimal("100").compareTo(availableQuantity()));
    }

    @Test
    void releasingRequiresAnApproverDifferentFromTheRequester() {
        quarantineService.quarantine(lot.getId(), quarantineRequest("Non conforme"));

        LotReleaseRequest release = new LotReleaseRequest();
        release.setRequestedBy("magasinier");
        release.setApprovedBy("Magasinier");

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> quarantineService.release(lot.getId(), release));

        assertTrue(error.getMessage().contains("different from the requester"));
    }

    @Test
    void blockingIsIndependentFromQuarantineButAlsoImmobilises() {
        LotBlockRequest block = new LotBlockRequest();
        block.setReasonType(LotBlockReasonType.SUPPLIER_DISPUTE);
        block.setReason("Litige sur la facture");
        block.setBlockedBy("responsable-achats");

        quarantineService.block(lot.getId(), block);

        assertEquals(0, new BigDecimal("40").compareTo(quarantinedQuantity()));
        assertFalse(lotRepository.findById(lot.getId()).orElseThrow().getQuarantined(),
                "Un blocage administratif ne doit pas se confondre avec une quarantaine qualite");
    }

    @Test
    void unblockingRestoresTheAvailableQuantity() {
        LotBlockRequest block = new LotBlockRequest();
        block.setReasonType(LotBlockReasonType.ADMINISTRATIVE);
        block.setReason("En attente de decision");
        quarantineService.block(lot.getId(), block);

        quarantineService.unblock(lot.getId(), "direction");

        assertEquals(0, BigDecimal.ZERO.compareTo(quarantinedQuantity()));
        assertEquals(0, new BigDecimal("100").compareTo(availableQuantity()));
    }

    @Test
    void aLotCannotBeQuarantinedTwice() {
        quarantineService.quarantine(lot.getId(), quarantineRequest("Premier controle"));

        assertThrows(BadRequestException.class,
                () -> quarantineService.quarantine(lot.getId(), quarantineRequest("Second controle")));
    }

    @Test
    void anImmobilisedLotIsListedAsSuch() {
        quarantineService.quarantine(lot.getId(), quarantineRequest("Controle"));

        assertEquals(1, quarantineService.listImmobilised("INV-QUA-" + itemId, null).size());
    }

    private LotQuarantineRequest quarantineRequest(String reason) {
        LotQuarantineRequest request = new LotQuarantineRequest();
        request.setReason(reason);
        request.setQuarantinedBy("controleur");
        return request;
    }

    /**
     * Vide le contexte de persistance avant de relire en SQL : sans cela, la lecture verrait l etat
     * d avant l operation, les ecritures JPA n etant pas encore parvenues a la base.
     */
    private void flush() {
        entityManager.flush();
    }

    private BigDecimal quarantinedQuantity() {
        flush();
        return jdbc.queryForObject(
                "SELECT quantity_quarantined FROM stock_level WHERE id = ?", BigDecimal.class, levelId);
    }

    private BigDecimal availableQuantity() {
        flush();
        return jdbc.queryForObject(
                "SELECT quantity_available FROM stock_level WHERE id = ?", BigDecimal.class, levelId);
    }
}
