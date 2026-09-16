package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.AdjustmentReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdjustmentReasonRepository extends JpaRepository<AdjustmentReason, Long> {

    Optional<AdjustmentReason> findByReasonCode(String reasonCode);

    boolean existsByReasonCode(String reasonCode);

    List<AdjustmentReason> findAllByOrderByLabelAsc();

    List<AdjustmentReason> findAllByActiveTrueOrderByLabelAsc();
}
