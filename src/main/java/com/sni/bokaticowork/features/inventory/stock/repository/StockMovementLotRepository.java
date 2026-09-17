package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovementLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementLotRepository extends JpaRepository<StockMovementLot, Long> {
    List<StockMovementLot> findAllByMovement(StockMovement movement);

    /**
     * Mouvements ayant touche un numero de lot, du plus recent au plus ancien.
     *
     * <p>Base de la tracabilite aval : c est la seule trace de ce qu un lot est devenu apres sa
     * sortie du stock.</p>
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT line FROM StockMovementLot line
            WHERE upper(line.lotNumber) = upper(:lotNumber)
            ORDER BY line.movement.performedAt DESC
            """)
    List<StockMovementLot> findAllByLotNumber(@org.springframework.data.repository.query.Param("lotNumber") String lotNumber);
}
