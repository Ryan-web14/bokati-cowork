package com.sni.bokaticowork.features.inventory.stock;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.stock.dto.request.QualityControlPlanRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.QualityInspectionRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.QualityControlPlanResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.QualityInspectionResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityControlStage;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityDecision;
import com.sni.bokaticowork.features.inventory.stock.enums.QualitySamplingMode;
import com.sni.bokaticowork.features.inventory.stock.mapper.decorator.StockLotResponseFactory;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.implementation.QualityControlServiceImpl;
import com.sni.bokaticowork.features.inventory.stock.service.implementation.StockQuarantineServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie qu une inspection en echec immobilise le lot et ouvre une non-conformite, et qu une
 * derogation reste possible mais engage quelqu un.
 */
@Import({QualityControlServiceImpl.class, StockQuarantineServiceImpl.class, StockLotResponseFactory.class})
class QualityControlServiceIT extends PostgresIntegrationTestBase {

    private static final AtomicLong SEQUENCE = new AtomicLong(9_910_000L);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private QualityControlServiceImpl qualityService;
    @Autowired
    private StockLotRepository lotRepository;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    /**
     * Le decoupage DataJpaTest ne charge pas les services du catalogue ni le moteur de sequences.
     * On les remplace par des doublures, l objet du test etant la logique de controle qualite.
     */
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService itemLookupService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService categoryService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade sequenceGenerator;

    @Autowired
    private com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository itemRepository;

    private JdbcTemplate jdbc;
    private String itemCode;
    private long lotId;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        long itemId = SEQUENCE.incrementAndGet();
        long locationId = SEQUENCE.incrementAndGet();
        lotId = SEQUENCE.incrementAndGet();
        itemCode = "INV-QC-" + itemId;

        jdbc.update("""
                INSERT INTO inventory_location (id, location_code, name, location_type, active, created_at, updated_at)
                VALUES (?, ?, 'Depot qualite', 'WAREHOUSE', TRUE, now(), now())
                """, locationId, "LOC-QC-" + locationId);
        jdbc.update("""
                INSERT INTO inventory_item
                    (id, item_code, name, item_type, tracking_type, lifecycle_status, taxable,
                     allow_negative_stock, requires_expiry_date, requires_lot_number,
                     requires_serial_number, stackable, active, created_at, updated_at)
                VALUES (?, ?, 'Ciment controle', 'CONSUMABLE', 'LOT', 'ACTIVE', FALSE,
                        FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, now(), now())
                """, itemId, itemCode);
        jdbc.update("""
                INSERT INTO stock_level
                    (id, item_id, location_id, quantity_on_hand, quantity_reserved, quantity_available,
                     quantity_quarantined, version, created_at, updated_at)
                VALUES (?, ?, ?, 200, 0, 200, 0, 0, now(), now())
                """, SEQUENCE.incrementAndGet(), itemId, locationId);
        jdbc.update("""
                INSERT INTO stock_lot
                    (id, item_id, location_id, lot_number, initial_quantity, remaining_quantity,
                     received_at, active, quarantined, blocked)
                VALUES (?, ?, ?, 'LOT-QC-1', 200, 200, now(), TRUE, FALSE, FALSE)
                """, lotId, itemId, locationId);

        org.mockito.Mockito.when(itemLookupService.findByItemCodeOrThrow(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(itemRepository.findById(itemId).orElseThrow());
        org.mockito.Mockito.when(sequenceGenerator.next(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(java.time.LocalDate.class)))
                .thenAnswer(call -> call.getArgument(0) + "-" + SEQUENCE.incrementAndGet());
    }

    @Test
    void aConformInspectionLeavesTheLotAvailable() {
        createPlan(QualityDecision.QUARANTINED);

        QualityInspectionResponse response = qualityService.inspect(lotId, measure(new BigDecimal("25"), true));

        assertEquals(QualityDecision.ACCEPTED, response.getDecision());
        assertFalse(response.isLotQuarantined());
        assertNull(response.getNonConformanceCode());
    }

    @Test
    void aBlockingFailureQuarantinesTheLotAndOpensANonConformance() {
        createPlan(QualityDecision.QUARANTINED);

        // La resistance mesuree tombe sous la borne basse du critere bloquant.
        QualityInspectionResponse response = qualityService.inspect(lotId, measure(new BigDecimal("12"), true));

        assertEquals(QualityDecision.QUARANTINED, response.getDecision());
        assertTrue(response.isLotQuarantined());
        assertNotNull(response.getNonConformanceCode());
        entityManager.flush();
        assertTrue(lotRepository.findById(lotId).orElseThrow().getQuarantined());
    }

    @Test
    void aFailingPlanCanRejectOutrightRatherThanQuarantine() {
        createPlan(QualityDecision.REJECTED);

        QualityInspectionResponse response = qualityService.inspect(lotId, measure(new BigDecimal("12"), true));

        assertEquals(QualityDecision.REJECTED, response.getDecision());
        assertTrue(response.isLotQuarantined(), "Un lot refuse doit aussi sortir du stock disponible");
    }

    @Test
    void aNonBlockingCriterionDoesNotCondemnTheLot() {
        createPlan(QualityDecision.QUARANTINED);

        // L aspect visuel est declare non bloquant : son echec se note sans condamner le lot.
        QualityInspectionResponse response = qualityService.inspect(lotId, measure(new BigDecimal("25"), false));

        assertEquals(QualityDecision.ACCEPTED, response.getDecision());
        assertFalse(response.isLotQuarantined());
    }

    @Test
    void aDerogationRequiresBothAnAuthorAndAReason() {
        createPlan(QualityDecision.QUARANTINED);

        QualityInspectionRequest request = measure(new BigDecimal("12"), true);
        request.setAcceptByDerogation(true);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> qualityService.inspect(lotId, request));

        assertTrue(error.getMessage().contains("decisionBy"));
    }

