package com.sni.bokaticowork.features.approval.repository;

import com.sni.bokaticowork.features.approval.model.ApprovalCircuit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalCircuitRepository extends JpaRepository<ApprovalCircuit, Long> {

    Optional<ApprovalCircuit> findByCode(String code);

    Optional<ApprovalCircuit> findByCenterAndRequestType(String center, String requestType);

    List<ApprovalCircuit> findAllByOrderByCenterAscCodeAsc();

    List<ApprovalCircuit> findByCenterOrderByCodeAsc(String center);
}
