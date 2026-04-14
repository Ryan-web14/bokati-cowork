package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovementLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementLotRepository extends JpaRepository<StockMovementLot, Long> {
    List<StockMovementLot> findAllByMovement(StockMovement movement);
}
