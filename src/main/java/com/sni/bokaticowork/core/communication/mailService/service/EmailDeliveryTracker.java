package com.sni.bokaticowork.core.communication.mailService.service;

import com.sni.bokaticowork.core.communication.mailService.dto.response.EmailDeliveryResponse;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailDeliveryStatus;
import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import com.sni.bokaticowork.core.communication.mailService.repository.EmailDeliveryLogRepository;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailDeliveryTracker {

    public static final String SEQUENCE_CODE = "email_delivery";

    private final EmailDeliveryLogRepository repository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailDeliveryResponse queue(String provider,
                                       String fromEmail,
                                       String recipientEmail,
                                       String subject,
                                       String bodyType,
                                       String bodyContent,
                                       String relatedType,
                                       String relatedCode) {
        return queue(provider, fromEmail, recipientEmail, subject, bodyType, bodyContent,
                relatedType, relatedCode, null);
    }

    /**
     * Inserts the delivery row. A non-null {@code dedupKey} is protected by a unique index, so a
     * concurrent insert of the same logical email throws {@link org.springframework.dao.DataIntegrityViolationException}
     * rather than producing a second send · callers treat that as "someone else already queued it".
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailDeliveryResponse queue(String provider,
                                       String fromEmail,
                                       String recipientEmail,
                                       String subject,
                                       String bodyType,
                                       String bodyContent,
                                       String relatedType,
                                       String relatedCode,
                                       String dedupKey) {
        EmailDeliveryLog log = EmailDeliveryLog.builder()
                .emailNumber(sequenceGenerator.next(SEQUENCE_CODE))
                .provider(provider)
                .fromEmail(fromEmail)
                .recipientEmail(recipientEmail)
                .subject(subject)
                .bodyType(bodyType)
                .bodyContent(bodyContent)
                .relatedType(relatedType)
                .relatedCode(relatedCode)
                .dedupKey(dedupKey)
                .status(EmailDeliveryStatus.QUEUED)
                .attempts(0)
                .build();
        return toResponse(repository.saveAndFlush(log));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<EmailDeliveryResponse> findByDedupKeys(Collection<String> dedupKeys) {
        if (dedupKeys == null || dedupKeys.isEmpty()) {
            return Optional.empty();
        }
        return repository.findFirstByDedupKeyInOrderByCreatedAtDesc(dedupKeys).map(this::toResponse);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailDeliveryLog markSending(String emailNumber) {
        EmailDeliveryLog log = getEntity(emailNumber);
        log.setStatus(EmailDeliveryStatus.SENDING);
        log.setAttempts(log.getAttempts() == null ? 1 : log.getAttempts() + 1);
        log.setLastError(null);
        log.setFailedAt(null);
        return repository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(String emailNumber) {
        EmailDeliveryLog log = getEntity(emailNumber);
        log.setStatus(EmailDeliveryStatus.SENT);
        log.setSentAt(Instant.now());
        log.setLastError(null);
        log.setFailedAt(null);
        repository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String emailNumber, String error) {
        EmailDeliveryLog log = getEntity(emailNumber);
        log.setStatus(EmailDeliveryStatus.FAILED);
        log.setLastError(truncate(error, 4000));
        log.setFailedAt(Instant.now());
        repository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailDeliveryResponse resetForRetry(String emailNumber) {
        EmailDeliveryLog log = getEntity(emailNumber);
        log.setStatus(EmailDeliveryStatus.QUEUED);
        log.setLastError(null);
        log.setSentAt(null);
        log.setFailedAt(null);
        return toResponse(repository.save(log));
    }

    @Transactional(readOnly = true)
    public EmailDeliveryLog getEntity(String emailNumber) {
        return repository.findByEmailNumber(emailNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Email delivery log not found: " + emailNumber));
    }

    @Transactional(readOnly = true)
    public EmailDeliveryResponse get(String emailNumber) {
        return toResponse(getEntity(emailNumber));
    }

    @Transactional(readOnly = true)
    public Page<EmailDeliveryResponse> list(EmailDeliveryStatus status,
                                            String provider,
                                            String recipientEmail,
                                            String relatedType,
                                            String relatedCode,
                                            Instant createdFrom,
                                            Instant createdTo,
                                            Pageable pageable) {
        Specification<EmailDeliveryLog> specification = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(provider)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("provider")), provider.trim().toUpperCase()));
        }
        if (StringUtils.hasText(recipientEmail)) {
            String pattern = "%" + recipientEmail.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("recipientEmail")), pattern));
        }
        if (StringUtils.hasText(relatedType)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("relatedType")), relatedType.trim().toUpperCase()));
        }
        if (StringUtils.hasText(relatedCode)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("relatedCode"), relatedCode.trim()));
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }

        return repository.findAll(specification, pageable).map(this::toResponse);
    }

    public EmailDeliveryResponse toResponse(EmailDeliveryLog log) {
        return new EmailDeliveryResponse(
                log.getEmailNumber(),
                log.getProvider(),
                log.getFromEmail(),
                log.getRecipientEmail(),
                log.getSubject(),
                log.getBodyType(),
                log.getStatus(),
                log.getAttempts(),
                log.getLastError(),
                log.getRelatedType(),
                log.getRelatedCode(),
                log.getCreatedAt(),
                log.getUpdatedAt(),
                log.getSentAt(),
                log.getFailedAt()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
