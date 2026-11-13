package com.sni.bokaticowork.features.approval.repository;

import com.sni.bokaticowork.features.approval.model.ApprovalCircuitLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalCircuitLevelRepository extends JpaRepository<ApprovalCircuitLevel, Long> {

    List<ApprovalCircuitLevel> findByCircuitIdOrderByLevelOrderAsc(Long circuitId);

    void deleteByCircuitId(Long circuitId);
}
