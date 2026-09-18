package com.sni.bokaticowork.features.subscription.lifecycle.repository;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionQuoteRepository extends JpaRepository<SubscriptionQuote, Long> {

    Optional<SubscriptionQuote> findByQuoteNumber(String quoteNumber);

    List<SubscriptionQuote> findBySubscriberTypeAndSubscriberCodeOrderByCreatedAtDesc(
            com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType subscriberType, String subscriberCode);

    org.springframework.data.domain.Page<SubscriptionQuote> findByStatusOrderByCreatedAtDesc(SubscriptionQuote.Status status, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<SubscriptionQuote> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    /** Les devis ouverts dont la validite est passee. */
    @Query(nativeQuery = true, value = "SELECT * FROM subscription_quote WHERE status IN ('DRAFT', 'SENT') AND valid_until < :day")
    List<SubscriptionQuote> findExpiredOpen(@Param("day") LocalDate day);
}
