package com.sni.bokaticowork.features.contract.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.contract.enums.AmendmentStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractAmendment;
import com.sni.bokaticowork.features.contract.model.ContractAmendmentVariable;
import com.sni.bokaticowork.features.contract.model.ContractAuditEvent;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentRepository;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentVariableRepository;
import com.sni.bokaticowork.features.contract.repository.ContractAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Les versions d'un contrat · l'original, puis chaque avenant applique et chaque extension de date,
 * dans l'ordre. La derniere est celle en vigueur.
 *
 * <p>Rien n'est copie : tout se relit sur les avenants et sur le journal du contrat, qui font foi.</p>
 */
@Component
@RequiredArgsConstructor
public class ContractVersionHistory {

    public static final String TERM_EXTENDED_EVENT = "CONTRACT_TERM_EXTENDED";
    private static final String END_VARIABLE = "periode_fin";

    public enum Kind { ORIGINAL, AMENDMENT, EXTENSION }

    /**
     * @param previousEndDate la fin du contrat avant ce changement · vide s'il n'a pas touche aux dates
     * @param endDate         la fin du contrat apres ce changement · vide s'il n'a pas touche aux dates
     */
    public record Version(int version, Kind kind, String amendmentCode, String description, LocalDate effectiveDate,
                          LocalDate previousEndDate, LocalDate endDate, Instant at, String documentCode) {
    }

    private record Change(Kind kind, String amendmentCode, String description, LocalDate effectiveDate,
                          LocalDate previousEndDate, LocalDate endDate, Instant at, String documentCode) {
    }

    private final ContractAmendmentRepository amendmentRepository;
    private final ContractAmendmentVariableRepository variableRepository;
    private final ContractAuditEventRepository auditRepository;
    private final ObjectMapper objectMapper;

    public List<Version> of(Contract contract) {
        List<Change> changes = new ArrayList<>();
        amendmentRepository.findAllByOriginalContractCodeAndStatusIn(contract.getContractCode(), List.of(AmendmentStatus.ACTIVE))
                .forEach(amendment -> changes.add(amendmentChange(amendment)));
        auditRepository.findAllByContractCodeOrderByOccurredAtAsc(contract.getContractCode()).stream()
                .filter(event -> TERM_EXTENDED_EVENT.equals(event.getEventType()))
                .forEach(event -> changes.add(extensionChange(event)));
        changes.sort(Comparator.comparing(Change::at, Comparator.nullsLast(Comparator.naturalOrder())));

        LocalDate originalEnd = changes.stream().map(Change::previousEndDate).filter(java.util.Objects::nonNull).findFirst()
                .orElse(contract.getEndDate());
        List<Version> versions = new ArrayList<>();
        versions.add(new Version(1, Kind.ORIGINAL, null, contract.getTitle(), contract.getStartDate(), null, originalEnd,
                contract.getSignedAt() != null ? contract.getSignedAt() : contract.getCreatedAt(), documentOf(contract)));
        for (Change change : changes) {
            versions.add(new Version(versions.size() + 1, change.kind(), change.amendmentCode(), change.description(),
                    change.effectiveDate(), change.previousEndDate(), change.endDate(), change.at(), change.documentCode()));
        }
        return versions;
    }

    private Change amendmentChange(ContractAmendment amendment) {
        ContractAmendmentVariable end = variableRepository.findAllByAmendmentCodeOrderByVariableKeyAsc(amendment.getCode()).stream()
                .filter(variable -> END_VARIABLE.equals(variable.getVariableKey()))
                .findFirst().orElse(null);
        return new Change(Kind.AMENDMENT, amendment.getCode(), amendment.getDescription(), amendment.getEffectiveDate(),
                end == null ? null : date(end.getPreviousValue()), end == null ? null : date(end.getNewValue()),
                amendment.getActivatedAt() != null ? amendment.getActivatedAt() : amendment.getSignedAt(),
                StringUtils.hasText(amendment.getSignedDocumentCode()) ? amendment.getSignedDocumentCode() : amendment.getDraftDocumentCode());
    }

    private Change extensionChange(ContractAuditEvent event) {
        JsonNode payload = payload(event.getPayload());
        LocalDate previousEnd = date(payload.path("previousEndDate").asText(null));
        return new Change(Kind.EXTENSION, null, event.getJustification(),
                previousEnd == null ? null : previousEnd.plusDays(1), previousEnd,
                date(payload.path("newEndDate").asText(null)), event.getOccurredAt(), null);
    }

    private JsonNode payload(String json) {
        try {
            return objectMapper.readTree(StringUtils.hasText(json) ? json : "{}");
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private static String documentOf(Contract contract) {
        return StringUtils.hasText(contract.getSignedDocumentCode()) ? contract.getSignedDocumentCode() : contract.getDraftDocumentCode();
    }

    private static LocalDate date(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
