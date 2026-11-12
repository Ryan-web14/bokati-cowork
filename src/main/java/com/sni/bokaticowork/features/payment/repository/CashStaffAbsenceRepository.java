package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashStaffAbsenceRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashStaffAbsence, Long> {

    Optional<com.sni.bokaticowork.features.payment.model.CashStaffAbsence> findByAbsenceNumber(String absenceNumber);

    @Query("SELECT a FROM CashStaffAbsence a WHERE lower(a.userEmail) = lower(:userEmail) ORDER BY a.startsOn DESC")
    List<com.sni.bokaticowork.features.payment.model.CashStaffAbsence> findByUser(@Param("userEmail") String userEmail);

    List<com.sni.bokaticowork.features.payment.model.CashStaffAbsence> findByStatusInOrderByStartsOnAsc(java.util.Collection<com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus> statuses);
}
