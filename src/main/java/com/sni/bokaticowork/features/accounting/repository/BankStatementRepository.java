package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {

    java.util.List<BankStatement> findByBankAccountCodeAndStatusOrderByPeriodToDesc(String bankAccountCode, String status);

    java.util.Optional<BankStatement> findByNumber(String number);
}
