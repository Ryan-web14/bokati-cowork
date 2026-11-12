package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashDenominationRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashDenomination, Long> {

    List<com.sni.bokaticowork.features.payment.model.CashDenomination> findByCurrencyOrderBySortOrderAsc(String currency);

    List<com.sni.bokaticowork.features.payment.model.CashDenomination> findAllByOrderByCurrencyAscSortOrderAsc();
}
