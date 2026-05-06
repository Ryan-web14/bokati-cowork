package com.sni.bokaticowork.core.outbox.service.interfaces;

import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OutboxService {

    OutboxEvent publish(String eventType, String aggregateType, String aggregateId, Object payload);

    List<OutboxEvent> claimPending(int batchSize);

    void markPublished(Long eventId);

    void markFailed(Long eventId, String errorMessage, Instant nextAttemptAt);

    Page<OutboxEvent> list(String eventType,
                           String aggregateType,
                           String aggregateId,
                           com.sni.bokaticowork.core.outbox.enums.OutboxEventStatus status,
                           Instant createdFrom,
                           Instant createdTo,
                           Pageable pageable);

    int processPending(int batchSize);

    void requeue(Long eventId);

    int requeueFailed(String aggregateType);
}
