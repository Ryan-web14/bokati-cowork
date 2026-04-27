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
}
