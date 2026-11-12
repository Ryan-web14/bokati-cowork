package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudgetItemMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetItemMappingRepository extends JpaRepository<AccBudgetItemMapping, Long> {

    java.util.List<AccBudgetItemMapping> findByItemCode(String itemCode);

    java.util.Optional<AccBudgetItemMapping> findByKindAndValue(String kind, String value);

    java.util.List<AccBudgetItemMapping> findByKind(String kind);
}
