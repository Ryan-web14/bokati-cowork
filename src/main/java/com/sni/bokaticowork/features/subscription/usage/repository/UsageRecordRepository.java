package com.sni.bokaticowork.features.subscription.usage.repository;

import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long>, JpaSpecificationExecutor<UsageRecord> {

    @Query(nativeQuery = true, value = "SELECT * FROM usage_record WHERE usage_number = :usageNumber")
    Optional<UsageRecord> findByUsageNumber(@Param("usageNumber") String usageNumber);
}
