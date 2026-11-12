package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    java.util.Optional<BankAccount> findByCode(String code);

    java.util.List<BankAccount> findAllByOrderByCodeAsc();
}
