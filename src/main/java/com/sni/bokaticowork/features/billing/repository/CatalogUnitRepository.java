package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.CatalogUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C0 : unites de vente du catalogue. */
@Repository
public interface CatalogUnitRepository extends JpaRepository<CatalogUnit, Long> {

    @Query("SELECT u FROM CatalogUnit u ORDER BY u.displayOrder, lower(u.label)")
    List<CatalogUnit> findAllOrdered();

    @Query("SELECT u FROM CatalogUnit u WHERE lower(u.code) = lower(:code)")
    Optional<CatalogUnit> findByCodeIgnoreCase(@Param("code") String code);

    @Query("SELECT u FROM CatalogUnit u WHERE lower(u.label) = lower(:label) OR lower(u.pluralLabel) = lower(:label)")
    List<CatalogUnit> findByLabelIgnoreCase(@Param("label") String label);
}
