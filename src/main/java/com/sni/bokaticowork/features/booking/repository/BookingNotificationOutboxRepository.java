package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingNotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingNotificationOutboxRepository extends JpaRepository<BookingNotificationOutbox, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM booking_notification_outbox WHERE notification_number = :notificationNumber")
    Optional<BookingNotificationOutbox> findByNotificationNumber(@Param("notificationNumber") String notificationNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking_notification_outbox
            WHERE status = 'PENDING'
              AND scheduled_at <= :now
            ORDER BY scheduled_at ASC
            LIMIT :limit
            """)
    List<BookingNotificationOutbox> findDue(@Param("now") Instant now, @Param("limit") int limit);

    /** Lot B6 : reservations confirmees qui commencent dans la fenetre des rappels, avec un courriel de contact. */
    @Query(nativeQuery = true, value = """
            SELECT b.booking_number
            FROM booking b
            WHERE b.deleted = false AND b.status = 'CONFIRMED'
              AND b.contact_email IS NOT NULL AND b.contact_email <> ''
              AND b.started_at > :from AND b.started_at <= :to
            ORDER BY b.started_at ASC
            LIMIT :limit
            """)
    List<String> findBookingsToRemind(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to,
                                      @Param("limit") int limit);

    /** Lot B6 : un rappel est-il deja prevu a cette heure pour cette reservation ? */
    @Query(nativeQuery = true, value = """
            SELECT EXISTS (SELECT 1 FROM booking_notification_outbox
                           WHERE booking_id = :bookingId AND notification_type = 'REMINDER' AND scheduled_at = :scheduledAt)
            """)
    boolean existsReminder(@Param("bookingId") Long bookingId, @Param("scheduledAt") Instant scheduledAt);
}
