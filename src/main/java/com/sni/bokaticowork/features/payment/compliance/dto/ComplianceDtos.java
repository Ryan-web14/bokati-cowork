package com.sni.bokaticowork.features.payment.compliance.dto;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCaseNote;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceFreeze;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningCheck;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningListEntry;
import com.sni.bokaticowork.features.payment.compliance.service.ScreeningService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Les contrats du module de conformite · petits, et ensemble. */
public final class ComplianceDtos {

    private ComplianceDtos() {
    }

    // ---- Regles --------------------------------------------------------------------------

    public record RuleUpdateRequest(
            String name,
            String description,
            ComplianceRule.Severity severity,
            ComplianceRule.Action action,
            BigDecimal amountThreshold,
            Integer countThreshold,
            BigDecimal ratioThreshold,
            Integer windowMinutes,
            Boolean active,
            Instant effectiveFrom,
            Instant effectiveTo,
            @NotBlank(message = "Un changement de règle se motive") String rationale
    ) {
    }

    public record RuleView(String ruleCode, String name, String description, ComplianceRule.Category category,
                           String detector, ComplianceRule.Severity severity, ComplianceRule.Action action,
                           BigDecimal amountThreshold, Integer countThreshold, BigDecimal ratioThreshold,
                           Integer windowMinutes, Boolean active, Instant effectiveFrom, Instant effectiveTo,
                           Long hitCount, Long confirmedCount, Long dismissedCount, BigDecimal falsePositiveRate,
                           String createdBy, String approvedBy, Instant updatedAt) {
        public static RuleView of(ComplianceRule r) {
            return new RuleView(r.getRuleCode(), r.getName(), r.getDescription(), r.getCategory(), r.getDetector(),
                    r.getSeverity(), r.getAction(), r.getAmountThreshold(), r.getCountThreshold(), r.getRatioThreshold(),
                    r.getWindowMinutes(), r.getActive(), r.getEffectiveFrom(), r.getEffectiveTo(), r.getHitCount(),
                    r.getConfirmedCount(), r.getDismissedCount(), r.falsePositiveRate(), r.getCreatedBy(), r.getApprovedBy(),
                    r.getUpdatedAt());
        }
    }

    // ---- Dossiers ------------------------------------------------------------------------

    public record AssignRequest(@NotBlank(message = "Indiquez à qui confier le dossier") String assignee) {
    }

    public record NoteRequest(@NotBlank(message = "Une note vide n'est pas une note") String note) {
    }

    public record EscalateRequest(@NotBlank String escalatedTo, @NotBlank String reason) {
    }

    public record CloseRequest(
            @NotNull(message = "Une clôture porte une décision") ComplianceCase.Decision decision,
            @NotBlank(message = "Une décision sans motif écrit n'est pas une décision") String rationale,
            String findings
    ) {
    }

    public record CaseView(String caseNumber, String subjectType, String subjectCode, String walletNumber, String title,
                           List<String> triggeredByFlags, ComplianceCase.Status status, ComplianceCase.Priority priority,
                           String assignedTo, Instant assignedAt, Instant dueAt, boolean overdue, String findings,
                           ComplianceCase.Decision decision, String decisionRationale, String decidedBy, Instant decidedAt,
                           String escalatedTo, Instant escalatedAt, String openedBy, Instant createdAt) {
        public static CaseView of(ComplianceCase c) {
            return new CaseView(c.getCaseNumber(), c.getSubjectType(), c.getSubjectCode(),
                    c.getWallet() == null ? null : c.getWallet().getWalletNumber(), c.getTitle(),
                    c.getTriggeredByFlags() == null ? List.of() : List.of(c.getTriggeredByFlags().split(",")),
                    c.getStatus(), c.getPriority(), c.getAssignedTo(), c.getAssignedAt(), c.getDueAt(),
                    c.overdueAt(Instant.now()), c.getFindings(), c.getDecision(), c.getDecisionRationale(),
                    c.getDecidedBy(), c.getDecidedAt(), c.getEscalatedTo(), c.getEscalatedAt(), c.getOpenedBy(), c.getCreatedAt());
        }
    }

    public record NoteView(String author, String note, Instant createdAt) {
        public static NoteView of(ComplianceCaseNote n) {
            return new NoteView(n.getAuthor(), n.getNote(), n.getCreatedAt());
        }
    }

    // ---- Listes ---------------------------------------------------------------------------

    public record ListEntryRequest(@NotBlank String fullName, String aliases, Integer birthYear, String nationality, String reference) {
        public ScreeningService.ListEntryInput toInput() {
            return new ScreeningService.ListEntryInput(fullName, aliases, birthYear, nationality, reference);
        }
    }

    public record LoadListRequest(
            @NotBlank String listCode,
            @NotBlank String listVersion,
            @NotNull ScreeningListEntry.Type type,
            @NotNull List<ListEntryRequest> entries
    ) {
    }

    public record ScreenRequest(@NotBlank String subjectType, @NotBlank String subjectCode, @NotBlank String fullName, Integer birthYear) {
    }

    public record ScreeningReviewRequest(boolean trueMatch, @NotBlank String rationale) {
    }

    public record CheckView(String checkNumber, String subjectType, String subjectCode, String subjectName,
                            ScreeningCheck.Trigger trigger, String listsChecked, ScreeningCheck.Result result,
                            String matchDetails, Instant checkedAt, String checkedBy, String caseNumber,
                            String reviewedBy, Instant reviewedAt, String reviewOutcome) {
        public static CheckView of(ScreeningCheck c) {
            return new CheckView(c.getCheckNumber(), c.getSubjectType(), c.getSubjectCode(), c.getSubjectName(),
                    c.getTriggerReason(), c.getListsChecked(), c.getResult(), c.getMatchDetails(), c.getCheckedAt(),
                    c.getCheckedBy(), c.getCaseNumber(), c.getReviewedBy(), c.getReviewedAt(), c.getReviewOutcome());
        }
    }

    // ---- Gel -------------------------------------------------------------------------------

    public record FreezeRequest(@NotBlank String authority, @NotBlank String instructionReference,
                                LocalDate instructionDate, @NotBlank String rationale) {
    }

    public record LiftRequest(@NotBlank String liftReference, @NotBlank String liftRationale) {
    }

    public record FreezeView(String freezeNumber, String walletNumber, String authority, String instructionReference,
                             LocalDate instructionDate, String rationale, String frozenBy, Instant frozenAt,
                             String liftedBy, Instant liftedAt, String liftReference, String liftRationale, String caseNumber) {
        public static FreezeView of(ComplianceFreeze f) {
            return new FreezeView(f.getFreezeNumber(), f.getWallet().getWalletNumber(), f.getAuthority(),
                    f.getInstructionReference(), f.getInstructionDate(), f.getRationale(), f.getFrozenBy(), f.getFrozenAt(),
                    f.getLiftedBy(), f.getLiftedAt(), f.getLiftReference(), f.getLiftRationale(), f.getCaseNumber());
        }
    }
}
