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

    /** Lot B6 : entrees en attente sur un creneau a venir, la plus ancienne d'abord. */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking_waitlist_entry
            WHERE status = 'WAITING' AND started_at > :now
            ORDER BY created_at ASC
            LIMIT :limit
            """)
    List<BookingWaitlistEntry> findWaitingCandidates(@Param("now") java.time.LocalDateTime now, @Param("limit") int limit);

    /** Lot B6 : entrees en attente dont le creneau est passe. */
    @Query(nativeQuery = true, value = """
            SELECT * FROM booking_waitlist_entry
            WHERE status = 'WAITING' AND started_at <= :now
            LIMIT :limit
            """)
    List<BookingWaitlistEntry> findPastWaiting(@Param("now") java.time.LocalDateTime now, @Param("limit") int limit);

    /** Lot B6 : une offre est-elle deja en cours sur un creneau qui chevauche, pour la meme ressource ? */
    @Query(nativeQuery = true, value = """
            SELECT EXISTS (
              SELECT 1 FROM booking_waitlist_entry
              WHERE status = 'OFFERED' AND resource_id = :resourceId
                AND started_at < :endedAt AND ended_at > :startedAt)
            """)
    boolean existsOpenOffer(@Param("resourceId") Long resourceId, @Param("startedAt") java.time.LocalDateTime startedAt,
                            @Param("endedAt") java.time.LocalDateTime endedAt);

    /** Lot B6 : liste filtree et paginee. */
    @Query(nativeQuery = true, value = """
            SELECT w.* FROM booking_waitlist_entry w
            JOIN resource r ON r.id = w.resource_id
            WHERE (:status IS NULL OR w.status = :status)
              AND (:resourceCode IS NULL OR r.code = :resourceCode)
              AND (:ownerType IS NULL OR w.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR w.owner_code = :ownerCode)
            ORDER BY w.created_at ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM booking_waitlist_entry w
            JOIN resource r ON r.id = w.resource_id
            WHERE (:status IS NULL OR w.status = :status)
              AND (:resourceCode IS NULL OR r.code = :resourceCode)
              AND (:ownerType IS NULL OR w.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR w.owner_code = :ownerCode)
            """)
    org.springframework.data.domain.Page<BookingWaitlistEntry> search(@Param("status") String status, @Param("resourceCode") String resourceCode,
                                                                      @Param("ownerType") String ownerType, @Param("ownerCode") String ownerCode,
                                                                      org.springframework.data.domain.Pageable pageable);

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
