package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryPeriodRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryPeriodReopenRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryPeriodResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockValuationSnapshotResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Periodes comptables de stock, et photos de valorisation qui les accompagnent.
 */
public interface InventoryPeriodService {

    InventoryPeriodResponse create(InventoryPeriodRequest request);

    InventoryPeriodResponse get(String periodCode);

    List<InventoryPeriodResponse> list();

    /**
     * Clot la periode : fige la valeur du stock, prend une photo de valorisation, et interdit tout
     * mouvement date dans l'intervalle.
     */
    InventoryPeriodResponse close(String periodCode, String closedBy);

    /**
     * Rouvre une periode close. Exige un motif, qui reste attache a la periode : rouvrir un exercice
     * clos est une decision qui doit laisser une trace.
     */
    InventoryPeriodResponse reopen(String periodCode, InventoryPeriodReopenRequest request);

    /**
     * Refuse un mouvement date dans une periode qui n'accepte plus d'ecriture.
     *
     * <p>Ne fait rien si aucune periode ne couvre la date : une entreprise qui n'a pas encore
     * declare ses periodes continue de fonctionner sans contrainte.</p>
     */
    void assertMovementAllowed(LocalDate movementDate);

    /** Code de la periode couvrant la date, ou null. */
    String periodCodeFor(LocalDate date);

    /**
     * Prend une photo de la valeur du stock a la date fournie, un enregistrement par couple article
     * et emplacement portant du stock.
     */
    List<StockValuationSnapshotResponse> takeSnapshot(LocalDate snapshotDate, String periodCode);

    List<StockValuationSnapshotResponse> snapshotAt(LocalDate snapshotDate);
}
