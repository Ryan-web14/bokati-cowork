package com.sni.bokaticowork.features.accounting.dto;

import com.sni.bokaticowork.features.accounting.enums.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Reponses du module comptable (lot F1). */
public final class AccountingResponses {

    private AccountingResponses() {
    }

    public record Account(
            String number,
            String label,
            String parentNumber,
            String classCode,
            AccountNature nature,
            AccountSide normalSide,
            Boolean reconcilable,
            ThirdPartyType thirdPartyType,
            TreasuryType treasuryType,
            Boolean analyticRequired,
            String taxCode,
            String statementLineCode,
            AccountStatus status,
            String mergedInto,
            Boolean hasPostings,
            Integer childCount,
            String createdBy,
            Instant createdAt
    ) {
    }

    /** Valeurs proposees pour un nouveau compte sous un parent. */
    public record AccountProposal(
            String parentNumber,
            String proposedNumber,
            String classCode,
            AccountNature nature,
            AccountSide normalSide,
            Boolean reconcilable,
            ThirdPartyType thirdPartyType,
            TreasuryType treasuryType,
            Boolean analyticRequired,
            String taxCode,
            String statementLineCode,
            Boolean needsApproval
    ) {
    }

    public record AccountHistory(String field, String oldValue, String newValue, String reason, String changedBy, Instant changedAt) {
    }

    public record ThirdParty(String accountNumber, ThirdPartyType partyType, String partyCode, String label, Instant createdAt) {
    }

    public record Period(String code, LocalDate startDate, LocalDate endDate, PeriodStatus status, Instant lockedAt, String lockedBy,
                         Instant closedAt, String closedBy, Long pendingEntries, Long draftEntries) {
    }

    public record FiscalYear(String code, LocalDate startDate, LocalDate endDate, FiscalYearStatus status, List<Period> periods) {
    }

    public record Journal(String code, String label, JournalType type, String treasuryAccountNumber, NumberingReset numberingReset,
                          Boolean autoValidate, List<String> allowedAccounts, String cashRegisterCode, Boolean active) {
    }

    public record EntryLine(
            Long id,
            Integer lineNo,
            String accountNumber,
            String accountLabel,
            ThirdPartyType thirdPartyType,
            String thirdPartyCode,
            String label,
            BigDecimal debit,
            BigDecimal credit,
            String currency,
            BigDecimal foreignAmount,
            BigDecimal rate,
            String taxCode,
            BigDecimal taxBase,
            LocalDate dueDate,
            String reference,
            String letteringCode,
            BigDecimal letteredAmount
    ) {
    }

    public record Entry(
            String entryNumber,
            String provisionalNumber,
            String journalCode,
            String fiscalYearCode,
            String periodCode,
            LocalDate entryDate,
            LocalDate originalDate,
            LocalDate documentDate,
            String label,
            EntryOrigin origin,
            String sourceType,
            String sourceCode,
            EntryStatus status,
            Boolean acceptedUnbalanced,
            BigDecimal unbalancedAmount,
            PendingReason pendingReason,
            String pendingDetail,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            BigDecimal difference,
            String createdBy,
            Instant createdAt,
            String validatedBy,
            Instant validatedAt,
            String reversalOf,
            String reversedByEntry,
            String reversalReason,
            String regularizationOf,
            String regularizedBy,
            String rejectedBy,
            String rejectionReason,
            String contentHash,
            List<EntryLine> lines,
            List<String> messages,
            /** Lot K1 : piece corrigee par celle-ci, nature de la correction (RECLASS, CORRECTION, REPOST), piece qui la corrige. */
            String correctionOf,
            String correctionKind,
            String correctedBy,
            /** Lot K1 (S4) : modification a la main d'une piece automatique, et lignes produites par le schema avant. */
            String manuallyChangedBy,
            Instant manuallyChangedAt,
            String changeReason,
            List<GeneratedLine> generatedLines,
            /** Lot K1 (S11) : brouillard de correction qui attend la validation d'un autre comptable. */
            Boolean awaitingSecondValidation
    ) {
    }

    /**
     * Lot K1 : resultat de « Corriger ». Piece manuelle : contre-passation et piece juste. Piece
     * automatique : contre-passation, evenement recomptabilise et nouvelle piece.
     */
    public record Correction(Entry reversal, Entry correction, com.sni.bokaticowork.features.accounting.dto.PostingDtos.Event event,
                             List<String> messages) {
    }

    /** Lot K1 (S4) : ligne telle que le schema l'avait produite. */
    public record GeneratedLine(String accountNumber, String thirdPartyCode, String label, BigDecimal debit, BigDecimal credit,
                                String taxCode, String reference) {
    }

    public record Lettering(
            Long id,
            String code,
            String accountNumber,
            String thirdPartyCode,
            LetteringKind kind,
            LetteringStatus status,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            BigDecimal difference,
            String differenceEntryNumber,
            String letteredBy,
            Instant letteredAt,
            String unletteredBy,
            Instant unletteredAt,
            String unletteringReason,
            List<Long> lineIds
    ) {
    }

    /** Ligne ouverte (non lettree ou partiellement lettree) d'un compte lettrable. */
    public record OpenLine(
            Long lineId,
            String entryNumber,
            LocalDate entryDate,
            String journalCode,
            String label,
            String reference,
            String thirdPartyCode,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal remaining,
            AccountSide side
    ) {
    }

    public record LedgerLine(
            LocalDate date,
            String entryNumber,
            String journalCode,
            String label,
            String thirdPartyCode,
            String reference,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal balance,
            String letteringCode
    ) {
    }

    public record LedgerAccount(
            String accountNumber,
            String accountLabel,
            String thirdPartyCode,
            BigDecimal openingBalance,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            BigDecimal closingBalance,
            List<LedgerLine> lines
    ) {
    }

    public record GeneralLedger(LocalDate from, LocalDate to, List<LedgerAccount> accounts, Instant generatedAt, String generatedBy) {
    }

    /** Ligne de balance : a-nouveaux (ou soldes d'ouverture), mouvements, soldes. */
    public record TrialBalanceLine(
            String accountNumber,
            String accountLabel,
            String classCode,
            BigDecimal openingDebit,
            BigDecimal openingCredit,
            BigDecimal movementDebit,
            BigDecimal movementCredit,
            BigDecimal closingDebit,
            BigDecimal closingCredit
    ) {
    }

    public record TrialBalance(
            LocalDate from,
            LocalDate to,
            String scope,
            List<TrialBalanceLine> lines,
            TrialBalanceLine totals,
            Boolean balanced,
            BigDecimal imbalance,
            List<String> imbalanceEntries,
            Instant generatedAt,
            String generatedBy
    ) {
    }

    public record JournalReport(String journalCode, LocalDate from, LocalDate to, List<Entry> entries, BigDecimal totalDebit,
                                BigDecimal totalCredit, Instant generatedAt, String generatedBy) {
    }

    public record NumberingCheck(
            String journalCode,
            String fiscalYearCode,
            Instant checkedAt,
            String checkedBy,
            Integer firstNumber,
            Integer lastNumber,
            Integer entryCount,
            Integer counterValue,
            List<Integer> gaps,
            List<Integer> duplicates,
            Boolean chainOk,
            Boolean ok,
            String brokenAt
    ) {
    }
}
