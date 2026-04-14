package com.sni.bokaticowork.core.generator.sequenceEngine.repository;

import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SequenceCounterRepository extends JpaRepository<SequenceCounter, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SequenceCounter> findByPeriodKeyAndSequenceDefinitionId(String periodKey, Long sequenceDefinitionId);

    @Modifying
    @Query(value = """
            insert into sequence_counter (
                id,
                sequence_definition_id,
                sequence_code,
                period_key,
                current_value,
                version,
                created_at,
                updated_at
            ) values (
                :id,
                :sequenceDefinitionId,
                :sequenceCode,
                :periodKey,
                :currentValue,
                0,
                now(),
                now()
            )
            on conflict (sequence_definition_id, period_key) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") Long id,
                       @Param("sequenceDefinitionId") Long sequenceDefinitionId,
                       @Param("sequenceCode") String sequenceCode,
                       @Param("periodKey") String periodKey,
                       @Param("currentValue") Long currentValue);
}
