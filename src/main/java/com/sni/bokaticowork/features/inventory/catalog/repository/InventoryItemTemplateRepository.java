package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemTemplateRepository extends JpaRepository<InventoryItemTemplate, Long> {

    Optional<InventoryItemTemplate> findByTemplateCode(String templateCode);

    boolean existsByTemplateCode(String templateCode);

    List<InventoryItemTemplate> findAllByOrderByNameAsc();

    List<InventoryItemTemplate> findAllByActiveTrueOrderByNameAsc();
}
