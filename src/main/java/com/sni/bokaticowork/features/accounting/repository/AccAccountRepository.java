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

public interface AccAccountRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccAccount, Long> {

    Optional<com.sni.bokaticowork.features.accounting.model.AccAccount> findByNumber(String number);

    List<com.sni.bokaticowork.features.accounting.model.AccAccount> findByParentNumberOrderByNumberAsc(String parentNumber);

    List<com.sni.bokaticowork.features.accounting.model.AccAccount> findAllByOrderByNumberAsc();

    @Query("SELECT a FROM AccAccount a WHERE a.number LIKE CONCAT(:prefix, '%') ORDER BY a.number")
    List<com.sni.bokaticowork.features.accounting.model.AccAccount> findByPrefix(@Param("prefix") String prefix);
}
