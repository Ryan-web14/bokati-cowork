package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccUsageConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccUsageConsumptionRepository extends JpaRepository<AccUsageConsumption, Long> {
    List<AccUsageConsumption> findAllByScheduleIdOrderByUsageDateAsc(Long scheduleId);
}
