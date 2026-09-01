package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResourceAvailabilityRepository extends JpaRepository<ResourceAvailability, Long> {

    /**
     * Creneaux encore a venir pour une ressource · gouverne le changement de duree de creneau.
     * Tant qu'il en reste, changer la duree laisserait la ressource avec des creneaux melanges.
     */
    long countByResourceAndEndedAtAfter(Resource resource, java.time.LocalDateTime moment);

    /** Creneaux d'une ressource compris dans une plage · sert au compte rendu d'un appel groupe. */
    long countByResourceAndStartedAtGreaterThanEqualAndEndedAtLessThanEqual(
            Resource resource, java.time.LocalDateTime from, java.time.LocalDateTime to);

    Page<ResourceAvailability> findAllByResource(Resource resource, Pageable pageable);

    Page<ResourceAvailability> findAllByEndedAtAfter(LocalDateTime cutoff, Pageable pageable);

    Page<ResourceAvailability> findAllByResourceAndEndedAtAfter(Resource resource, LocalDateTime cutoff, Pageable pageable);

    @Query("""
            select case when count(ra) > 0 then true else false end
            from ResourceAvailability ra
            where ra.resource = :resource
              and ra.active = true
              and ra.startedAt < :endedAt
              and ra.endedAt > :startedAt
            """)
    boolean existsActiveOverlap(
            @Param("resource") Resource resource,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("endedAt") LocalDateTime endedAt
    );

    @Query("""
            select ra
            from ResourceAvailability ra
            join fetch ra.resource resource
            order by resource.code asc, ra.startedAt asc
            """)
    List<ResourceAvailability> findAllForGroupedView();

    @Query("""
            select ra
            from ResourceAvailability ra
            join fetch ra.resource resource
            where ra.endedAt > :cutoff
            order by resource.code asc, ra.startedAt asc
            """)
    List<ResourceAvailability> findFutureForGroupedView(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            select ra
            from ResourceAvailability ra
            where ra.resource = :resource
              and ra.startedAt >= :startedAt
              and ra.startedAt < :endedAt
            order by ra.startedAt asc
            """)
    List<ResourceAvailability> findAllSlotsInRange(
            @Param("resource") Resource resource,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("endedAt") LocalDateTime endedAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ra
            from ResourceAvailability ra
            where ra.resource = :resource
              and ra.startedAt >= :startedAt
              and ra.endedAt <= :endedAt
            order by ra.startedAt asc
            """)
    List<ResourceAvailability> lockAllSlotsInRange(
            @Param("resource") Resource resource,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("endedAt") LocalDateTime endedAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ra
            from ResourceAvailability ra
            where ra.resource = :resource
              and ra.startedAt < :endedAt
              and ra.endedAt > :startedAt
            order by ra.startedAt asc
            """)
    List<ResourceAvailability> lockOverlappingSlots(
            @Param("resource") Resource resource,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("endedAt") LocalDateTime endedAt
    );

    @Query("""
            select ra
            from ResourceAvailability ra
            where ra.resource = :resource
              and ra.startedAt >= :startedAt
              and ra.endedAt <= :endedAt
            order by ra.startedAt asc
            """)
    List<ResourceAvailability> findCandidateSlots(
            @Param("resource") Resource resource,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("endedAt") LocalDateTime endedAt
    );

    @Query("""
            select ra
            from ResourceAvailability ra
            join fetch ra.resource r
            where ra.resource = :resource
              and ra.active = true
              and ra.available = true
              and ra.remainingCapacity > 0
              and ra.startedAt >= :startedAt
            order by ra.startedAt asc
            """)
    List<ResourceAvailability> findFutureReservableSlots(
            @Param("resource") Resource resource,
            @Param("startedAt") LocalDateTime startedAt,
            Pageable pageable
    );

    @Query("""
            select ra
            from ResourceAvailability ra
            join fetch ra.resource r
            where ra.resource = :resource
              and ra.active = true
              and ra.endedAt > :cutoff
            order by ra.startedAt asc
            """)
    List<ResourceAvailability> findFutureActiveSlots(
            @Param("resource") Resource resource,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    @Query("""
            select ra
            from ResourceAvailability ra
            where ra.endedAt <= :cutoff
              and (ra.active = true or ra.available = true)
            order by ra.endedAt asc
            """)
    List<ResourceAvailability> findExpiredSlots(@Param("cutoff") LocalDateTime cutoff, Limit limit);
}
