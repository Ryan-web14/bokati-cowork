package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.model.QualityInspection;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualityInspectionRepository extends JpaRepository<QualityInspection, Long> {

    Optional<QualityInspection> findByInspectionCode(String inspectionCode);

    boolean existsByInspectionCode(String inspectionCode);

    List<QualityInspection> findAllByLotOrderByInspectedAtDesc(StockLot lot);

    Page<QualityInspection> findAllByItemOrderByInspectedAtDesc(InventoryItem item, Pageable pageable);
}
