package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.WithholdingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C6 : types de retenue a la source. */
@Repository
public interface WithholdingTypeRepository extends JpaRepository<WithholdingType, Long> {

    Optional<WithholdingType> findByCode(String code);

    @Query("SELECT t FROM WithholdingType t ORDER BY t.displayOrder, t.label")
    List<WithholdingType> findAllOrdered();
}
