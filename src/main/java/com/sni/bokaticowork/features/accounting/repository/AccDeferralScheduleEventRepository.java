package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccDeferralScheduleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccDeferralScheduleEventRepository extends JpaRepository<AccDeferralScheduleEvent, Long> {
    List<AccDeferralScheduleEvent> findAllByScheduleIdOrderByCreatedAtAsc(Long scheduleId);
}
