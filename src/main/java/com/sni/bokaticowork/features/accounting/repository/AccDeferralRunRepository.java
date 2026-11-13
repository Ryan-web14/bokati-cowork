package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccDeferralRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccDeferralRunRepository extends JpaRepository<AccDeferralRun, Long> {
    List<AccDeferralRun> findAllByKindAndStatusOrderByPeriodCodeDescIdDesc(String kind, String status);
    List<AccDeferralRun> findAllByKindAndPeriodCodeAndStatus(String kind, String periodCode, String status);
}
