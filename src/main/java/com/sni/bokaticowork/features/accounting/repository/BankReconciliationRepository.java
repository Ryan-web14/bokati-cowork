package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {

    java.util.List<BankReconciliation> findByBankAccountCodeOrderByReconciliationDateDesc(String bankAccountCode);

    java.util.Optional<BankReconciliation> findByNumber(String number);
}
