package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryUnitRepository extends JpaRepository<InventoryUnit, Long> {

    boolean existsByCode(String code);

    Optional<InventoryUnit> findByCode(String code);

    List<InventoryUnit> findAllByOrderByNameAsc();

    List<InventoryUnit> findAllByActiveTrueOrderByNameAsc();
}
