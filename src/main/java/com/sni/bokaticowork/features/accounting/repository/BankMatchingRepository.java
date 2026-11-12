package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankMatching;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankMatchingRepository extends JpaRepository<BankMatching, Long> {

    java.util.Optional<BankMatching> findByBankAccountCodeAndCode(String bankAccountCode, String code);

    long countByBankAccountCode(String bankAccountCode);

    java.util.List<BankMatching> findByBankAccountCodeAndUnmatchedAtIsNull(String bankAccountCode);
}
