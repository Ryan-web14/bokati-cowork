package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityControlStage;
import com.sni.bokaticowork.features.inventory.stock.model.QualityControlPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QualityControlPlanRepository extends JpaRepository<QualityControlPlan, Long> {

    Optional<QualityControlPlan> findByPlanCode(String planCode);

    boolean existsByPlanCode(String planCode);

    List<QualityControlPlan> findAllByOrderByNameAsc();

    /**
     * Plan applicable a un article pour une etape donnee.
     *
     * <p>Le plan porte sur l article lui-meme s il en existe un, a defaut sur sa categorie. Cet
     * ordre est volontaire : un plan pose sur un article precis doit primer sur la regle generale
     * de sa famille.</p>
     */
    @Query("""
            SELECT plan FROM QualityControlPlan plan
            WHERE plan.active = TRUE
              AND plan.controlStage = :stage
              AND (plan.item = :item OR (plan.item IS NULL AND plan.category = :#{#item.category}))
            ORDER BY CASE WHEN plan.item IS NOT NULL THEN 0 ELSE 1 END ASC
            """)
    List<QualityControlPlan> findApplicable(@Param("item") InventoryItem item,
                                            @Param("stage") QualityControlStage stage);
}
