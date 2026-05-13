package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.outbox.enums.OutboxEventStatus;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.repository.OutboxEventRepository;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private static final long PROCESSING_LEASE_SECONDS = 300;
    private static final long RETRY_DELAY_SECONDS = 30;

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final List<OutboxEventProcessor> processors;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public OutboxEvent publish(String eventType, String aggregateType, String aggregateId, Object payload) {
        validate(eventType, "Outbox event type is required");
        validate(aggregateType, "Outbox aggregate type is required");
        validate(aggregateId, "Outbox aggregate id is required");

        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType.trim().toUpperCase())
                .aggregateType(aggregateType.trim().toUpperCase())
                .aggregateId(aggregateId.trim())
                .payload(serialize(payload))
                .status(OutboxEventStatus.PENDING)
                .availableAt(Instant.now())
                .build();

        return repository.save(event);
    }

    @Override
    public List<OutboxEvent> claimPending(int batchSize) {
        if (batchSize < 1) {
            throw new BadRequestException("Outbox batch size must be greater than zero");
        }

        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            List<OutboxEvent> events = repository.findByStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                    Set.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED, OutboxEventStatus.PROCESSING),
                    now,
                    PageRequest.of(0, batchSize)
            );

            events.forEach(event -> {
                event.setStatus(OutboxEventStatus.PROCESSING);
                event.setAvailableAt(now.plusSeconds(PROCESSING_LEASE_SECONDS));
            });
            return repository.saveAll(events);
        });
    }

    @Override
    public void markPublished(Long eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            OutboxEvent event = find(eventId);
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            repository.save(event);
        });
    }

    @Override
    public void markFailed(Long eventId, String errorMessage, Instant nextAttemptAt) {
        transactionTemplate.executeWithoutResult(status -> {
            OutboxEvent event = find(eventId);
            event.setStatus(OutboxEventStatus.FAILED);
            event.setAttemptCount(event.getAttemptCount() + 1);
            event.setLastError(errorMessage);
            event.setAvailableAt(nextAttemptAt == null ? Instant.now() : nextAttemptAt);
            repository.save(event);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OutboxEvent> list(String eventType,
                                  String aggregateType,
                                  String aggregateId,
                                  OutboxEventStatus status,
                                  Instant createdFrom,
                                  Instant createdTo,
                                  Pageable pageable) {
        Specification<OutboxEvent> specification = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(eventType)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("eventType")), eventType.trim().toUpperCase()));
        }

        if (StringUtils.hasText(aggregateType)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("aggregateType")), aggregateType.trim().toUpperCase()));
        }

        if (StringUtils.hasText(aggregateId)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("aggregateId"), aggregateId.trim()));
        }

        if (status != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }

        if (createdFrom != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }

        if (createdTo != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }

        return repository.findAll(specification, pageable);
    }

    @Override
    public int processPending(int batchSize) {
        List<OutboxEvent> events = claimPending(batchSize);
        for (OutboxEvent event : events) {
            try {
                resolveProcessor(event).process(event);
                markPublished(event.getId());
            } catch (RuntimeException ex) {
                markFailed(event.getId(), ex.getMessage(), Instant.now().plusSeconds(RETRY_DELAY_SECONDS));
            }
        }
        return events.size();
    }

    @Override
    @Transactional
    public void requeue(Long eventId) {
        OutboxEvent event = find(eventId);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setAvailableAt(Instant.now());
        event.setLastError(null);
        repository.save(event);
    }

    @Override
    @Transactional
    public int requeueFailed(String aggregateType) {
        List<OutboxEvent> failed = repository.findByStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                Set.of(OutboxEventStatus.FAILED),
                Instant.now().plusSeconds(3600 * 24 * 365),
                PageRequest.of(0, 500)
        );
        List<OutboxEvent> toRequeue = failed.stream()
                .filter(e -> aggregateType == null
                        || aggregateType.isBlank()
                        || aggregateType.equalsIgnoreCase(e.getAggregateType()))
                .toList();
        toRequeue.forEach(e -> {
            e.setStatus(OutboxEventStatus.PENDING);
            e.setAvailableAt(Instant.now());
            e.setLastError(null);
        });
        repository.saveAll(toRequeue);
        return toRequeue.size();
    }

    private OutboxEvent find(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("Outbox event id is required");
        }

        return repository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event with id " + eventId + " not found"));
    }

    private void validate(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
    }

    private OutboxEventProcessor resolveProcessor(OutboxEvent event) {
        Optional<OutboxEventProcessor> processor = processors.stream()
                .filter(candidate -> candidate.supports(event))
                .findFirst();

        return processor.orElseThrow(() -> new IllegalStateException(
                "No outbox processor found for event type " + event.getEventType()
        ));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize outbox payload", ex);
        }
    }
}
