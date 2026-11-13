package com.sni.bokaticowork.features.approval.repository;

import com.sni.bokaticowork.features.approval.model.ApprovalDelegation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, Long> {

    List<ApprovalDelegation> findByCancelledAtIsNullOrderByStartsOnAsc();
}
