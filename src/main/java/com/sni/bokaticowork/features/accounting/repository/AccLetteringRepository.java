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

public interface AccLetteringRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccLettering, Long> {

    List<com.sni.bokaticowork.features.accounting.model.AccLettering> findByAccountNumberAndThirdPartyCodeOrderByCreatedAtAsc(String accountNumber, String thirdPartyCode);

    long countByAccountNumber(String accountNumber);
}
