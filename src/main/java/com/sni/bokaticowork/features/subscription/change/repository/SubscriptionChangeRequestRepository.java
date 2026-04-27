package com.sni.bokaticowork.features.subscription.change.repository;

import com.sni.bokaticowork.features.subscription.change.model.SubscriptionChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionChangeRequestRepository extends JpaRepository<SubscriptionChangeRequest, Long>, JpaSpecificationExecutor<SubscriptionChangeRequest> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_change_request WHERE change_number = :changeNumber")
    Optional<SubscriptionChangeRequest> findByChangeNumber(@Param("changeNumber") String changeNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_change_request
            WHERE status = 'APPROVED'
              AND effective_policy = 'NEXT_BILLING_PERIOD'
              AND effective_date <= :date
            ORDER BY effective_date ASC
            """)
    List<SubscriptionChangeRequest> findApprovedDueChanges(@Param("date") LocalDate date);
}
