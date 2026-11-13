package com.sni.bokaticowork.features.approval.repository;

import com.sni.bokaticowork.features.approval.model.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    List<ApprovalStep> findByRequestIdOrderByLevelOrderAsc(Long requestId);
}
