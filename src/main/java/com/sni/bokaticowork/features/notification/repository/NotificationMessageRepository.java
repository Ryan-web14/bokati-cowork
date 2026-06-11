package com.sni.bokaticowork.features.notification.repository;

import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.model.NotificationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {

    Optional<NotificationMessage> findByNotificationNumber(String notificationNumber);

    List<NotificationMessage> findByStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(Set<NotificationDeliveryStatus> statuses,
                                                                                           Instant availableAt,
                                                                                           Pageable pageable);

    List<NotificationMessage> findByRecipientEmailAndStatusOrderBySentAtDesc(String recipientEmail,
                                                                              NotificationDeliveryStatus status,
                                                                              Pageable pageable);

    List<NotificationMessage> findByRecipientEmailAndReadAtIsNullAndStatusInOrderBySentAtDesc(String recipientEmail,
                                                                                               Set<NotificationDeliveryStatus> statuses,
                                                                                               Pageable pageable);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE notification_message
            SET read_at = :readAt, updated_at = :readAt
            WHERE notification_number = :notificationNumber
              AND read_at IS NULL
            """)
    int markOneAsRead(@Param("notificationNumber") String notificationNumber, @Param("readAt") Instant readAt);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE notification_message
            SET read_at = :readAt, updated_at = :readAt
            WHERE recipient_email = :recipientEmail
              AND read_at IS NULL
            """)
    int markAllAsRead(@Param("recipientEmail") String recipientEmail, @Param("readAt") Instant readAt);

    @Query(value = """
            SELECT message.*
            FROM notification_message message
            WHERE (CAST(:status AS varchar) IS NULL OR message.status = CAST(:status AS varchar))
              AND (CAST(:channel AS varchar) IS NULL OR message.channel = CAST(:channel AS varchar))
              AND (CAST(:eventType AS varchar) IS NULL OR message.event_type = CAST(:eventType AS varchar))
              AND (CAST(:recipientEmail AS varchar) IS NULL OR message.recipient_email = CAST(:recipientEmail AS varchar))
              AND (
                    CAST(:search AS varchar) IS NULL
                    OR message.notification_number ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR message.recipient_email ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR message.recipient_name ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR message.event_type ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR message.aggregate_id ILIKE CONCAT('%', CAST(:search AS varchar), '%')
              )
            ORDER BY message.created_at DESC
            """,
            countQuery = """
            SELECT count(*)
            FROM notification_message message
            WHERE (CAST(:status AS varchar) IS NULL OR message.status = CAST(:status AS varchar))
              AND (CAST(:channel AS varchar) IS NULL OR message.channel = CAST(:channel AS varchar))
              AND (CAST(:eventType AS varchar) IS NULL OR message.event_type = CAST(:eventType AS varchar))
              AND (CAST(:recipientEmail AS varchar) IS NULL OR message.recipient_email = CAST(:recipientEmail AS varchar))
              AND (
                    CAST(:search AS varchar) IS NULL
                    OR message.notification_number ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR message.recipient_email ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR message.recipient_name ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR message.event_type ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR message.aggregate_id ILIKE CONCAT('%', CAST(:search AS varchar), '%')
              )
            """,
            nativeQuery = true)
    Page<NotificationMessage> search(String status,
                                     String channel,
                                     String eventType,
                                     String recipientEmail,
                                     String search,
                                     Pageable pageable);
}
