package com.sni.bokaticowork.features.approval.repository;

import com.sni.bokaticowork.features.approval.model.ApprovalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalEventRepository extends JpaRepository<ApprovalEvent, Long> {

    List<ApprovalEvent> findByRequestIdOrderByOccurredAtAscIdAsc(Long requestId);
}
