package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long>, JpaSpecificationExecutor<Subscription> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription WHERE subscription_number = :subscriptionNumber")
    Optional<Subscription> findBySubscriptionNumber(@Param("subscriptionNumber") String subscriptionNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription
            WHERE subscriber_type = :subscriberType
              AND subscriber_code = :subscriberCode
              AND status = 'ACTIVE'
              AND current_period_start <= :today
              AND (current_period_end IS NULL OR current_period_end >= :today)
            ORDER BY current_period_start DESC, created_at DESC
            LIMIT 1
            """)
    Optional<Subscription> findCurrentActive(@Param("subscriberType") String subscriberType,
                                             @Param("subscriberCode") String subscriberCode,
                                             @Param("today") LocalDate today);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription
            WHERE subscriber_type = :subscriberType
              AND subscriber_code = :subscriberCode
              AND status = 'ACTIVE'
              AND current_period_start <= :today
              AND (current_period_end IS NULL OR current_period_end >= :today)
            ORDER BY current_period_start DESC, created_at DESC
            """)
    List<Subscription> findAllCurrentActive(@Param("subscriberType") String subscriberType,
                                            @Param("subscriberCode") String subscriberCode,
                                            @Param("today") LocalDate today);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription
            WHERE status = :status
              AND next_billing_date <= :nextBillingDate
              AND auto_renew = true
            ORDER BY next_billing_date ASC
            """)
    List<Subscription> findAllByStatusAndNextBillingDateLessThanEqualAndAutoRenewTrue(
            @Param("status") String status,
            @Param("nextBillingDate") LocalDate nextBillingDate
    );

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription
            WHERE cancel_at_period_end = true
              AND current_period_end <= :currentPeriodEnd
              AND status = :status
            ORDER BY current_period_end ASC
            """)
    List<Subscription> findAllByCancelAtPeriodEndTrueAndCurrentPeriodEndLessThanEqualAndStatus(
            @Param("currentPeriodEnd") LocalDate currentPeriodEnd,
            @Param("status") String status
    );

    @Query(nativeQuery = true, value = """
            SELECT s.*
            FROM subscription s
            WHERE s.status = 'ACTIVE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM entitlement_grant eg
                  WHERE eg.subscription_id = s.id
                    AND eg.pass_id IS NULL
                    AND eg.status = 'ACTIVE'
                    AND eg.valid_from <= NOW()
                    AND (eg.valid_until IS NULL OR eg.valid_until >= NOW())
                    AND (eg.unlimited = true OR eg.quantity_remaining > 0)
              )
            ORDER BY s.created_at ASC
            """)
    List<Subscription> findActiveSubscriptionsWithoutGrants();



    @Query(nativeQuery = true, value = "SELECT * FROM subscription WHERE subscriber_code = :subscriberCode AND plan_version_id = :planVersion AND status NOT IN ('CANCELLED', 'EXPIRED')")
    List<Subscription> findBySuscriberCodeAndPlanVersion(@Param("subscriberCode") String subscriberCode, @Param("planVersion") Long planVersion);

    List<Subscription> findAllByContractCode(String contractCode);

    /** Les abonnements en cours sur une version de plan · ceux qu'une hausse de catalogue toucherait. */
    @Query(nativeQuery = true, value = """
            SELECT * FROM subscription
            WHERE plan_version_id = :planVersionId
              AND status IN ('ACTIVE', 'TRIALING', 'PAST_DUE', 'PAUSED', 'PENDING_ACTIVATION', 'GRACE_PERIOD', 'PENDING_TERMINATION', 'PENDING_DOCUMENTS')
            ORDER BY subscription_number
            """)
    List<Subscription> findAllOpenOnPlanVersion(@Param("planVersionId") Long planVersionId);

    /**
     * Les abonnements dont la periode est echue et que rien n'a fait avancer.
     *
     * <p>Trois portes menaient a cet etat, et aucune ne se refermait : le renouvellement ne prend
     * que les abonnements en reconduction automatique, la cloture de fin de periode ne prend que
     * ceux qui l'avaient demandee, et la tolerance ne voit que ceux qui portent une facture de
     * renouvellement impayee. Un abonnement sans reconduction, ou dont le renouvellement a
     * echoue, n'etait donc ni facture, ni clos · il restait ACTIVE indefiniment, droits ouverts,
     * des mois apres la fin de sa periode.</p>
     *
     * <p>On ne retient que les abonnements encore en cours dont la periode est derriere nous. Un
     * preavis de resiliation en cours est laisse tranquille : il a sa propre date d'effet.</p>
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription
            WHERE status IN ('ACTIVE', 'TRIALING', 'GRACE_PERIOD', 'PAST_DUE')
              AND current_period_end IS NOT NULL
              AND current_period_end < :today
            ORDER BY current_period_end ASC
            """)
    List<Subscription> findEndedPeriodsStillOpen(@Param("today") LocalDate today);

    /**
     * Les abonnements actifs dont une echeance de renouvellement est impayee depuis plus de
     * {@code unpaidSince} · candidats a la tolerance avant suspension.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT s.*
            FROM subscription s
            JOIN billable_item bi ON bi.source_id = s.subscription_number AND bi.source_type = 'SUBSCRIPTION_RENEWAL'
            JOIN billing_document bd ON bd.id = bi.invoice_id
            WHERE s.status = 'ACTIVE'
              AND bd.balance_due > 0
              AND bd.status IN ('ISSUED', 'VALIDATED', 'SENT', 'VIEWED', 'PARTIALLY_PAID', 'OVERDUE')
              AND bi.created_at < :unpaidSince
            """)
    List<Subscription> findActiveWithRenewalUnpaidSince(@Param("unpaidSince") Instant unpaidSince);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription WHERE status = :status ORDER BY updated_at ASC")
    List<Subscription> findAllByStatus(@Param("status") String status);

    /** Les abonnements ouverts d'un souscripteur · pour aligner leurs echeances. */
    @Query(nativeQuery = true, value = """
            SELECT * FROM subscription
            WHERE subscriber_type = :subscriberType AND subscriber_code = :subscriberCode
              AND status IN ('ACTIVE', 'TRIALING', 'PAST_DUE', 'GRACE_PERIOD')
            ORDER BY current_period_end ASC
            """)
    List<Subscription> findOpenBySubscriber(@Param("subscriberType") String subscriberType, @Param("subscriberCode") String subscriberCode);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription WHERE subscriber_type = :subscriberType AND subscriber_code = :subscriberCode ORDER BY created_at DESC")
    List<Subscription> findAllBySubscriber(@Param("subscriberType") String subscriberType, @Param("subscriberCode") String subscriberCode);

    @Query(nativeQuery = true, value = """
            SELECT id
            FROM subscription
            WHERE contract_code IS NULL
              AND created_at <= :createdBefore
              AND status NOT IN ('DRAFT', 'CANCELLED', 'EXPIRED')
            ORDER BY created_at ASC
            LIMIT :limit
            """)
    List<Long> findIdsMissingContract(@Param("createdBefore") Instant createdBefore,
                                      @Param("limit") int limit);


}

