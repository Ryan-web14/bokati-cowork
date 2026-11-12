package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudgetTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetTransferRepository extends JpaRepository<AccBudgetTransfer, Long> {

    java.util.List<AccBudgetTransfer> findAllByOrderByCreatedAtDesc();
}
