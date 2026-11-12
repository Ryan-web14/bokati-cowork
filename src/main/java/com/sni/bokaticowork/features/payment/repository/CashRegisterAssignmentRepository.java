package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashRegisterAssignmentRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashRegisterAssignment, Long> {

    List<com.sni.bokaticowork.features.payment.model.CashRegisterAssignment> findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(String registerCode);

    @Query("SELECT a FROM CashRegisterAssignment a WHERE lower(a.userEmail) = lower(:userEmail) AND a.revokedAt IS NULL ORDER BY a.validFrom DESC")
    List<com.sni.bokaticowork.features.payment.model.CashRegisterAssignment> findLiveByUser(@Param("userEmail") String userEmail);
}
