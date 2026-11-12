package com.sni.bokaticowork.features.fund.repository;

import com.sni.bokaticowork.features.fund.model.ExpenseJustificationLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseJustificationLineRepository extends JpaRepository<ExpenseJustificationLine, Long> {

    java.util.List<ExpenseJustificationLine> findByRequestIdOrderByLineNoAsc(Long requestId);

    java.util.Optional<ExpenseJustificationLine> findByNumber(String number);

    java.util.List<ExpenseJustificationLine> findByStatusOrderByCreatedAtAsc(com.sni.bokaticowork.features.fund.enums.ExpenseLineStatus status);
}
