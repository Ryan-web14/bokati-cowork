package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.ProductRecallStatus;
import com.sni.bokaticowork.features.inventory.stock.model.ProductRecall;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRecallRepository extends JpaRepository<ProductRecall, Long> {

    Optional<ProductRecall> findByRecallCode(String recallCode);

    boolean existsByRecallCode(String recallCode);

    List<ProductRecall> findAllByOrderByCreatedAtDesc();

    List<ProductRecall> findAllByStatusOrderByCreatedAtDesc(ProductRecallStatus status);

    /**
     * Lots d un article tombant dans une plage de numeros, bornes comprises.
     *
     * <p>Une borne nulle est ouverte : sans borne du tout, tous les lots de l article sont
     * concernes, ce qui est le cas du rappel total.</p>
     */
    @Query("""
            SELECT lot FROM StockLot lot
            WHERE lot.item = :item
              AND lot.active = TRUE
              AND (:lotFrom IS NULL OR lot.lotNumber >= :lotFrom)
              AND (:lotTo IS NULL OR lot.lotNumber <= :lotTo)
            ORDER BY lot.lotNumber ASC
            """)
    List<StockLot> findLotsInRange(@Param("item") InventoryItem item,
                                   @Param("lotFrom") String lotFrom,
                                   @Param("lotTo") String lotTo);
}
