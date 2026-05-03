package com.sni.bokaticowork.core.outbox.repository;

import com.sni.bokaticowork.core.outbox.enums.OutboxEventStatus;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>, JpaSpecificationExecutor<OutboxEvent> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findByStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(Collection<OutboxEventStatus> statuses,
                                                                                   Instant availableAt,
                                                                                   Pageable pageable);

    List<OutboxEvent> findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(String aggregateType, String aggregateId);
}
