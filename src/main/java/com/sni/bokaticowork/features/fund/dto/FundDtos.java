package com.sni.bokaticowork.features.fund.dto;

import com.sni.bokaticowork.features.fund.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requetes et reponses des etats de besoin, demandes de fonds et justifications (lots F5 et F6). */
public final class FundDtos {

    private FundDtos() {
    }

    // ------------------------------------------------------------------ etat de besoin

    /** {@code supplierCode} (facultatif) : fiche fournisseur d'inventaire active ; son nom remplit {@code supplier}. */
    public record NeedLineRequest(@NotBlank String designation, BigDecimal quantity, String unit, BigDecimal unitPrice,
                                  String natureCode, String supplier, String supplierCode) {
        public NeedLineRequest(String designation, BigDecimal quantity, String unit, BigDecimal unitPrice, String natureCode, String supplier) {
            this(designation, quantity, unit, unitPrice, natureCode, supplier, null);
        }
    }

    public record NeedRequest(@NotBlank String subject, String department, String locationCode, String costCenter, FundUrgency urgency,
                              LocalDate wantedDate, @NotEmpty @Valid List<NeedLineRequest> lines, String attachments) {
    }

    public record NeedLine(Integer lineNo, String designation, BigDecimal quantity, String unit, BigDecimal unitPrice, BigDecimal totalAmount,
                           String natureCode, String supplier, String transformedRequestNumber, String supplierCode) {
        public NeedLine(Integer lineNo, String designation, BigDecimal quantity, String unit, BigDecimal unitPrice, BigDecimal totalAmount,
                        String natureCode, String supplier, String transformedRequestNumber) {
            this(lineNo, designation, quantity, unit, unitPrice, totalAmount, natureCode, supplier, transformedRequestNumber, null);
        }
    }

    public record Need(String number, String requester, String department, String locationCode, String costCenter, String subject,
                       FundUrgency urgency, LocalDate wantedDate, FundNeedStatus status, BigDecimal totalAmount, String totalInWords,
                       Instant submittedAt, String decidedBy, Instant decidedAt, String decisionNote, String attachments, Instant createdAt,
                       List<NeedLine> lines) {
    }

    /** Transformation en demande de fonds : toutes les lignes, ou celles indiquees. */
    public record TransformNeed(@NotNull FundRequestType type, List<Integer> lineNos, String registerCode) {
    }

    // ------------------------------------------------------------------ demande de fonds

    /** {@code supplierCode} (facultatif) : fiche fournisseur d'inventaire active ; son nom remplit {@code supplier}. */
    public record RequestLine(String natureCode, @NotBlank String designation, @NotNull BigDecimal amount, String supplier, String section,
                              String supplierCode) {
        public RequestLine(String natureCode, String designation, BigDecimal amount, String supplier, String section) {
            this(natureCode, designation, amount, supplier, section, null);
        }
    }

    public record RequestInput(
            @NotNull FundRequestType type,
            @NotBlank String subject,
            String requesterDepartment,
            String requesterPhone,
            FundBeneficiaryType beneficiaryType,
            String beneficiaryCode,
            String beneficiaryName,
            String beneficiaryIdNumber,
            String beneficiaryPhone,
            String registerCode,
            FundDeliveryMode deliveryMode,
            LocalDate wantedDate,
            String missionDestination,
            LocalDate missionStart,
            LocalDate missionEnd,
            Boolean unplanned,
            String unplannedReason,
            @NotEmpty @Valid List<RequestLine> lines,
            String attachments,
            /** Vrai : la demande est soumise aussitot. */
            Boolean submit,
            /** Prevision budgetaire couverte (lot F16) ; sinon depense non prevue avec motif. */
            String forecastNumber,
            String unplannedCode
    ) {
        public RequestInput(FundRequestType type, String subject, String requesterDepartment, String requesterPhone, FundBeneficiaryType beneficiaryType,
                            String beneficiaryCode, String beneficiaryName, String beneficiaryIdNumber, String beneficiaryPhone, String registerCode,
                            FundDeliveryMode deliveryMode, LocalDate wantedDate, String missionDestination, LocalDate missionStart, LocalDate missionEnd,
                            Boolean unplanned, String unplannedReason, List<RequestLine> lines, String attachments, Boolean submit) {
            this(type, subject, requesterDepartment, requesterPhone, beneficiaryType, beneficiaryCode, beneficiaryName, beneficiaryIdNumber,
                    beneficiaryPhone, registerCode, deliveryMode, wantedDate, missionDestination, missionStart, missionEnd, unplanned, unplannedReason,
                    lines, attachments, submit, null, null);
        }
    }

    public record Decision(String comment, BigDecimal reducedAmount) {
    }

    public record Disburse(@NotBlank String sessionNumber, BigDecimal amount, String signedByName, String signature) {
    }

