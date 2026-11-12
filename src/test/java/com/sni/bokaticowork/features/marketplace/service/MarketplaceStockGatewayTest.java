package com.sni.bokaticowork.features.marketplace.service;

import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import com.sni.bokaticowork.features.inventory.stock.model.StockReservation;
import com.sni.bokaticowork.features.inventory.stock.repository.StockReservationRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryConsumptionService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Chaque mouvement part de l'etat reel de la retenue · il se rejoue sans sortir ni rendre deux fois.
 */
class MarketplaceStockGatewayTest {

    private static final MarketplaceStockGateway.StockLine LINE = new MarketplaceStockGateway.StockLine(
            "RSV-1", "ITM-CAFE", "LOC-ACCUEIL", BigDecimal.valueOf(2), "MKC-1", "SYSTEM");

    private final StockReservationRepository reservations = mock(StockReservationRepository.class);
    private final InventoryConsumptionService consumption = mock(InventoryConsumptionService.class);
    private final StockService stock = mock(StockService.class);
    private final InventoryItemRepository items = mock(InventoryItemRepository.class);
    private final MarketplaceStockGateway gateway = new MarketplaceStockGateway(reservations, consumption, stock, items);

    private void reservationIs(StockReservationStatus status) {
        StockReservation reservation = mock(StockReservation.class);
        when(reservation.getStatus()).thenReturn(status);
        when(reservations.findByReservationCode("RSV-1")).thenReturn(Optional.of(reservation));
    }

    @Test
    @DisplayName("Sortie : une retenue active est consommee, une retenue deja consommee ne sort pas deux fois")
    void issueIsIdempotent() {
        reservationIs(StockReservationStatus.ACTIVE);
        assertThat(gateway.issue(LINE)).isEqualTo("RSV-1");
        verify(consumption).consumeBusinessReservation("RSV-1", "SYSTEM");

        reservationIs(StockReservationStatus.CONSUMED);
        assertThat(gateway.issue(LINE)).isEqualTo("RSV-1");
        verify(consumption).consumeBusinessReservation(anyString(), anyString());
    }

    @Test
    @DisplayName("Sortie : une retenue levee entre-temps est reprise, puis consommee")
    void issueReservesAgain() {
        reservationIs(StockReservationStatus.RELEASED);
        StockReservationResponse fresh = StockReservationResponse.builder().reservationCode("RSV-2").build();
        when(consumption.reserveForBusinessEvent(any())).thenReturn(fresh);

        assertThat(gateway.issue(LINE)).isEqualTo("RSV-2");

        verify(consumption).consumeBusinessReservation("RSV-2", "SYSTEM");
    }

    @Test
    @DisplayName("Retour : la retenue active est levee sans entree de stock · un article sorti revient par une entree")
    void giveBackFollowsTheReservation() {
        reservationIs(StockReservationStatus.ACTIVE);
        gateway.giveBack(LINE);
        verify(consumption).releaseBusinessReservation("RSV-1");
        verify(stock, never()).receive(any());

        reservationIs(StockReservationStatus.CONSUMED);
        gateway.giveBack(LINE);
        ArgumentCaptor<StockInRequest> back = ArgumentCaptor.forClass(StockInRequest.class);
        verify(stock).receive(back.capture());
        assertThat(back.getValue().getItemCode()).isEqualTo("ITM-CAFE");
        assertThat(back.getValue().getQuantity()).isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Retour : une retenue deja levee ne bouge rien")
    void giveBackIgnoresClosedReservation() {
        reservationIs(StockReservationStatus.RELEASED);

        gateway.giveBack(LINE);

        verify(consumption, never()).releaseBusinessReservation(anyString());
        verify(stock, never()).receive(any());
    }
}
