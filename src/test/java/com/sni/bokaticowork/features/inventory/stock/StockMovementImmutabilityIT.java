package com.sni.bokaticowork.features.inventory.stock;

import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie les declencheurs d'immuabilite du journal de mouvements, poses par la migration V216.
 *
 * <p>Ces garanties vivent dans la base : aucun test Mockito ne peut les couvrir. C'est exactement
 * le trou que le socle Testcontainers vient combler, et qui restait en procedure manuelle.</p>
 *
 * <p>La transaction du test est desactivee : chaque instruction doit etre validee pour que le
 * declencheur agisse reellement. Chaque test travaille donc sur son propre mouvement, puisque rien
 * n'est annule en fin de test et qu'un mouvement, par construction, ne se supprime pas.</p>
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockMovementImmutabilityIT extends PostgresIntegrationTestBase {

    private static final AtomicLong SEQUENCE = new AtomicLong(9_990_000L);

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        seedReferenceData();
    }

    @Test
    void theThreeTriggersAreInstalled() {
        Integer installed = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_trigger
                WHERE NOT tgisinternal
                  AND tgname IN ('trg_stock_movement_immutable',
                                 'trg_stock_movement_no_delete',
                                 'trg_stock_movement_lot_immutable')
                """, Integer.class);

        assertEquals(3, installed);
    }

    @Test
    void everyBusinessColumnIsFrozenAfterInsertion() {
        String code = newMovement();

        for (String statement : new String[]{
                "UPDATE stock_movement SET quantity = quantity + 1 WHERE movement_code = ?",
                "UPDATE stock_movement SET unit_cost = 999 WHERE movement_code = ?",
                "UPDATE stock_movement SET total_cost = 999 WHERE movement_code = ?",
                "UPDATE stock_movement SET movement_type = 'OUT' WHERE movement_code = ?",
                "UPDATE stock_movement SET performed_at = now() WHERE movement_code = ?",
                "UPDATE stock_movement SET allow_negative_override = TRUE WHERE movement_code = ?",
                "UPDATE stock_movement SET reference_code = 'HACK' WHERE movement_code = ?",
                "UPDATE stock_movement SET movement_code = 'HACKED' WHERE movement_code = ?"}) {

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> jdbc.update(statement, code), statement);
            assertTrue(messageChain(error).contains("immuable"), statement + " : " + messageChain(error));
        }
    }

    @Test
    void deletingAMovementIsRefused() {
        String code = newMovement();

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                jdbc.update("DELETE FROM stock_movement WHERE movement_code = ?", code));

        assertTrue(messageChain(error).contains("interdite"), messageChain(error));
    }

    @Test
    void theReversalColumnsRemainWritable() {
        // Le controle le plus important du lot 0 : un declencheur trop strict casserait
        // reverseMovement(), qui renseigne ces colonnes sur le mouvement d'origine.
        String code = newMovement();

        int updated = jdbc.update("""
                UPDATE stock_movement
                SET reversed = TRUE,
                    reversed_at = now(),
                    reversed_by = 'integration-test',
                    reversal_reason = 'Verification du declencheur'
                WHERE movement_code = ?
                """, code);

        assertEquals(1, updated);
    }

    @Test
    void unwindingAnAlreadyReversedMovementIsRefused() {
        String code = newMovement();
        jdbc.update("UPDATE stock_movement SET reversed = TRUE WHERE movement_code = ?", code);

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                jdbc.update("UPDATE stock_movement SET reversed = FALSE WHERE movement_code = ?", code));

        assertTrue(messageChain(error).contains("contre-passe"), messageChain(error));
    }

    /**
     * Le rattachement de contre-passation doit pouvoir etre pose une fois, puisque
     * {@code reverseMovement()} le renseigne juste apres l'insertion du mouvement compensatoire,
     * puis rester fige.
     */
    @Test
    void theReversalLinkCanBeSetOnceThenBecomesFrozen() {
        String code = newMovement();
        Long id = jdbc.queryForObject("SELECT id FROM stock_movement WHERE movement_code = ?", Long.class, code);

        assertEquals(1, jdbc.update(
                "UPDATE stock_movement SET reversal_of_movement_id = ? WHERE movement_code = ?", id, code));

        RuntimeException error = assertThrows(RuntimeException.class, () -> jdbc.update(
                "UPDATE stock_movement SET reversal_of_movement_id = NULL WHERE movement_code = ?", code));

        assertTrue(messageChain(error).contains("fige"), messageChain(error));
    }

    @Test
    void theLotBreakdownOfAMovementIsAlsoFrozen() {
        String code = newMovement();
        Long movementId = jdbc.queryForObject(
                "SELECT id FROM stock_movement WHERE movement_code = ?", Long.class, code);

        long lotId = SEQUENCE.incrementAndGet();
        jdbc.update("""
                INSERT INTO stock_movement_lot (id, movement_id, lot_number, quantity)
                VALUES (?, ?, 'LOT-IT', 5)
                """, lotId, movementId);

        RuntimeException onUpdate = assertThrows(RuntimeException.class, () ->
                jdbc.update("UPDATE stock_movement_lot SET quantity = 6 WHERE id = ?", lotId));
        assertTrue(messageChain(onUpdate).contains("immuable"), messageChain(onUpdate));

        RuntimeException onDelete = assertThrows(RuntimeException.class, () ->
                jdbc.update("DELETE FROM stock_movement_lot WHERE id = ?", lotId));
        assertTrue(messageChain(onDelete).contains("immuable"), messageChain(onDelete));
    }

    /**
     * Cree un mouvement dedie au test courant. Chaque test a le sien, faute de pouvoir nettoyer.
     */
    private String newMovement() {
        long id = SEQUENCE.incrementAndGet();
        String code = "MOV-IT-" + id;
        jdbc.update("""
                INSERT INTO stock_movement
                    (id, movement_code, item_id, location_to_id, movement_type, quantity,
                     allow_negative_override, reversed, performed_at)
                VALUES (?, ?, ?, ?, 'IN', 10, FALSE, FALSE, now())
                """, id, code, itemId(), locationId());
        return code;
    }

    private static final long ITEM_ID = 9_900_001L;
    private static final long LOCATION_ID = 9_900_002L;

    private long itemId() {
        return ITEM_ID;
    }

    private long locationId() {
        return LOCATION_ID;
    }

    private void seedReferenceData() {
        jdbc.update("""
                INSERT INTO inventory_location
                    (id, location_code, name, location_type, active, created_at, updated_at)
                VALUES (?, 'LOC-IT-MAIN', 'Depot de test', 'WAREHOUSE', TRUE, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, LOCATION_ID);

        jdbc.update("""
                INSERT INTO inventory_item
                    (id, item_code, name, item_type, tracking_type, lifecycle_status,
                     taxable, allow_negative_stock, requires_expiry_date, requires_lot_number,
                     requires_serial_number, stackable, active, created_at, updated_at)
                VALUES (?, 'INV-IT-0001', 'Article de test', 'CONSUMABLE', 'QUANTITY', 'ACTIVE',
                        FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, ITEM_ID);
    }

    /**
     * Les messages PostgreSQL remontent enveloppes dans plusieurs exceptions Spring.
     */
    private String messageChain(Throwable error) {
        StringBuilder message = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                message.append(current.getMessage()).append(' ');
            }
        }
        return message.toString();
    }
}
