package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.features.contract.dto.response.ContractAuditEventResponse;
import com.sni.bokaticowork.features.contract.model.ContractAuditEvent;
import com.sni.bokaticowork.features.contract.repository.ContractAuditEventRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAuditEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ContractAuditEventServiceImpl implements ContractAuditEventService {

    private final ContractAuditEventRepository repository;

    @Override
    public void record(String contractCode, String amendmentCode, String eventType,
                       Long actorId, String actorType, String actorName,
                       String justification, String payload) {
        Instant now = Instant.now();
        String previousHash = repository.findTopByContractCodeOrderByOccurredAtDesc(contractCode)
                .map(ContractAuditEvent::getEventHash)
                .orElse(null);

        String rawData = (previousHash != null ? previousHash : "GENESIS")
                + "|" + contractCode
                + "|" + eventType
                + "|" + (actorId != null ? actorId : "SYSTEM")
                + "|" + now
                + "|" + (payload != null ? payload : "");

        repository.save(ContractAuditEvent.builder()
                .contractCode(contractCode)
                .amendmentCode(amendmentCode)
                .eventType(eventType)
                .actorId(actorId)
                .actorType(actorType != null ? actorType : "SYSTEM")
                .actorName(actorName)
                .occurredAt(now)
                .previousHash(previousHash)
                .eventHash(sha256(rawData))
                .payload(payload)
                .justification(justification)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractAuditEventResponse> getAuditTrail(String contractCode) {
        return repository.findAllByContractCodeOrderByOccurredAtAsc(contractCode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private ContractAuditEventResponse toResponse(ContractAuditEvent event) {
        return ContractAuditEventResponse.builder()
                .id(event.getId())
                .contractCode(event.getContractCode())
                .amendmentCode(event.getAmendmentCode())
                .eventType(event.getEventType())
                .actorId(event.getActorId())
                .actorType(event.getActorType())
                .actorName(event.getActorName())
                .occurredAt(event.getOccurredAt())
                .previousHash(event.getPreviousHash())
                .eventHash(event.getEventHash())
                .payload(event.getPayload())
                .justification(event.getJustification())
                .build();
    }
}
