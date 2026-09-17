package com.sni.bokaticowork.features.inventory.stock;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.StockJournalDirection;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.model.AdjustmentReason;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockJournalEntry;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.repository.AdjustmentReasonRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockJournalEntryRepository;
import com.sni.bokaticowork.features.inventory.stock.service.implementation.StockAccountingServiceImpl;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockAccountingServiceTest {

    @Mock
    private StockJournalEntryRepository journalRepository;
    @Mock
    private AdjustmentReasonRepository adjustmentReasonRepository;
    @Mock
    private InventoryPeriodService periodService;

    @InjectMocks
    private StockAccountingServiceImpl service;

    private InventoryItem item;
    private InventoryLocation location;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "stockAccount", "31");
        ReflectionTestUtils.setField(service, "purchaseAccount", "601");
        ReflectionTestUtils.setField(service, "consumptionAccount", "6031");
        ReflectionTestUtils.setField(service, "lossAccount", "6581");
        ReflectionTestUtils.setField(service, "defaultCounterpartAccount", "6031");

        item = InventoryItem.builder().itemCode("ART-1").name("Ciment").build();
        location = InventoryLocation.builder().locationCode("LOC-A").name("Depot").build();

        when(journalRepository.existsByMovementCode(anyString())).thenReturn(false);
        when(journalRepository.save(any(StockJournalEntry.class))).thenAnswer(call -> call.getArgument(0));
        when(periodService.periodCodeFor(any())).thenReturn("2026-09");
    }

    @Test
    void anEntryDebitsTheStockAccount() {
        service.recordMovement(movement(StockMovementType.IN, 600_000L, null, StockReferenceType.GOODS_RECEIPT));

        StockJournalEntry entry = captureEntry();
        assertEquals(StockJournalDirection.DEBIT, entry.getDirection());
        assertEquals("31", entry.getStockAccount());
        assertEquals(600_000L, entry.getAmount());
    }

    @Test
    void anEntryFromAGoodsReceiptUsesThePurchaseAccount() {
        service.recordMovement(movement(StockMovementType.IN, 600_000L, null, StockReferenceType.GOODS_RECEIPT));

        assertEquals("601", captureEntry().getCounterpartAccount());
    }

    @Test
    void anExitCreditsTheStockAccount() {
        service.recordMovement(movement(StockMovementType.OUT, 300_000L, StockOutReasonCode.CONSUMPTION, null));

        StockJournalEntry entry = captureEntry();
        assertEquals(StockJournalDirection.CREDIT, entry.getDirection());
        assertEquals("6031", entry.getCounterpartAccount());
    }

    @Test
    void aLossUsesTheLossAccountRatherThanConsumption() {
        service.recordMovement(movement(StockMovementType.OUT, 50_000L, StockOutReasonCode.LOSS, null));

        assertEquals("6581", captureEntry().getCounterpartAccount());
    }

    @Test
    void acodifiedAdjustmentReasonOverridesTheDefaultAccount() {
        when(adjustmentReasonRepository.findByReasonCode("THEFT")).thenReturn(Optional.of(
                AdjustmentReason.builder().reasonCode("THEFT").counterpartAccount("6588").build()));

        StockMovement movement = movement(StockMovementType.ADJUSTMENT_OUT, 20_000L, null, null);
        movement.setAdjustmentReasonCode("THEFT");

        service.recordMovement(movement);

        assertEquals("6588", captureEntry().getCounterpartAccount());
    }

    @Test
    void aTransferProducesNoEntryAtAll() {
        // Deplacer du stock d un emplacement a un autre ne change pas la valeur du patrimoine.
        service.recordMovement(movement(StockMovementType.TRANSFER, 100_000L, null, null));

        verify(journalRepository, never()).save(any());
    }

    @Test
    void anUnvaluedMovementProducesNoEntry() {
        StockMovement movement = movement(StockMovementType.OUT, 0L, null, null);
        movement.setTotalCost(null);

        service.recordMovement(movement);

        verify(journalRepository, never()).save(any());
    }

    @Test
    void aMovementAlreadyPostedIsNotPostedTwice() {
        when(journalRepository.existsByMovementCode("MOV-1")).thenReturn(true);

        service.recordMovement(movement(StockMovementType.IN, 600_000L, null, null));

        verify(journalRepository, never()).save(any());
    }

    @Test
    void theCsvExportBalancesDebitAndCredit() {
        when(journalRepository.findForExport(any(), any(), any())).thenReturn(java.util.List.of(
                StockJournalEntry.builder().movementCode("MOV-1").itemCode("ART-1").locationCode("LOC-A")
                        .stockAccount("31").counterpartAccount("601").direction(StockJournalDirection.DEBIT)
                        .amount(600_000L).accountingDate(java.time.LocalDate.of(2026, 9, 1)).build(),
                StockJournalEntry.builder().movementCode("MOV-2").itemCode("ART-1").locationCode("LOC-A")
                        .stockAccount("31").counterpartAccount("6031").direction(StockJournalDirection.CREDIT)
                        .amount(300_000L).accountingDate(java.time.LocalDate.of(2026, 9, 2)).build()));

        String csv = service.exportCsv(null, null, null);

        assertEquals(true, csv.contains("TOTAL,,,,,,,600000,300000"));
    }

    private StockMovement movement(StockMovementType type, Long totalCost,
                                   StockOutReasonCode reasonCode, StockReferenceType referenceType) {
        return StockMovement.builder()
                .movementCode("MOV-1")
                .item(item)
                .locationFrom(type == StockMovementType.IN || type == StockMovementType.ADJUSTMENT_IN ? null : location)
                .locationTo(type == StockMovementType.IN || type == StockMovementType.ADJUSTMENT_IN ? location : null)
                .movementType(type)
                .quantity(new BigDecimal("100"))
                .totalCost(totalCost)
                .reasonCode(reasonCode)
                .referenceType(referenceType)
                .performedAt(Instant.parse("2026-09-01T10:00:00Z"))
                .build();
    }

    private StockJournalEntry captureEntry() {
        ArgumentCaptor<StockJournalEntry> captor = ArgumentCaptor.forClass(StockJournalEntry.class);
        verify(journalRepository).save(captor.capture());
        return captor.getValue();
    }
}
