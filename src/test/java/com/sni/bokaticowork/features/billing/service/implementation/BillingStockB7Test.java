package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingStockReservation;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingStockReservationRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingStockGuard;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockOutRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot B7 : 12 chaises demandees pour 10 disponibles ; alerte de 2 manquantes, acceptation qui reserve
 * les 10, facture qui consomme la reservation et sort le reste si l'article autorise le stock negatif.
 */
class BillingStockB7Test {

    private final BillingStockReservationRepository reservations = mock(BillingStockReservationRepository.class);
    private final BillingDocumentLineRepository lines = mock(BillingDocumentLineRepository.class);
    private final InventoryItemRepository items = mock(InventoryItemRepository.class);
    private final StockLevelRepository levels = mock(StockLevelRepository.class);
    private final StockService stock = mock(StockService.class);
    private final BillingStockGuard guard = new BillingStockGuard(items, levels);
    private final BillingStockService service = new BillingStockService(reservations, lines, items, levels, stock, guard);

    private final InventoryItem chair = InventoryItem.builder().id(1L).itemCode("ART-CHAISE").name("Chaise").active(true)
            .itemType(InventoryItemType.CONSUMABLE).trackingType(InventoryTrackingType.QUANTITY).allowNegativeStock(false).build();
    private final List<BillingStockReservation> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(items.findByItemCode("ART-CHAISE")).thenReturn(Optional.of(chair));
        when(levels.sumAvailable(1L)).thenReturn(new BigDecimal("10"));
        InventoryLocation store = mock(InventoryLocation.class);
        when(store.getLocationCode()).thenReturn("MAG-1");
        StockLevel level = mock(StockLevel.class);
        when(level.getLocation()).thenReturn(store);
        when(level.getQuantityAvailable()).thenReturn(new BigDecimal("10"));
        when(levels.findAllByItemAndQuantityAvailableGreaterThanOrderByQuantityAvailableDesc(any(), any())).thenReturn(List.of(level));
        when(levels.findAllByItemIdOrderByQuantityAvailableAsc(1L)).thenReturn(List.of(level));
        StockReservationResponse reserved = mock(StockReservationResponse.class);
        when(reserved.getReservationCode()).thenReturn("RSV-1");
        when(stock.reserve(any())).thenReturn(reserved);
        when(reservations.save(any(BillingStockReservation.class))).thenAnswer(invocation -> {
            BillingStockReservation reservation = invocation.getArgument(0);
            if (!saved.contains(reservation)) {
                saved.add(reservation);
            }
            return reservation;
        });
        when(reservations.findByDocumentNumberAndStatus(anyString(), eq("ACTIVE")))
                .thenAnswer(invocation -> saved.stream().filter(r -> "ACTIVE".equals(r.getStatus())).toList());
    }

    @Test
    void shouldAnnounceTwoMissingChairs() {
        List<BillingStockGuard.StockAlert> alerts = guard.evaluate(BillingDocumentType.QUOTE, List.of(chairs(0, "12")));

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.severity()).isEqualTo("WARN");
            assertThat(alert.message()).contains("2 manquant(s)");
        });
        assertThat(guard.evaluate(BillingDocumentType.INVOICE, List.of(chairs(0, "12")))).singleElement()
                .satisfies(alert -> assertThat(alert.severity()).isEqualTo("BLOCK"));
    }

    @Test
    void shouldReserveTheTenAvailableChairsAtAcceptance() {
        BillingDocument quote = document("DEV-1", BillingDocumentType.QUOTE, null);
        when(lines.findAllByDocumentOrderByLineOrderAscIdAsc(quote)).thenReturn(List.of(line("12")));

        BigDecimal missing = service.reserveForQuote(quote);

        assertThat(missing).isEqualByComparingTo("2");
        ArgumentCaptor<StockReservationRequest> request = ArgumentCaptor.forClass(StockReservationRequest.class);
        verify(stock).reserve(request.capture());
        assertThat(request.getValue().getQuantity()).isEqualByComparingTo("10");
        assertThat(saved).singleElement().satisfies(reservation -> assertThat(reservation.getStatus()).isEqualTo("ACTIVE"));
    }

    @Test
    void shouldConsumeTheReservationAndIssueTheRestWhenNegativeStockIsAllowed() {
        chair.setAllowNegativeStock(true);
        BillingDocument quote = document("DEV-1", BillingDocumentType.QUOTE, null);
        when(lines.findAllByDocumentOrderByLineOrderAscIdAsc(quote)).thenReturn(List.of(line("12")));
        service.reserveForQuote(quote);
        BillingDocument invoice = document("FAC-1", BillingDocumentType.INVOICE, "DEV-1");
        when(lines.findAllByDocumentOrderByLineOrderAscIdAsc(invoice)).thenReturn(List.of(line("12")));
        when(levels.findAllByItemAndQuantityAvailableGreaterThanOrderByQuantityAvailableDesc(any(), any())).thenReturn(List.of());

        service.issueForInvoice(invoice);

        verify(stock).consumeReservation(eq("RSV-1"), anyString());
        ArgumentCaptor<StockOutRequest> out = ArgumentCaptor.forClass(StockOutRequest.class);
        verify(stock).issue(out.capture());
        assertThat(out.getValue().getQuantity()).isEqualByComparingTo("2");
        assertThat(out.getValue().getAllowNegativeOverride()).isTrue();
        assertThat(saved.get(0).getStatus()).isEqualTo("CONSUMED");
    }

    @Test
    void shouldRefuseToIssueMissingChairsOtherwise() {
        BillingDocument invoice = document("FAC-2", BillingDocumentType.INVOICE, null);
        when(lines.findAllByDocumentOrderByLineOrderAscIdAsc(invoice)).thenReturn(List.of(line("12")));
        when(levels.findAllByItemAndQuantityAvailableGreaterThanOrderByQuantityAvailableDesc(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.issueForInvoice(invoice)).isInstanceOf(BadRequestException.class).hasMessageContaining("12 manquant");
    }

    @Test
    void shouldReleaseTheReservationWhenTheQuoteIsRefused() {
        BillingDocument quote = document("DEV-1", BillingDocumentType.QUOTE, null);
        when(lines.findAllByDocumentOrderByLineOrderAscIdAsc(quote)).thenReturn(List.of(line("5")));
        service.reserveForQuote(quote);

        service.releaseForQuote("DEV-1");

        verify(stock).releaseReservation("RSV-1");
        assertThat(saved.get(0).getStatus()).isEqualTo("RELEASED");
    }

    private static BillingDocument document(String number, BillingDocumentType type, String quote) {
        return BillingDocument.builder().documentNumber(number).documentType(type).sourceType(quote == null ? null : "QUOTE").sourceCode(quote).build();
    }

    private static BillingDocumentLine line(String quantity) {
        return BillingDocumentLine.builder().lineOrder(1).lineType(BillingLineType.PRODUCT).itemCode("ART-CHAISE").sourceType("INVENTORY_ITEM")
                .description("Chaise").quantity(new BigDecimal(quantity)).build();
    }

    private static BillingStockGuard.Line chairs(int index, String quantity) {
        return new BillingStockGuard.Line(index, "ART-CHAISE", "INVENTORY_ITEM", BillingLineType.PRODUCT, "Chaise", new BigDecimal(quantity));
    }
}
