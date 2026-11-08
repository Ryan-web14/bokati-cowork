package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.features.inventory.stock.dto.response.InventorySerialResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.InventorySerialStatus;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La liste des numeros de serie repond sous /inventory/items/{itemCode}/serials,
 * et non sous le prefixe /inventory/stock du controleur de stock.
 */
class StockSerialRouteTest {

    @Test
    @DisplayName("GET /inventory/items/{itemCode}/serials repond, l ancien chemin double non")
    void serialsRouteIsReachable() throws Exception {
        StockService service = mock(StockService.class);
        when(service.getSerials("ITM-1", "LOC-1", InventorySerialStatus.AVAILABLE))
                .thenReturn(List.of(InventorySerialResponse.builder().build()));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new StockController(service), new StockSerialController(service))
                .build();

        mvc.perform(get("/sni/api/v1/inventory/items/ITM-1/serials")
                        .param("locationCode", "LOC-1")
                        .param("status", "AVAILABLE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(service).getSerials("ITM-1", "LOC-1", InventorySerialStatus.AVAILABLE);

        mvc.perform(get("/sni/api/v1/inventory/stock/sni/api/v1/inventory/items/ITM-1/serials"))
                .andExpect(status().isNotFound());
    }
}
