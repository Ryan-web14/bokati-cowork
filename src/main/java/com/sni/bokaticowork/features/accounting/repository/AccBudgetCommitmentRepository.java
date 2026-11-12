package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudgetCommitment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetCommitmentRepository extends JpaRepository<AccBudgetCommitment, Long> {

    java.util.List<AccBudgetCommitment> findByLineIdAndStatus(Long lineId, String status);

    java.util.List<AccBudgetCommitment> findBySourceTypeAndReference(String sourceType, String reference);

    java.util.List<AccBudgetCommitment> findByStatusOrderByCreatedAtAsc(String status);

    java.util.List<AccBudgetCommitment> findByLineId(Long lineId);
}
