package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccLoan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccLoanRepository extends JpaRepository<AccLoan, Long> {

    java.util.Optional<AccLoan> findByCode(String code);

    java.util.List<AccLoan> findAllByOrderByCodeAsc();

    java.util.List<AccLoan> findAllByStatus(String status);
}
