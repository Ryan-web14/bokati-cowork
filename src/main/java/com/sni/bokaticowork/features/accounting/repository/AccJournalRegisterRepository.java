package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccJournalRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccJournalRegisterRepository extends JpaRepository<AccJournalRegister, Long> {

    Optional<AccJournalRegister> findByJournalCodeAndCashRegisterCode(String journalCode, String cashRegisterCode);

    List<AccJournalRegister> findByCashRegisterCode(String cashRegisterCode);
}
