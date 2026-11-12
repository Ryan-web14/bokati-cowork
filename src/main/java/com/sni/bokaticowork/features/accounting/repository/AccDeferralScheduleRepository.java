package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccDeferralSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccDeferralScheduleRepository extends JpaRepository<AccDeferralSchedule, Long> {
    Optional<AccDeferralSchedule> findByKindAndSourceTypeAndSourceLineId(String kind, String sourceType, Long sourceLineId);
    List<AccDeferralSchedule> findAllByKindOrderByIdAsc(String kind);
    List<AccDeferralSchedule> findAllByDocumentNumber(String documentNumber);
}
