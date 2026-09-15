package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReconciliationReportResponse;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReconciliationServiceImplTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @InjectMocks
    private StockReconciliationServiceImpl service;

    @Test
    void reportsNoDivergenceWhenLevelsMatchTheMovementJournal() {
        when(stockLevelRepository.findReconciliationDivergences(isNull(), isNull())).thenReturn(List.of());
        when(stockLevelRepository.countReconciliationPairs()).thenReturn(42L);

        StockReconciliationReportResponse report = service.reconcile(null, null);

        assertTrue(report.isConsistent());
        assertEquals(0, report.getDivergenceCount());
        assertEquals(42L, report.getPairsChecked());
        assertEquals(BigDecimal.ZERO, report.getTotalAbsoluteDifference());
        assertTrue(report.getDivergences().isEmpty());
    }

    @Test
    void sumsAbsoluteGapsSoOppositeDivergencesDoNotCancelOut() {
        when(stockLevelRepository.findReconciliationDivergences(isNull(), isNull())).thenReturn(List.<Object[]>of(
                row("ART-1", "Ciment", "LOC-A", "Depot A", "10", "4", "6", null),
                row("ART-2", "Fer", "LOC-B", "Depot B", "2", "8", "-6", null)
        ));
        when(stockLevelRepository.countReconciliationPairs()).thenReturn(2L);

        StockReconciliationReportResponse report = service.reconcile(null, null);

        assertFalse(report.isConsistent());
        assertEquals(2, report.getDivergenceCount());
        assertEquals(0, new BigDecimal("12").compareTo(report.getTotalAbsoluteDifference()));
    }

    @Test
    void normalizesFilterCodesLikeTheRestOfTheModule() {
        when(stockLevelRepository.findReconciliationDivergences(eq("ART-1"), eq("LOC-A"))).thenReturn(List.of());
        when(stockLevelRepository.countReconciliationPairs()).thenReturn(1L);

        StockReconciliationReportResponse report = service.reconcile("  art 1 ", "loc_a");

        assertEquals("ART-1", report.getItemCodeFilter());
        assertEquals("LOC-A", report.getLocationCodeFilter());
        verify(stockLevelRepository).findReconciliationDivergences("ART-1", "LOC-A");
    }

    @Test
    void treatsBlankFiltersAsNoFilter() {
        when(stockLevelRepository.findReconciliationDivergences(isNull(), isNull())).thenReturn(List.of());
        when(stockLevelRepository.countReconciliationPairs()).thenReturn(1L);

        StockReconciliationReportResponse report = service.reconcile("   ", "");

        assertNull(report.getItemCodeFilter());
        assertNull(report.getLocationCodeFilter());
    }

    @Test
    void convertsSqlTimestampsComingFromTheNativeQuery() {
        Instant lastMovement = Instant.parse("2026-09-01T10:15:30Z");
        when(stockLevelRepository.findReconciliationDivergences(isNull(), isNull())).thenReturn(List.<Object[]>of(
                row("ART-1", "Ciment", "LOC-A", "Depot A", "10", "4", "6", Timestamp.from(lastMovement))
        ));
        when(stockLevelRepository.countReconciliationPairs()).thenReturn(1L);

        StockReconciliationReportResponse report = service.reconcile(null, null);

        assertEquals(lastMovement, report.getDivergences().get(0).getLastMovementAt());
    }

    @Test
    void csvCarriesTheHeaderSummaryAndOneLinePerDivergence() {
        when(stockLevelRepository.findReconciliationDivergences(isNull(), isNull())).thenReturn(List.<Object[]>of(
                row("ART-1", "Ciment, 50kg", "LOC-A", "Depot A", "10", "4", "6", null)
        ));
        when(stockLevelRepository.countReconciliationPairs()).thenReturn(1L);

        String csv = service.reconcileCsv(null, null);

        assertTrue(csv.contains("Reconciliation des niveaux de stock"));
        assertTrue(csv.contains("Divergences:,1"));
        assertTrue(csv.contains("ART-1"));
        // Un libelle contenant une virgule doit etre echappe, sans quoi les colonnes se decalent.
        assertTrue(csv.contains("\"Ciment, 50kg\""));
    }

    private Object[] row(String itemCode, String itemName, String locationCode, String locationName,
                         String recorded, String expected, String difference, Object lastMovementAt) {
        return new Object[]{
                itemCode,
                itemName,
                locationCode,
                locationName,
                new BigDecimal(recorded),
                new BigDecimal(expected),
                new BigDecimal(difference),
                lastMovementAt
        };
    }
}
