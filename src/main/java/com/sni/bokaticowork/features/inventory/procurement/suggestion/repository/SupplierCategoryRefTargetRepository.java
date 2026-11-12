package com.sni.bokaticowork.features.inventory.procurement.suggestion.repository;

import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryKind;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryRefTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SupplierCategoryRefTargetRepository extends JpaRepository<SupplierCategoryRefTarget, Long> {

    List<SupplierCategoryRefTarget> findAllByCategoryIdIn(Collection<Long> categoryIds);

    List<SupplierCategoryRefTarget> findAllByTargetKindAndTargetCode(SupplierCategoryKind targetKind, String targetCode);

    void deleteAllByCategoryId(Long categoryId);
}
