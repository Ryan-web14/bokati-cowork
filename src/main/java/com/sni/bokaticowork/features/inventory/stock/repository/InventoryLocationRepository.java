package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, Long> {

    boolean existsByLocationCode(String locationCode);

    Optional<InventoryLocation> findByLocationCode(String locationCode);

    List<InventoryLocation> findAllByOrderByNameAsc();

    List<InventoryLocation> findAllByActiveTrueOrderByNameAsc();
}
