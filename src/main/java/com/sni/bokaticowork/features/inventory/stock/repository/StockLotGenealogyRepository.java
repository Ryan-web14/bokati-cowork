package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.model.StockLotGenealogy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockLotGenealogyRepository extends JpaRepository<StockLotGenealogy, Long> {

    /** Liens ou les lots fournis sont le parent : ce qui en descend. */
    @Query("SELECT link FROM StockLotGenealogy link WHERE link.parentLot IN :lots ORDER BY link.createdAt ASC")
    List<StockLotGenealogy> findByParents(@Param("lots") List<StockLot> lots);

    /** Liens ou les lots fournis sont l enfant : ce dont ils descendent. */
    @Query("SELECT link FROM StockLotGenealogy link WHERE link.childLot IN :lots ORDER BY link.createdAt ASC")
    List<StockLotGenealogy> findByChildren(@Param("lots") List<StockLot> lots);

    boolean existsByParentLotAndChildLot(StockLot parentLot, StockLot childLot);
}
