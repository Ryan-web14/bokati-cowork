package com.sni.bokaticowork.features.inventory.procurement.suggestion.repository;

import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierCategoryRefRepository extends JpaRepository<SupplierCategoryRef, Long> {

    Optional<SupplierCategoryRef> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByParentId(Long parentId);

    List<SupplierCategoryRef> findAllByOrderBySortOrderAscLabelAsc();
}
