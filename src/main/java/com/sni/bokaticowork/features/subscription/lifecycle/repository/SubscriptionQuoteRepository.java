package com.sni.bokaticowork.features.subscription.lifecycle.repository;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /** Le plan est lu avec le devis : la vue du devis le lit hors transaction. */
    @EntityGraph(attributePaths = {"planVersion", "planVersion.plan"})
    Optional<SubscriptionQuote> findByQuoteNumber(String quoteNumber);

    /** Le plan est lu avec le devis : la vue du devis le lit hors transaction. */
    @EntityGraph(attributePaths = {"planVersion", "planVersion.plan"})
    List<SubscriptionQuote> findBySubscriberTypeAndSubscriberCodeOrderByCreatedAtDesc(
            com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType subscriberType, String subscriberCode);

    /** Le plan est lu avec le devis : la vue du devis le lit hors transaction. */
    @EntityGraph(attributePaths = {"planVersion", "planVersion.plan"})
    org.springframework.data.domain.Page<SubscriptionQuote> findByStatusOrderByCreatedAtDesc(SubscriptionQuote.Status status, org.springframework.data.domain.Pageable pageable);

    /** Le plan est lu avec le devis : la vue du devis le lit hors transaction. */
    @EntityGraph(attributePaths = {"planVersion", "planVersion.plan"})
    org.springframework.data.domain.Page<SubscriptionQuote> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    /** Les devis ouverts dont la validite est passee. */
    @Query(nativeQuery = true, value = "SELECT * FROM subscription_quote WHERE status IN ('DRAFT', 'SENT') AND valid_until < :day")
    List<SubscriptionQuote> findExpiredOpen(@Param("day") LocalDate day);
}
