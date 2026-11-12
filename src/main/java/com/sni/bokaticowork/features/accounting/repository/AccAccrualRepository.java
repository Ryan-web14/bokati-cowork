package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAccrual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccAccrualRepository extends JpaRepository<AccAccrual, Long> {
    List<AccAccrual> findAllByOrderByCreatedAtDesc();
    List<AccAccrual> findAllByPeriodCode(String periodCode);
    List<AccAccrual> findAllByStatus(String status);
}
