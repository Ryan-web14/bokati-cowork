package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingHoldRepository extends JpaRepository<BookingHold, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM booking_hold WHERE hold_number = :holdNumber")
    Optional<BookingHold> findByHoldNumber(@Param("holdNumber") String holdNumber);

    @Query(nativeQuery = true, value = "SELECT * FROM booking_hold WHERE idempotency_key = :idempotencyKey")
    Optional<BookingHold> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Query(nativeQuery = true, value = """
            SELECT EXISTS(
                SELECT 1 FROM booking_hold
                WHERE resource_id = :resourceId
                  AND status = 'ACTIVE'
                  AND expires_at > now()
                  AND started_at < :endedAt
                  AND ended_at > :startedAt
                  AND (:excludedHoldNumber IS NULL OR hold_number <> :excludedHoldNumber)
            )
            """)
    boolean existsActiveOverlap(@Param("resourceId") Long resourceId,
                                @Param("startedAt") LocalDateTime startedAt,
                                @Param("endedAt") LocalDateTime endedAt,
                                @Param("excludedHoldNumber") String excludedHoldNumber);

    @Query(nativeQuery = true, value = "SELECT * FROM booking_hold WHERE status = 'ACTIVE' AND expires_at <= :now ORDER BY expires_at ASC LIMIT :limit")
    List<BookingHold> findExpired(@Param("now") Instant now, @Param("limit") int limit);
}
