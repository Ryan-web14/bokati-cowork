package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudgetForecast;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetForecastRepository extends JpaRepository<AccBudgetForecast, Long> {

    java.util.Optional<AccBudgetForecast> findByNumber(String number);

    java.util.List<AccBudgetForecast> findByItemCodeOrderByPlannedMonthAsc(String itemCode);

    java.util.List<AccBudgetForecast> findByStatusInOrderByPlannedMonthAsc(java.util.Collection<String> statuses);
}
