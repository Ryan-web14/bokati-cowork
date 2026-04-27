package com.sni.bokaticowork.features.subscription.notification.repository;

import com.sni.bokaticowork.features.subscription.notification.model.SubscriptionNotificationOutbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionNotificationOutboxRepository extends JpaRepository<SubscriptionNotificationOutbox, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_notification_outbox WHERE notification_number = :notificationNumber")
    Optional<SubscriptionNotificationOutbox> findByNotificationNumber(@Param("notificationNumber") String notificationNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_notification_outbox
            WHERE status = 'PENDING'
              AND scheduled_at <= :now
            ORDER BY scheduled_at ASC, created_at ASC
            LIMIT :limit
            """)
    List<SubscriptionNotificationOutbox> findDueNotifications(@Param("now") Instant now, @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            SELECT sno.*
            FROM subscription_notification_outbox sno
            LEFT JOIN subscription s ON s.id = sno.subscription_id
            WHERE (:subscriptionNumber IS NULL OR s.subscription_number = :subscriptionNumber)
              AND (:ownerType IS NULL OR sno.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR sno.owner_code = :ownerCode)
              AND (:notificationType IS NULL OR sno.notification_type = :notificationType)
              AND (:status IS NULL OR sno.status = :status)
            """,
            countQuery = """
            SELECT count(*)
            FROM subscription_notification_outbox sno
            LEFT JOIN subscription s ON s.id = sno.subscription_id
            WHERE (:subscriptionNumber IS NULL OR s.subscription_number = :subscriptionNumber)
              AND (:ownerType IS NULL OR sno.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR sno.owner_code = :ownerCode)
              AND (:notificationType IS NULL OR sno.notification_type = :notificationType)
              AND (:status IS NULL OR sno.status = :status)
            """)
    Page<SubscriptionNotificationOutbox> search(@Param("subscriptionNumber") String subscriptionNumber,
                                                @Param("ownerType") String ownerType,
                                                @Param("ownerCode") String ownerCode,
                                                @Param("notificationType") String notificationType,
                                                @Param("status") String status,
                                                Pageable pageable);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE subscription_notification_outbox
            SET status = 'CANCELLED',
                updated_at = now()
            WHERE status = 'PENDING'
              AND notification_number = :notificationNumber
            """)
    int cancelPending(@Param("notificationNumber") String notificationNumber);
}
