package com.sni.bokaticowork.features.accounting.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccNumberingCounterRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccNumberingCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AccNumberingCounter c WHERE c.journalCode = :journal AND c.fiscalYearCode = :year AND c.periodCode = :period")
    Optional<com.sni.bokaticowork.features.accounting.model.AccNumberingCounter> lock(@Param("journal") String journal, @Param("year") String year, @Param("period") String period);

    List<com.sni.bokaticowork.features.accounting.model.AccNumberingCounter> findByJournalCodeAndFiscalYearCode(String journalCode, String fiscalYearCode);
}
