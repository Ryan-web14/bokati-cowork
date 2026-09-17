package com.sni.bokaticowork.features.inventory.control.repository;

import com.sni.bokaticowork.features.inventory.control.enums.InventoryCountStatus;
import com.sni.bokaticowork.features.inventory.control.model.InventoryCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {

    /**
     * Inventaires non encore valides sur une plage, utilise pour refuser la cloture d une periode
     * dont les ecarts ne sont pas leves.
     */
    long countByStatusInAndCreatedAtBetween(java.util.Collection<InventoryCountStatus> statuses,
                                            java.time.Instant from,
                                            java.time.Instant to);
    boolean existsByCountCode(String countCode);

    Optional<InventoryCount> findByCountCode(String countCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT count FROM InventoryCount count WHERE count.countCode = :countCode")
    Optional<InventoryCount> findByCountCodeForUpdate(@Param("countCode") String countCode);

    Page<InventoryCount> findAllByStatus(InventoryCountStatus status, Pageable pageable);
}
