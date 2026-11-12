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

public interface AccFiscalYearRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccFiscalYear, Long> {

    Optional<com.sni.bokaticowork.features.accounting.model.AccFiscalYear> findByCode(String code);

    List<com.sni.bokaticowork.features.accounting.model.AccFiscalYear> findAllByOrderByStartDateAsc();
}
