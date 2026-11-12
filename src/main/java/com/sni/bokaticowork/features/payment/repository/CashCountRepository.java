package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashCountRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashCount, Long> {

    Optional<com.sni.bokaticowork.features.payment.model.CashCount> findByCountNumber(String countNumber);

    List<com.sni.bokaticowork.features.payment.model.CashCount> findBySessionNumberOrderByCreatedAtAsc(String sessionNumber);

    List<com.sni.bokaticowork.features.payment.model.CashCount> findByRegisterCodeOrderByCreatedAtDesc(String registerCode);
}
