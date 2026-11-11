package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashResponsibilityTransferRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashResponsibilityTransfer, Long> {

    List<com.sni.bokaticowork.features.payment.model.CashResponsibilityTransfer> findByRegisterCodeOrderByEffectiveAtDesc(String registerCode);

    Optional<com.sni.bokaticowork.features.payment.model.CashResponsibilityTransfer> findByTransferNumber(String transferNumber);
}
