package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccDeferralRunLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccDeferralRunLineRepository extends JpaRepository<AccDeferralRunLine, Long> {
    List<AccDeferralRunLine> findAllByRunId(Long runId);
    List<AccDeferralRunLine> findAllByScheduleIdOrderByIdDesc(Long scheduleId);
}
