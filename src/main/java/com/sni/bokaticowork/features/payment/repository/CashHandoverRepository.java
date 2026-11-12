package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashHandoverRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashHandover, Long> {

    Optional<com.sni.bokaticowork.features.payment.model.CashHandover> findByHandoverNumber(String handoverNumber);

    List<com.sni.bokaticowork.features.payment.model.CashHandover> findByFromSessionAndStatus(String fromSession, com.sni.bokaticowork.features.payment.enums.CashHandoverStatus status);
}
