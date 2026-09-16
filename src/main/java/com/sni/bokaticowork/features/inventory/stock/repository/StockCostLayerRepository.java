package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockCostLayer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockCostLayerRepository extends JpaRepository<StockCostLayer, Long> {

    /**
     * Couches consommables, dans l'ordre FIFO, verrouillees pour ecriture.
     *
     * <p>Le verrou est indispensable : deux sorties simultanees sur le meme article liraient sinon
     * les memes couches et les consommeraient deux fois.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT layer FROM StockCostLayer layer
            WHERE layer.item = :item
              AND layer.location = :location
              AND layer.remainingQuantity > 0
            ORDER BY layer.receivedAt ASC, layer.id ASC
            """)
    List<StockCostLayer> findConsumableForUpdate(@Param("item") InventoryItem item,
                                                 @Param("location") InventoryLocation location);

    @Query("""
            SELECT layer FROM StockCostLayer layer
            WHERE layer.item = :item
              AND layer.location = :location
              AND layer.remainingQuantity > 0
            ORDER BY layer.receivedAt ASC, layer.id ASC
            """)
    List<StockCostLayer> findConsumableReadOnly(@Param("item") InventoryItem item,
                                                @Param("location") InventoryLocation location);

    boolean existsByItemAndLocation(InventoryItem item, InventoryLocation location);

    boolean existsByItem(InventoryItem item);
}
