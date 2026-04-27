package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM payment_schedule WHERE schedule_number = :scheduleNumber")
    Optional<PaymentSchedule> findByScheduleNumber(@Param("scheduleNumber") String scheduleNumber);

    @Query(nativeQuery = true, value = """
            SELECT * FROM payment_schedule
            WHERE billing_document_number = :documentNumber AND status = 'ACTIVE'
            LIMIT 1
            """)
    Optional<PaymentSchedule> findActiveByDocumentNumber(@Param("documentNumber") String documentNumber);

    @Query(nativeQuery = true, value = """
            SELECT * FROM payment_schedule
            WHERE billing_document_number = :documentNumber
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<PaymentSchedule> findLatestByDocumentNumber(@Param("documentNumber") String documentNumber);
}