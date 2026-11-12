package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankStatementLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, Long> {

    java.util.List<BankStatementLine> findByStatementIdOrderByLineNoAsc(Long statementId);

    java.util.List<BankStatementLine> findByBankAccountCodeAndMatchingCodeIsNullOrderByOperationDateAsc(String bankAccountCode);

    boolean existsByBankAccountCodeAndFingerprint(String bankAccountCode, String fingerprint);

    java.util.List<BankStatementLine> findByBankAccountCodeOrderByOperationDateAsc(String bankAccountCode);
}
