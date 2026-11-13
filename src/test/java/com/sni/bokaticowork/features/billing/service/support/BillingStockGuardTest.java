package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lots B13 et D5 : manque de stock, alerte sur un devis, blocage sur une facture sauf stock negatif autorise. */
class BillingStockGuardTest {

    private final InventoryItemRepository items = mock(InventoryItemRepository.class);
    private final StockLevelRepository levels = mock(StockLevelRepository.class);
    private final BillingStockGuard guard = new BillingStockGuard(items, levels);

    private final InventoryItem chair = InventoryItem.builder().id(1L).itemCode("ART-CHAISE").name("Chaise")
            .itemType(InventoryItemType.CONSUMABLE).trackingType(InventoryTrackingType.QUANTITY).allowNegativeStock(false).build();

    @BeforeEach
    void setUp() {
        when(items.findByItemCode("ART-CHAISE")).thenReturn(Optional.of(chair));
        when(levels.sumAvailable(1L)).thenReturn(new BigDecimal("2"));
    }

    @Test
    void shouldBlockAnInvoiceAndOnlyWarnOnAQuote() {
        List<BillingStockGuard.Line> lines = List.of(new BillingStockGuard.Line(0, "ART-CHAISE", "INVENTORY_ITEM", BillingLineType.PRODUCT,
                "Chaise", new BigDecimal("5")));

        assertThat(guard.evaluate(BillingDocumentType.INVOICE, lines)).singleElement().satisfies(alert -> {
            assertThat(alert.severity()).isEqualTo("BLOCK");
            assertThat(alert.available()).isEqualByComparingTo("2");
        });
        assertThat(guard.evaluate(BillingDocumentType.QUOTE, lines)).singleElement()
                .satisfies(alert -> assertThat(alert.severity()).isEqualTo("WARN"));
    }

    @Test
    void shouldOnlyWarnWhenNegativeStockIsAllowed() {
        chair.setAllowNegativeStock(true);

        assertThat(guard.evaluate(BillingDocumentType.INVOICE, List.of(new BillingStockGuard.Line(0, "ART-CHAISE", null, BillingLineType.PRODUCT,
                "Chaise", new BigDecimal("5"))))).singleElement().satisfies(alert -> assertThat(alert.severity()).isEqualTo("WARN"));
    }

    @Test
    void shouldIgnoreServicesUntrackedItemsAndSufficientStock() {
        assertThat(guard.evaluate(BillingDocumentType.INVOICE, List.of(new BillingStockGuard.Line(0, "ART-CHAISE", "INVENTORY_ITEM",
                BillingLineType.PRODUCT, "Chaise", new BigDecimal("2"))))).isEmpty();
        assertThat(guard.evaluate(BillingDocumentType.INVOICE, List.of(new BillingStockGuard.Line(0, "SRV-1", "SERVICE_CATALOG",
                BillingLineType.SERVICE, "Salle", new BigDecimal("9"))))).isEmpty();
        chair.setTrackingType(InventoryTrackingType.NONE);
        assertThat(guard.evaluate(BillingDocumentType.INVOICE, List.of(new BillingStockGuard.Line(0, "ART-CHAISE", "INVENTORY_ITEM",
                BillingLineType.PRODUCT, "Chaise", new BigDecimal("5"))))).isEmpty();
    }
}
