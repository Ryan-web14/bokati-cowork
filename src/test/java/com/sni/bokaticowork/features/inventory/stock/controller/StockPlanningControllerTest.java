package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryUnitConversionRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryUnitConversionResponse;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnitConversion;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryUnitConversionRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les conversions d'unite se lisent hors transaction : leur article est un proxy non charge. La
 * reponse prend le code de l'article deja lu, sans toucher a ce proxy.
 */
@ExtendWith(MockitoExtension.class)
class StockPlanningControllerTest {

    @Mock private InventoryItemLookupService itemLookupService;
    @Mock private StockLevelRepository levelRepository;
    @Mock private InventoryUnitConversionRepository conversionRepository;

    private StockPlanningController controller;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        controller = new StockPlanningController(itemLookupService, levelRepository, conversionRepository);
        item = new InventoryItem();
        item.setItemCode("ITM-PAPER");
        when(itemLookupService.findByItemCodeOrThrow("ITM-PAPER")).thenReturn(item);
    }

    @Test
    @DisplayName("La liste des conversions ne lit pas l'article non charge de chaque conversion")
    void listDoesNotTouchTheDetachedItem() {
        InventoryUnitConversion listed = conversion();
        when(conversionRepository.findAllByItemAndActiveTrue(item)).thenReturn(List.of(listed));

        List<InventoryUnitConversionResponse> body = controller.conversions("ITM-PAPER").getBody();

        assertThat(body).singleElement().satisfies(response -> {
            assertThat(response.getItemCode()).isEqualTo("ITM-PAPER");
            assertThat(response.getFromUnitCode()).isEqualTo("BOX");
        });
    }

    @Test
    @DisplayName("La mise a jour d'une conversion existante ne lit pas l'article rendu par la fusion")
    void updateDoesNotTouchTheMergedItem() {
        InventoryUnitConversion existing = conversion();
        when(conversionRepository.findByItemAndFromUnitCodeAndToUnitCodeAndActiveTrue(item, "BOX", "UNIT"))
                .thenReturn(Optional.of(existing));
        InventoryUnitConversion merged = conversion();
        when(conversionRepository.save(any())).thenReturn(merged);

        InventoryUnitConversionRequest request = new InventoryUnitConversionRequest();
        request.setItemCode("ITM-PAPER");
        request.setFromUnitCode("box");
        request.setToUnitCode("unit");
        request.setFactor(new BigDecimal("500"));

        assertThat(controller.createConversion(request).getBody().getItemCode()).isEqualTo("ITM-PAPER");
    }

    /** Une conversion dont l'article est un proxy hors session. */
    private InventoryUnitConversion conversion() {
        InventoryItem proxy = mock(InventoryItem.class);
        lenient().when(proxy.getItemCode()).thenThrow(new LazyInitializationException("no session"));
        InventoryUnitConversion conversion = new InventoryUnitConversion();
        conversion.setItem(proxy);
        conversion.setFromUnitCode("BOX");
        conversion.setToUnitCode("UNIT");
        conversion.setFactor(new BigDecimal("500"));
        conversion.setActive(true);
        return conversion;
    }
}
