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

public interface AccPeriodRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccPeriod, Long> {

    Optional<com.sni.bokaticowork.features.accounting.model.AccPeriod> findByCode(String code);

    List<com.sni.bokaticowork.features.accounting.model.AccPeriod> findByFiscalYearIdOrderByStartDateAsc(Long fiscalYearId);

    @Query("SELECT p FROM AccPeriod p WHERE p.startDate <= :date AND p.endDate >= :date")
    Optional<com.sni.bokaticowork.features.accounting.model.AccPeriod> findCovering(@Param("date") LocalDate date);

    List<com.sni.bokaticowork.features.accounting.model.AccPeriod> findAllByOrderByStartDateAsc();
}
