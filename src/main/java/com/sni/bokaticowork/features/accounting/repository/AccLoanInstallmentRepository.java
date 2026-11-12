package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccLoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccLoanInstallmentRepository extends JpaRepository<AccLoanInstallment, Long> {

    java.util.List<AccLoanInstallment> findAllByLoanIdAndVersionOrderByNumberAsc(Long loanId, Integer version);

    java.util.List<AccLoanInstallment> findAllByLoanIdOrderByVersionAscNumberAsc(Long loanId);

    java.util.List<AccLoanInstallment> findAllByStatusAndDueDateLessThanEqualOrderByDueDateAsc(String status, java.time.LocalDate date);
}
