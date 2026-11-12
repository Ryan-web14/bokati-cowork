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

public interface AccNumberingCheckRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccNumberingCheck, Long> {

    List<com.sni.bokaticowork.features.accounting.model.AccNumberingCheck> findTop50ByOrderByCreatedAtDesc();
}
