package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceClosure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface ResourceClosureRepository extends JpaRepository<ResourceClosure, Long> {

    Page<ResourceClosure> findAllByResource(Resource resource, Pageable pageable);

    Optional<ResourceClosure> findByIdAndResource(Long id, Resource resource);

    @org.springframework.data.jpa.repository.Query("""
            select case when count(rc) > 0 then true else false end
            from ResourceClosure rc
            where rc.resource = :resource
              and rc.active = true
              and rc.startedAt < :endedAt
              and rc.endedAt > :startedAt
            """)
    boolean existsActiveOverlap(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt);
}
