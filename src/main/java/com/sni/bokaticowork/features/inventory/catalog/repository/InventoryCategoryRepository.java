package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

    boolean existsByCode(String code);

    Optional<InventoryCategory> findByCode(String code);

    List<InventoryCategory> findAllByOrderByNameAsc();

    List<InventoryCategory> findAllByActiveTrueOrderByNameAsc();
}
