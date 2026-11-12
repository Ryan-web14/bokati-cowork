package com.sni.bokaticowork.features.inventory.procurement.suggestion.repository;

import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryKind;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SupplierCategoryLinkRepository extends JpaRepository<SupplierCategoryLink, Long> {

    List<SupplierCategoryLink> findAllBySupplierIdOrderByCategoryKindAscCategoryCodeAsc(Long supplierId);

    List<SupplierCategoryLink> findAllBySupplierIdIn(Collection<Long> supplierIds);

    List<SupplierCategoryLink> findAllByCategoryKindAndCategoryCodeIn(SupplierCategoryKind kind, Collection<String> codes);

    boolean existsByCategoryKindAndCategoryCode(SupplierCategoryKind kind, String code);

    long countByCategoryKindAndCategoryCode(SupplierCategoryKind kind, String code);

    void deleteAllBySupplierId(Long supplierId);
}