    public record CashBack(@NotBlank String sessionNumber, BigDecimal amount, String signedByName, String signature) {
    }

    public record LinkTransfer(@NotBlank String transferOrderNumber) {
    }

    public record Approval(Integer level, String approver, FundApprovalDecision decision, BigDecimal amountBefore, BigDecimal amountAfter,
                           String comment, String delegatedFrom, Instant decidedAt) {
    }

    public record Disbursement(FundDisbursementKind kind, String movementNumber, String sessionNumber, BigDecimal amount, String signedByName,
                               String createdBy, Instant createdAt) {
    }

    public record ExpenseLine(String number, Integer lineNo, LocalDate expenseDate, String natureCode, String supplier, String designation,
                              BigDecimal amount, BigDecimal vatAmount, String section, String documentReference, Boolean honourStatement,
                              Integer requestLineNo, ExpenseLineStatus status, String rejectReason, String enteredBy, String validatedBy,
                              Instant validatedAt, String supplierCode) {
        public ExpenseLine(String number, Integer lineNo, LocalDate expenseDate, String natureCode, String supplier, String designation,
                           BigDecimal amount, BigDecimal vatAmount, String section, String documentReference, Boolean honourStatement,
                           Integer requestLineNo, ExpenseLineStatus status, String rejectReason, String enteredBy, String validatedBy,
                           Instant validatedAt) {
            this(number, lineNo, expenseDate, natureCode, supplier, designation, amount, vatAmount, section, documentReference, honourStatement,
                    requestLineNo, status, rejectReason, enteredBy, validatedBy, validatedAt, null);
        }
    }

    /** Ce que la demande devient, affiche avant l'envoi et au fil de l'eau. */
    public record Insight(Integer requiredLevels, List<String> levelLabels, BigDecimal registerBalance, Boolean registerCanCover,
                          List<String> openAdvances, Boolean blockedByOverdueAdvance, String warning,
                          /** Impact budgetaire (lot F16) : lignes touchees, disponible avant et apres, previsions ouvertes. */
                          com.sni.bokaticowork.core.accounting.BudgetControl.Impact budget, String budgetApproval) {
    }

    public record Request(
            String number, FundRequestType type, FundRequestStatus status, String requester, String requesterDepartment, String requesterPhone,
            FundBeneficiaryType beneficiaryType, String beneficiaryCode, String beneficiaryName, String beneficiaryIdNumber, String beneficiaryPhone,
            String registerCode, String subject, BigDecimal totalAmount, String totalInWords, BigDecimal approvedAmount, BigDecimal disbursedAmount,
            BigDecimal justifiedAmount, BigDecimal returnedAmount, BigDecimal complementAmount, BigDecimal remainingToJustify,
            BigDecimal remainingToDisburse, FundDeliveryMode deliveryMode, LocalDate wantedDate, String missionDestination, LocalDate missionStart,
            LocalDate missionEnd, Boolean unplanned, String unplannedReason, String needStatementNumber, String transferOrderNumber,
            Integer requiredLevels, Integer currentLevel, String lastComment, Instant submittedAt, Instant approvedAt, Instant expiresAt,
            Instant disbursedAt, LocalDate justificationDueDate, Instant settledAt, String legacyRequestNumber, String attachments,
            Instant createdAt, List<RequestLine> lines, List<Approval> timeline, List<Disbursement> disbursements, List<ExpenseLine> justification,
            Insight insight) {
    }

    // ------------------------------------------------------------------ justification

    public record ExpenseInput(@NotNull LocalDate expenseDate, @NotBlank String natureCode, String supplier, @NotBlank String designation,
                               @NotNull BigDecimal amount, BigDecimal vatAmount, String section, String documentReference, Boolean honourStatement,
                               Integer requestLineNo, String supplierCode) {
        public ExpenseInput(LocalDate expenseDate, String natureCode, String supplier, String designation, BigDecimal amount, BigDecimal vatAmount,
                            String section, String documentReference, Boolean honourStatement, Integer requestLineNo) {
            this(expenseDate, natureCode, supplier, designation, amount, vatAmount, section, documentReference, honourStatement, requestLineNo, null);
        }
    }

    public record Reject(@NotBlank String reason) {
    }

    public record Delegation(@NotBlank String delegate, @NotNull Integer level, @NotNull LocalDate startsOn, @NotNull LocalDate endsOn) {
    }

    public record DelegationView(Long id, String delegator, String delegate, Integer level, LocalDate startsOn, LocalDate endsOn) {
    }

    public record AdvanceRow(String number, String beneficiaryCode, String beneficiaryName, BigDecimal disbursed, BigDecimal justified,
                             BigDecimal returned, BigDecimal remaining, LocalDate dueDate, Long daysOverdue, FundRequestStatus status) {
    }
}
