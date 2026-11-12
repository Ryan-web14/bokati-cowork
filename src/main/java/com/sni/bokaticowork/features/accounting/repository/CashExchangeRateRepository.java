package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.CashExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashExchangeRateRepository extends JpaRepository<CashExchangeRate, Long> {

    java.util.Optional<CashExchangeRate> findFirstByCurrencyAndValidOnLessThanEqualOrderByValidOnDesc(String currency, java.time.LocalDate date);

    java.util.Optional<CashExchangeRate> findByCurrencyAndValidOn(String currency, java.time.LocalDate date);

    java.util.List<CashExchangeRate> findTop30ByOrderByValidOnDescCurrencyAsc();
}
