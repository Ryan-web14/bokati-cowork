package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccLiasseSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccLiasseSheetRepository extends JpaRepository<AccLiasseSheet, Long> {
    List<AccLiasseSheet> findAllByVersionAndActiveTrueOrderByPositionAsc(Integer version);
    java.util.Optional<AccLiasseSheet> findFirstByActiveTrueOrderByVersionDesc();
}
