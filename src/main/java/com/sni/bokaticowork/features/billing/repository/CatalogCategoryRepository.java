package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.CatalogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C0 : familles et sous-familles du catalogue. */
@Repository
public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, Long> {

    @Query("SELECT c FROM CatalogCategory c ORDER BY c.displayOrder, lower(c.label)")
    List<CatalogCategory> findAllOrdered();

    @Query("SELECT c FROM CatalogCategory c WHERE c.parentCode IS NULL AND lower(c.code) = lower(:code)")
    Optional<CatalogCategory> findFamily(@Param("code") String code);

    @Query("SELECT c FROM CatalogCategory c WHERE c.parentCode IS NULL AND lower(c.label) = lower(:label)")
    List<CatalogCategory> findFamiliesByLabel(@Param("label") String label);

    @Query("SELECT c FROM CatalogCategory c WHERE lower(c.parentCode) = lower(:parent) AND lower(c.code) = lower(:code)")
    Optional<CatalogCategory> findSubcategory(@Param("parent") String parent, @Param("code") String code);

    @Query("SELECT c FROM CatalogCategory c WHERE lower(c.parentCode) = lower(:parent) AND lower(c.label) = lower(:label)")
    List<CatalogCategory> findSubcategoriesByLabel(@Param("parent") String parent, @Param("label") String label);

    @Query("SELECT c FROM CatalogCategory c WHERE c.parentCode IS NOT NULL AND (lower(c.code) = lower(:value) OR lower(c.label) = lower(:value))")
    List<CatalogCategory> findAnySubcategory(@Param("value") String value);

    @Query("SELECT c FROM CatalogCategory c WHERE lower(c.parentCode) = lower(:parent) ORDER BY c.displayOrder, lower(c.label)")
    List<CatalogCategory> findSubcategories(@Param("parent") String parent);

    @Modifying
    @Query("UPDATE CatalogCategory c SET c.parentCode = :target WHERE lower(c.parentCode) = lower(:source)")
    int moveSubcategories(@Param("source") String source, @Param("target") String target);
}
