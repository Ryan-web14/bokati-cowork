package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashDelegationRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashDelegation, Long> {

    List<com.sni.bokaticowork.features.payment.model.CashDelegation> findByCancelledAtIsNullOrderByStartsOnAsc();
}