    @Test
    void aProperlyJustifiedDerogationKeepsTheLotAvailable() {
        createPlan(QualityDecision.QUARANTINED);

        QualityInspectionRequest request = measure(new BigDecimal("12"), true);
        request.setAcceptByDerogation(true);
        request.setDecisionBy("responsable-qualite");
        request.setDecisionReason("Usage non structurel accepte par le client");

        QualityInspectionResponse response = qualityService.inspect(lotId, request);

        assertEquals(QualityDecision.ACCEPTED_BY_DEROGATION, response.getDecision());
        assertFalse(response.isLotQuarantined());
        // La derogation reste une non-conformite : elle a ete constatee, seulement acceptee.
        assertNotNull(response.getNonConformanceCode());
    }

    @Test
    void aMeasureOutsideThePlanIsRecordedWithoutFailingTheInspection() {
        createPlan(QualityDecision.QUARANTINED);

        QualityInspectionRequest request = new QualityInspectionRequest();
        QualityInspectionRequest.Measure stray = new QualityInspectionRequest.Measure();
        stray.setCriterionCode("HUMIDITE");
        stray.setMeasuredValue(new BigDecimal("3"));
        request.setMeasures(List.of(stray));
        request.setInspectedBy("controleur");

        QualityInspectionResponse response = qualityService.inspect(lotId, request);

        assertEquals(QualityDecision.ACCEPTED, response.getDecision());
        assertEquals(1, response.getResults().size());
        assertFalse(response.getResults().get(0).getBlocking());
    }

    @Test
    void aReceiptPlanQuarantinesTheLotAutomatically() {
        QualityControlPlanRequest request = planRequest(QualityDecision.QUARANTINED);
        request.setQuarantineOnReceipt(true);
        qualityService.createPlan(request);

        boolean quarantined = qualityService.applyReceiptPlan(lotRepository.findById(lotId).orElseThrow());

        assertTrue(quarantined);
    }

    @Test
    void withoutAnyPlanTheReceiptLeavesTheLotAvailable() {
        // C est la promesse de non-regression : sans plan declare, rien ne change.
        boolean quarantined = qualityService.applyReceiptPlan(lotRepository.findById(lotId).orElseThrow());

        assertFalse(quarantined);
    }

    private QualityControlPlanResponse createPlan(QualityDecision decisionOnFail) {
        return qualityService.createPlan(planRequest(decisionOnFail));
    }

    private QualityControlPlanRequest planRequest(QualityDecision decisionOnFail) {
        QualityControlPlanRequest request = new QualityControlPlanRequest();
        request.setName("Controle ciment");
        request.setItemCode(itemCode);
        request.setControlStage(QualityControlStage.ON_RECEIPT);
        request.setSamplingMode(QualitySamplingMode.FIXED_QUANTITY);
        request.setSamplingParameter(new BigDecimal("5"));
        request.setDecisionOnFail(decisionOnFail);

        QualityControlPlanRequest.Criterion resistance = new QualityControlPlanRequest.Criterion();
        resistance.setCriterionCode("RESISTANCE");
        resistance.setName("Resistance a la compression");
        resistance.setUnit("MPa");
        resistance.setMinValue(new BigDecimal("20"));
        resistance.setBlocking(true);

        QualityControlPlanRequest.Criterion aspect = new QualityControlPlanRequest.Criterion();
        aspect.setCriterionCode("ASPECT");
        aspect.setName("Aspect du sac");
        aspect.setBooleanExpected(true);
        aspect.setBlocking(false);

        request.setCriteria(List.of(resistance, aspect));
        return request;
    }

    private QualityInspectionRequest measure(BigDecimal resistance, boolean aspectOk) {
        QualityInspectionRequest request = new QualityInspectionRequest();

        QualityInspectionRequest.Measure resistanceMeasure = new QualityInspectionRequest.Measure();
        resistanceMeasure.setCriterionCode("RESISTANCE");
        resistanceMeasure.setMeasuredValue(resistance);

        QualityInspectionRequest.Measure aspectMeasure = new QualityInspectionRequest.Measure();
        aspectMeasure.setCriterionCode("ASPECT");
        aspectMeasure.setMeasuredFlag(aspectOk);

        request.setMeasures(List.of(resistanceMeasure, aspectMeasure));
        request.setInspectedBy("controleur");
        request.setSampledQuantity(new BigDecimal("5"));
        return request;
    }
}
