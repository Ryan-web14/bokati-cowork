package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccLoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccLoanEventRepository extends JpaRepository<AccLoanEvent, Long> {

    java.util.List<AccLoanEvent> findAllByLoanIdOrderByCreatedAtAsc(Long loanId);
}
