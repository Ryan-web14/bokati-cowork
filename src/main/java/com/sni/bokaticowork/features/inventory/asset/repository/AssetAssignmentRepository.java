package com.sni.bokaticowork.features.inventory.asset.repository;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssignmentStatus;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Long> {

    Optional<AssetAssignment> findFirstByAssetAndStatusOrderByStartAtDesc(Asset asset, AssetAssignmentStatus status);

    boolean existsByAssetAndStatusIn(Asset asset, java.util.Collection<AssetAssignmentStatus> statuses);

    @Query("""
            SELECT COUNT(assignment) > 0
            FROM AssetAssignment assignment
            WHERE assignment.asset = :asset
              AND assignment.status IN :statuses
              AND assignment.startAt < :endAt
              AND COALESCE(assignment.expectedReturnAt, assignment.endAt, :endAt) > :startAt
            """)
    boolean existsOverlappingReservationOrAssignment(@Param("asset") Asset asset,
                                                     @Param("statuses") java.util.Collection<AssetAssignmentStatus> statuses,
                                                     @Param("startAt") Instant startAt,
                                                     @Param("endAt") Instant endAt);

    List<AssetAssignment> findAllByAssetOrderByStartAtDesc(Asset asset);

    List<AssetAssignment> findAllByStatusAndExpectedReturnAtBefore(AssetAssignmentStatus status, Instant expectedReturnAt);

    List<AssetAssignment> findAllByStatusAndExpectedReturnAtBetween(AssetAssignmentStatus status, Instant from, Instant to);
}
