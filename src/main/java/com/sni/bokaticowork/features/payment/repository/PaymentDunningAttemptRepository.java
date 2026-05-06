package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.enums.DunningAttemptStatus;
import com.sni.bokaticowork.features.payment.model.PaymentDunningAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PaymentDunningAttemptRepository extends JpaRepository<PaymentDunningAttempt, Long> {

    @Query("SELECT a FROM PaymentDunningAttempt a WHERE a.status = 'PENDING' AND a.scheduledAt <= :now")
    List<PaymentDunningAttempt> findDuePending(@Param("now") Instant now);

    boolean existsByPaymentIntentIdAndStatusIn(Long paymentIntentId, List<DunningAttemptStatus> statuses);

    List<PaymentDunningAttempt> findAllByPaymentIntentIdOrderByAttemptNumber(Long paymentIntentId);
}