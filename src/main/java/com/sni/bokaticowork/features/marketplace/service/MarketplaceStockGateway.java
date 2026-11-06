package com.sni.bokaticowork.features.marketplace.service;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import com.sni.bokaticowork.features.inventory.stock.model.StockReservation;
import com.sni.bokaticowork.features.inventory.stock.repository.StockReservationRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryConsumptionService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Les mouvements de stock d'une commande, chacun dans sa propre transaction.
 *
 * <p>Le stock est transactionnel : une erreur qui le traverserait dans la transaction de la commande
 * la condamnerait, meme attrapee, et avec elle le passage en « reglee ». Isole ici, un mouvement qui
 * echoue n'emporte que lui.</p>
 *
 * <p>Chaque mouvement part de l'etat reel de la retenue, et se rejoue donc sans risque : une retenue
 * deja consommee ne sort pas deux fois.</p>
 */
@Component
@RequiredArgsConstructor
public class MarketplaceStockGateway {

    /** Une ligne de commande vue du stock · sans entite, elle change de transaction. */
    public record StockLine(String reservationCode, String itemCode, String locationCode, BigDecimal quantity,
                            String orderNumber, String actor) {
    }

    private final StockReservationRepository reservationRepository;
    private final InventoryConsumptionService consumptionService;
    private final StockService stockService;
    private final InventoryItemRepository itemRepository;

    /**
     * Sort l'article du stock.
     *
     * @return la retenue consommee · une nouvelle si la precedente a ete levee entre-temps
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String issue(StockLine line) {
        StockReservationStatus status = statusOf(line);
        if (status == StockReservationStatus.CONSUMED) {
            return line.reservationCode();
        }
        String reservationCode = status == StockReservationStatus.ACTIVE ? line.reservationCode() : reserveAgain(line);
        consumptionService.consumeBusinessReservation(reservationCode, line.actor());
        return reservationCode;
    }

    /** Rend ce que la commande tenait · la retenue est levee, ce qui etait deja sorti revient. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void giveBack(StockLine line) {
        StockReservationStatus status = statusOf(line);
        if (status == StockReservationStatus.ACTIVE) {
            consumptionService.releaseBusinessReservation(line.reservationCode());
        } else if (status == StockReservationStatus.CONSUMED) {
            StockInRequest back = new StockInRequest();
            back.setItemCode(line.itemCode());
            back.setLocationCode(line.locationCode());
            back.setQuantity(line.quantity());
            back.setUnitCost(itemRepository.findByItemCode(line.itemCode()).map(InventoryItem::getDefaultCost).orElse(null));
            back.setReferenceType(StockReferenceType.POS_SALE);
            back.setReferenceCode(line.orderNumber());
            back.setReason("Retour · annulation de la commande " + line.orderNumber());
            back.setPerformedBy(line.actor());
            stockService.receive(back);
        }
    }

    private StockReservationStatus statusOf(StockLine line) {
        return reservationRepository.findByReservationCode(line.reservationCode())
                .map(StockReservation::getStatus)
                .orElse(null);
    }

    private String reserveAgain(StockLine line) {
        StockReservationRequest reservation = new StockReservationRequest();
        reservation.setItemCode(line.itemCode());
        reservation.setLocationCode(line.locationCode());
        reservation.setQuantity(line.quantity());
        reservation.setReferenceType(StockReferenceType.POS_SALE);
        reservation.setReferenceCode(line.orderNumber());
        reservation.setReservedBy(line.actor());
        return consumptionService.reserveForBusinessEvent(reservation).getReservationCode();
    }
}
