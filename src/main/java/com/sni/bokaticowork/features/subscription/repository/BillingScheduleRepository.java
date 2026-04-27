package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.BillingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingScheduleRepository extends JpaRepository<BillingSchedule, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM billing_schedule WHERE subscription_id = :subscriptionId")
    Optional<BillingSchedule> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_schedule
            WHERE status = 'ACTIVE'
              AND next_billing_date <= :date
            ORDER BY next_billing_date ASC
            """)
    List<BillingSchedule> findDueSchedules(@Param("date") LocalDate date);
}
