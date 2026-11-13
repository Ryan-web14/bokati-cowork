package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.CatalogComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Lot C14 : composants des articles composes. */
@Repository
public interface CatalogComponentRepository extends JpaRepository<CatalogComponent, Long> {

    List<CatalogComponent> findByParentItemCodeOrderByDisplayOrderAsc(String parentItemCode);

    @Modifying
    @Query("DELETE FROM CatalogComponent c WHERE c.parentItemCode = :parent")
    void deleteByParent(@Param("parent") String parent);
}
