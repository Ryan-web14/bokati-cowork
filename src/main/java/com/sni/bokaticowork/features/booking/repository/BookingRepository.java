package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM booking WHERE booking_number = :bookingNumber AND deleted = false)")
    boolean existsByBookingNumber(@Param("bookingNumber") String bookingNumber);

    @Query(nativeQuery = true, value = "SELECT * FROM booking WHERE booking_number = :bookingNumber AND deleted = false")
    Optional<Booking> findByBookingNumber(@Param("bookingNumber") String bookingNumber);

    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.resource r
            LEFT JOIN FETCH r.resourceType
            WHERE b.bookingNumber = :bookingNumber
              AND b.deleted = false
            """)
    Optional<Booking> findPublicByBookingNumberWithResource(@Param("bookingNumber") String bookingNumber);

    /** Reservation portant cet element facturable, pour remonter d'une facture vers sa reservation. */
    @Query(nativeQuery = true, value = "SELECT * FROM booking WHERE billable_number = :billableNumber AND deleted = false")
    Optional<Booking> findByBillableNumber(@Param("billableNumber") String billableNumber);

    /** Les reservations d'un lot multi-ressources, ressource chargee pour la reponse. */
    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.resource r
            LEFT JOIN FETCH r.resourceType
            LEFT JOIN FETCH r.resourceGroup
            WHERE b.batchNumber = :batchNumber
              AND b.deleted = false
            ORDER BY b.ownerCode, b.startedAt, b.id
            """)
    List<Booking> findAllByBatchNumber(@Param("batchNumber") String batchNumber);

    /** Lot B1 : reservations en attente de paiement dont l'echeance est passee. */
    @Query(nativeQuery = true, value = """
            SELECT * FROM booking
            WHERE deleted = false AND status = 'PENDING_PAYMENT'
              AND payment_due_at IS NOT NULL AND payment_due_at < :now
            ORDER BY payment_due_at
            LIMIT :limit
            """)
    List<Booking> findUnpaidPastDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Query(nativeQuery = true, value = "SELECT * FROM booking WHERE idempotency_key = :idempotencyKey AND deleted = false")
    Optional<Booking> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Query(nativeQuery = true, value = "SELECT * FROM booking WHERE check_in_token = :checkInToken AND deleted = false")
    Optional<Booking> findByCheckInToken(@Param("checkInToken") String checkInToken);

    /** Lot R2 : reservations actives d'une ressource qui chevauchent une plage. */
    @Query(nativeQuery = true, value = """
            SELECT b.*
            FROM booking b
            WHERE b.resource_id = :resourceId
              AND b.deleted = false
              AND b.status IN ('CONFIRMED', 'IN_PROGRESS', 'PENDING_APPROVAL', 'PENDING_PAYMENT')
              AND b.started_at < :endedAt
              AND b.ended_at > :startedAt
            ORDER BY b.started_at
            """)
    java.util.List<com.sni.bokaticowork.features.booking.model.Booking> findActiveOnResource(@Param("resourceId") Long resourceId,
                                                                                         @Param("startedAt") LocalDateTime startedAt,
                                                                                         @Param("endedAt") LocalDateTime endedAt);

    @Query(nativeQuery = true, value = """
            SELECT EXISTS(
                SELECT 1
                FROM booking b
                WHERE b.resource_id = :resourceId
                  AND b.deleted = false
                  AND (b.status IN ('CONFIRMED', 'IN_PROGRESS')
                       OR (b.status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL') AND b.slots_reserved = true))
                  AND b.started_at < :endedAt
                  AND b.ended_at > :startedAt
                  AND (:excludedBookingNumber IS NULL OR b.booking_number <> :excludedBookingNumber)
            )
            """)
    boolean existsActiveConflict(@Param("resourceId") Long resourceId,
                                 @Param("startedAt") LocalDateTime startedAt,
                                 @Param("endedAt") LocalDateTime endedAt,
                                 @Param("excludedBookingNumber") String excludedBookingNumber);

    @Query(value = """
            SELECT b.*
            FROM booking b
            JOIN resource r ON r.id = b.resource_id
            LEFT JOIN resource_type rt ON rt.id = r.type_id
            LEFT JOIN resource_group rg ON rg.id = r.group_id
            WHERE b.deleted = false
              AND (:ownerType IS NULL OR b.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR b.owner_code = :ownerCode)
              AND (:resourceCode IS NULL OR r.code = :resourceCode)
              AND (:status IS NULL OR b.status = :status)
              AND (CAST(:startedFrom AS timestamp) IS NULL OR b.started_at >= :startedFrom)
              AND (CAST(:startedTo AS timestamp) IS NULL OR b.started_at <= :startedTo)
              AND (
                    :query IS NULL
                    OR b.booking_number    ILIKE '%' || :query || '%'
                    OR b.owner_code        ILIKE '%' || :query || '%'
                    OR COALESCE(b.contact_name,  '') ILIKE '%' || :query || '%'
                    OR COALESCE(b.contact_email, '') ILIKE '%' || :query || '%'
                    OR r.code              ILIKE '%' || :query || '%'
                    OR r.name              ILIKE '%' || :query || '%'
                    OR normalize_text(b.booking_number) % normalize_text(:query)
                    OR normalize_text(b.owner_code) % normalize_text(:query)
                    OR normalize_text(COALESCE(b.contact_name,  '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(b.contact_email, '')) % normalize_text(:query)
                    OR normalize_text(r.code) % normalize_text(:query)
                    OR normalize_text(r.name) % normalize_text(:query)
                    OR normalize_text(COALESCE(rt.code, '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(rg.code, '')) % normalize_text(:query)
              )
            ORDER BY
              CASE WHEN :sortKey = 'RELEVANCE' AND :query IS NOT NULL THEN GREATEST(
                    similarity(normalize_text(b.booking_number), normalize_text(:query)),
                    similarity(normalize_text(b.owner_code), normalize_text(:query)),
                    similarity(normalize_text(COALESCE(b.contact_name, '')), normalize_text(:query)),
                    similarity(normalize_text(COALESCE(b.contact_email, '')), normalize_text(:query)),
                    similarity(normalize_text(r.code), normalize_text(:query)),
                    similarity(normalize_text(r.name), normalize_text(:query))
              ) END DESC,
              CASE WHEN :sortKey = 'STARTED_AT' AND :sortDir = 'ASC' THEN b.started_at END ASC,
              CASE WHEN :sortKey = 'STARTED_AT' AND :sortDir = 'DESC' THEN b.started_at END DESC,
              CASE WHEN :sortKey = 'ENDED_AT' AND :sortDir = 'ASC' THEN b.ended_at END ASC,
              CASE WHEN :sortKey = 'ENDED_AT' AND :sortDir = 'DESC' THEN b.ended_at END DESC,
              CASE WHEN :sortKey = 'CREATED_AT' AND :sortDir = 'ASC' THEN b.created_at END ASC,
              CASE WHEN :sortKey = 'CREATED_AT' AND :sortDir = 'DESC' THEN b.created_at END DESC,
              CASE WHEN :sortKey = 'TOTAL_AMOUNT' AND :sortDir = 'ASC' THEN b.total_amount END ASC,
              CASE WHEN :sortKey = 'TOTAL_AMOUNT' AND :sortDir = 'DESC' THEN b.total_amount END DESC,
              CASE WHEN :sortKey = 'STATUS' AND :sortDir = 'ASC' THEN b.status END ASC,
              CASE WHEN :sortKey = 'STATUS' AND :sortDir = 'DESC' THEN b.status END DESC,
              CASE WHEN :sortKey = 'BOOKING_NUMBER' AND :sortDir = 'ASC' THEN b.booking_number END ASC,
              CASE WHEN :sortKey = 'BOOKING_NUMBER' AND :sortDir = 'DESC' THEN b.booking_number END DESC,
              CASE WHEN :sortKey = 'RESOURCE_CODE' AND :sortDir = 'ASC' THEN r.code END ASC,
              CASE WHEN :sortKey = 'RESOURCE_CODE' AND :sortDir = 'DESC' THEN r.code END DESC,
              CASE WHEN :sortKey = 'CONTACT_NAME' AND :sortDir = 'ASC' THEN b.contact_name END ASC,
              CASE WHEN :sortKey = 'CONTACT_NAME' AND :sortDir = 'DESC' THEN b.contact_name END DESC,
              b.started_at DESC,
              b.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM booking b
            JOIN resource r ON r.id = b.resource_id
            LEFT JOIN resource_type rt ON rt.id = r.type_id
            LEFT JOIN resource_group rg ON rg.id = r.group_id
            WHERE b.deleted = false
              AND (:ownerType IS NULL OR b.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR b.owner_code = :ownerCode)
              AND (:resourceCode IS NULL OR r.code = :resourceCode)
              AND (:status IS NULL OR b.status = :status)
              AND (CAST(:startedFrom AS timestamp) IS NULL OR b.started_at >= :startedFrom)
              AND (CAST(:startedTo AS timestamp) IS NULL OR b.started_at <= :startedTo)
              AND (
                    :query IS NULL
                    OR b.booking_number    ILIKE '%' || :query || '%'
                    OR b.owner_code        ILIKE '%' || :query || '%'
                    OR COALESCE(b.contact_name,  '') ILIKE '%' || :query || '%'
                    OR COALESCE(b.contact_email, '') ILIKE '%' || :query || '%'
                    OR r.code              ILIKE '%' || :query || '%'
                    OR r.name              ILIKE '%' || :query || '%'
                    OR normalize_text(b.booking_number) % normalize_text(:query)
                    OR normalize_text(b.owner_code) % normalize_text(:query)
                    OR normalize_text(COALESCE(b.contact_name,  '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(b.contact_email, '')) % normalize_text(:query)
                    OR normalize_text(r.code) % normalize_text(:query)
                    OR normalize_text(r.name) % normalize_text(:query)
                    OR normalize_text(COALESCE(rt.code, '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(rg.code, '')) % normalize_text(:query)
              )
            """,
            nativeQuery = true)
    Page<Booking> nativeSearch(@Param("query") String query,
                               @Param("ownerType") String ownerType,
                               @Param("ownerCode") String ownerCode,
                               @Param("resourceCode") String resourceCode,
                               @Param("status") String status,
                               @Param("startedFrom") LocalDateTime startedFrom,
                               @Param("startedTo") LocalDateTime startedTo,
                               @Param("sortKey") String sortKey,
                               @Param("sortDir") String sortDir,
                               Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT b.*
            FROM search_booking(:query) sb
            JOIN booking b ON b.id = sb.id
            WHERE b.deleted = false
            ORDER BY sb.score DESC
            LIMIT 20
            """)
    List<Booking> basicSearch(@Param("query") String query);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM booking
            WHERE deleted = false
              AND owner_type = :ownerType
              AND owner_code = :ownerCode
              AND status IN ('CONFIRMED', 'IN_PROGRESS', 'PENDING_APPROVAL', 'PENDING_PAYMENT')
            """)
    long countActiveForOwner(@Param("ownerType") String ownerType, @Param("ownerCode") String ownerCode);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM booking
            WHERE deleted = false
              AND owner_type = :ownerType
              AND owner_code = :ownerCode
              AND started_at >= :from
              AND started_at < :to
              AND status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED')
            """)
    long countForOwnerBetween(@Param("ownerType") String ownerType,
                              @Param("ownerCode") String ownerCode,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM booking
            WHERE deleted = false
              AND owner_type = :ownerType
              AND owner_code = :ownerCode
              AND status = 'NO_SHOW'
              AND started_at >= :from
              AND started_at < :to
            """)
    long countNoShowsForOwnerBetween(@Param("ownerType") String ownerType,
                                     @Param("ownerCode") String ownerCode,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking
            WHERE deleted = false
              AND status IN ('CONFIRMED', 'IN_PROGRESS')
              AND (
                    checked_in_at IS NULL
                    OR (payment_mode <> 'DIRECT' AND entitlement_code IS NOT NULL AND completed_at IS NULL AND status = 'COMPLETED')
              )
            ORDER BY started_at DESC
            LIMIT :limit
            """)
    List<Booking> findRepairCandidates(@Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM booking
            WHERE deleted = false
              AND status IN ('CONFIRMED', 'IN_PROGRESS')
              AND started_at <= :now
              AND ended_at >= :now
            """)
    long countActiveAt(@Param("now") LocalDateTime now);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM booking
            WHERE deleted = false
              AND status = 'IN_PROGRESS'
              AND checked_in_at IS NOT NULL
            """)
    long countCheckedInNow();

    @Query(nativeQuery = true, value = """
            SELECT COUNT(DISTINCT resource_id)
            FROM booking
            WHERE deleted = false
              AND status IN ('CONFIRMED', 'IN_PROGRESS')
              AND started_at <= :now
              AND ended_at >= :now
            """)
    long countOccupiedResourcesAt(@Param("now") LocalDateTime now);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking
            WHERE deleted = false
              AND status IN ('CONFIRMED', 'IN_PROGRESS')
              AND ended_at < :now
              AND checked_in_at IS NULL
              AND started_event_at IS NULL
            ORDER BY ended_at ASC
            LIMIT :limit
            """)
    List<Booking> findOverdueNoShowCandidates(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * <p>CONFIRMED est admis a cote de IN_PROGRESS : une reservation pointee d'avance reste
     * CONFIRMED jusqu'a son heure, et si le balayage n'a pas tourne d'ici la fin · application
     * arretee sur la plage, par exemple · elle ne correspondrait sinon ni a cette requete ni a
     * celle des absences, et resterait indefiniment CONFIRMED. Les deux requetes se partagent
     * ainsi exactement les creneaux depasses, selon qu'un pointage a eu lieu ou non.
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking
            WHERE deleted = false
              AND status IN ('CONFIRMED', 'IN_PROGRESS')
              AND ended_at < :now
              AND (checked_in_at IS NOT NULL OR started_event_at IS NOT NULL)
            ORDER BY ended_at ASC
            LIMIT :limit
            """)
    List<Booking> findOverdueCompletionCandidates(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * Reservations pointees d'avance dont l'heure est arrivee.
     *
     * <p>Un pointage quinze minutes avant l'heure n'ouvre pas la salle pour autant : la
     * reservation reste CONFIRMED et n'entre en cours qu'a l'heure dite. Sans ce rattrapage elle y
     * resterait, puis serait clôturee sans jamais avoir ete en cours.
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking
            WHERE deleted = false
              AND status = 'CONFIRMED'
              AND started_at <= :now
              AND ended_at > :now
              AND checked_in_at IS NOT NULL
            ORDER BY started_at ASC
            LIMIT :limit
            """)
    List<Booking> findCheckedInAwaitingStart(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** Les reservations a venir prises sur ces places · a signaler quand une place est liberee. */
    @Query("""
            SELECT b.bookingNumber FROM Booking b
            WHERE b.subscriptionSeatId IN :seatIds
              AND b.startedAt > :now
              AND b.status IN (com.sni.bokaticowork.features.booking.enums.BookingStatus.PENDING_APPROVAL,
                               com.sni.bokaticowork.features.booking.enums.BookingStatus.PENDING_PAYMENT,
                               com.sni.bokaticowork.features.booking.enums.BookingStatus.CONFIRMED)
            ORDER BY b.startedAt ASC
            """)
    java.util.List<String> findFutureBookingNumbersOnSeats(@org.springframework.data.repository.query.Param("seatIds") java.util.Collection<Long> seatIds,
                                                           @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    /** Les reservations a venir d'une personne sur un pass · a signaler quand son acces est revoque. */
    @Query("""
            SELECT b.bookingNumber FROM Booking b
            WHERE b.passNumber = :passNumber
              AND b.ownerCode = :ownerCode
              AND b.startedAt > :now
              AND b.status IN (com.sni.bokaticowork.features.booking.enums.BookingStatus.PENDING_APPROVAL,
                               com.sni.bokaticowork.features.booking.enums.BookingStatus.PENDING_PAYMENT,
                               com.sni.bokaticowork.features.booking.enums.BookingStatus.CONFIRMED)
            ORDER BY b.startedAt ASC
            """)
    java.util.List<String> findFutureBookingNumbersOnPass(@org.springframework.data.repository.query.Param("passNumber") String passNumber,
                                                          @org.springframework.data.repository.query.Param("ownerCode") String ownerCode,
                                                          @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
