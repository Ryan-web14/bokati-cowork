package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.booking.model.BookingWaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BookingWaitlistEntryRepository extends JpaRepository<BookingWaitlistEntry, Long> {

    List<BookingWaitlistEntry> findAllByStatusOrderByCreatedAtAsc(BookingWaitlistStatus status);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking_waitlist_entry
            WHERE status = 'WAITING'
            ORDER BY created_at ASC
            LIMIT :limit
            """)
    List<BookingWaitlistEntry> findWaitingCandidates(@Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking_waitlist_entry
            WHERE status = 'OFFERED'
              AND expires_at IS NOT NULL
              AND expires_at <= :now
            ORDER BY expires_at ASC
            LIMIT :limit
            """)
    List<BookingWaitlistEntry> findExpiredOffers(@Param("now") Instant now, @Param("limit") int limit);
}
