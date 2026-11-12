package com.sni.bokaticowork.features.accounting.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import com.sni.bokaticowork.features.accounting.enums.PostingRuleStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Requetes et reponses du moteur d'imputation (lot F2). */
public final class PostingDtos {

    private PostingDtos() {
    }

    /**
     * Schema d'ecriture. {@code conditions} et {@code lines} sont du JSON (objet et tableau), voir
     * la doc du lot F2 pour leur syntaxe.
     */
    public record RuleRequest(
            @NotBlank String code,
            @NotBlank String eventType,
            @NotBlank String label,
            String description,
            @NotBlank String family,
            Integer priority,
            Object conditions,
            String journal,
            String entryLabel,
            Object lines,
            Boolean noEntry,
            Object samplePayload,
            LocalDate effectiveFrom
    ) {
    }

    public record Rule(
            Long id,
            String code,
            Integer version,
            String eventType,
            String label,
            String description,
            String family,
            Integer priority,
            @JsonRawValue String conditions,
            String journal,
            String entryLabel,
            @JsonRawValue String lines,
            Boolean noEntry,
            @JsonRawValue String samplePayload,
            LocalDate effectiveFrom,
            PostingRuleStatus status,
            String activatedBy,
            Instant activatedAt,
            String createdBy,
            Instant createdAt
    ) {
    }

    /** Apercu : un schema (enregistre ou en cours d'edition) sur un evenement (enregistre ou fourni). */
    public record PreviewRequest(Long ruleId, RuleRequest rule, Long eventId, Object payload) {
    }

    public record PreviewLine(String accountNumber, String accountLabel, Boolean accountExists, String thirdPartyCode, String label,
                              BigDecimal debit, BigDecimal credit, String reference) {
    }

    public record Preview(
            String ruleCode,
            Integer ruleVersion,
            Boolean matches,
            String journalCode,
            Boolean journalExists,
            String label,
            List<PreviewLine> lines,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            Boolean balanced,
            List<String> warnings,
            String error
    ) {
    }

    public record Event(
            Long id,
            String eventType,
            String reference,
            String family,
            LocalDate eventDate,
            @JsonRawValue String payload,
            PostingEventStatus status,
            Boolean replay,
            String failureReason,
            String ruleCode,
            Integer ruleVersion,
            String entryNumber,
            String provisionalNumber,
            String shiftedNote,
            Integer attempts,
            Instant lastAttemptAt,
            Instant postedAt,
            String ignoredBy,
            String ignoredReason,
            Instant createdAt
    ) {
    }

    public record EventSummary(Map<PostingEventStatus, Long> counts, Boolean accountingEnabled, LocalDate startDate,
                               List<String> disabledFamilies) {
    }

    public record Reason(String reason) {
    }

    /** Evenement soumis a la main (test, reprise ponctuelle). */
    public record SubmitEvent(@NotBlank String eventType, @NotBlank String reference, String family, LocalDate eventDate,
                              Object payload, Boolean replay) {
    }

    public record Resolver(String name, String description) {
    }

    public record BatchResult(Integer processed, Integer succeeded, Integer failed, List<String> errors) {
    }
}
