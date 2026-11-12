package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetItemRepository extends JpaRepository<AccBudgetItem, Long> {

    java.util.Optional<AccBudgetItem> findByCode(String code);

    java.util.List<AccBudgetItem> findAllByOrderByCodeAsc();
}
