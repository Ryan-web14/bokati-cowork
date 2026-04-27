package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.PaymentScheduleInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentScheduleInstallmentRepository extends JpaRepository<PaymentScheduleInstallment, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM payment_schedule_installment WHERE installment_number = :number")
    Optional<PaymentScheduleInstallment> findByInstallmentNumber(@Param("number") String number);

    @Query(nativeQuery = true, value = """
            SELECT * FROM payment_schedule_installment
            WHERE schedule_id = :scheduleId
            ORDER BY installment_order ASC
            """)
    List<PaymentScheduleInstallment> findByScheduleIdOrdered(@Param("scheduleId") Long scheduleId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE payment_schedule_installment
            SET status = 'OVERDUE', updated_at = NOW()
            WHERE status = 'PENDING'
              AND due_date < :today
            """)
    int markOverdue(@Param("today") LocalDate today);
}