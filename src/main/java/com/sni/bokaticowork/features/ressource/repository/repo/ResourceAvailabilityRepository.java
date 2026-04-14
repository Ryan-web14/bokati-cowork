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
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResourceAvailabilityRepository extends JpaRepository<ResourceAvailability, Long> {

    Page<ResourceAvailability> findAllByResource(Resource resource, Pageable pageable);

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
            where ra.resource = :resource
              and ra.startedAt >= :startedAt
              and ra.endedAt <= :endedAt
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
}
