package com.sni.bokaticowork.features.accounting.dto;

import com.sni.bokaticowork.features.accounting.enums.JournalType;
import com.sni.bokaticowork.features.accounting.enums.NumberingReset;
import com.sni.bokaticowork.features.accounting.enums.ThirdPartyType;
import com.sni.bokaticowork.features.accounting.enums.TreasuryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Requetes du module comptable (lot F1). */
public final class AccountingRequests {

    private AccountingRequests() {
    }

    /**
     * Creation d'un compte : le parent et l'intitule suffisent ; le numero est propose (prochain
     * libre sous le parent) et les proprietes sont heritees du parent.
     */
    public record CreateAccount(
            String parentNumber,
            String number,
            @NotBlank String label,
            Boolean reconcilable,
            ThirdPartyType thirdPartyType,
            TreasuryType treasuryType,
            Boolean analyticRequired,
            String taxCode,
            String statementLineCode,
            String reason
    ) {
    }

    /** Modification d'un compte : l'intitule a tout moment ; le reste tant qu'aucune ecriture validee ne le touche. */
    public record UpdateAccount(
            String label,
            Boolean reconcilable,
            ThirdPartyType thirdPartyType,
            TreasuryType treasuryType,
            Boolean analyticRequired,
            String taxCode,
            String statementLineCode,
            String reason
    ) {
    }

    public record Reason(String reason) {
    }

    public record CreateFiscalYear(@NotBlank String code, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {
    }

    public record CreateJournal(
            @NotBlank String code,
            @NotBlank String label,
            @NotNull JournalType type,
            String treasuryAccountNumber,
            NumberingReset numberingReset,
            Boolean autoValidate,
            List<String> allowedAccounts,
            String cashRegisterCode
    ) {
    }

    public record UpdateJournal(
            String label,
            Boolean autoValidate,
            List<String> allowedAccounts,
            Boolean active
    ) {
    }

    /** Une ligne : un debit ou un credit, jamais les deux, jamais negatif. */
    public record EntryLine(
            @NotBlank String accountNumber,
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
            /** Ventilation analytique (lot F9) : axe vers section, ou vers "KEY:code" d'une cle de repartition. */
            java.util.Map<String, String> analytic
    ) {
        public EntryLine(String accountNumber, String thirdPartyCode, String label, BigDecimal debit, BigDecimal credit, String currency,
                         BigDecimal foreignAmount, BigDecimal rate, String taxCode, BigDecimal taxBase, LocalDate dueDate, String reference) {
            this(accountNumber, thirdPartyCode, label, debit, credit, currency, foreignAmount, rate, taxCode, taxBase, dueDate, reference, null);
        }
    }

    /**
     * Piece saisie a la main. Enregistree en brouillard ; {@code validate} la valide aussitot (une
     * piece desequilibree suit alors la politique en vigueur).
     */
    public record Entry(
            @NotBlank String journalCode,
            @NotNull LocalDate entryDate,
            LocalDate documentDate,
            @NotBlank String label,
            String sourceType,
            String sourceCode,
            @NotEmpty @Valid List<EntryLine> lines,
            Boolean validate
    ) {
    }

    public record Reverse(@NotBlank String reason, LocalDate date) {
    }

    /** Ecriture complementaire qui regularise une piece acceptee desequilibree. */
    public record Regularize(@NotBlank String label, @NotEmpty @Valid List<EntryLine> lines) {
    }

    /** Une ligne a lettrer ; sans montant, tout son reste est rapproche. */
    public record LetterLine(@NotNull Long lineId, BigDecimal amount) {
    }

    /**
     * Lettrage manuel. Difference non nulle : lettrage partiel si {@code partial}, ou avec ecart de
     * reglement si {@code withDifference} et sous le seuil parametre.
     */
    public record Letter(
            @NotBlank String accountNumber,
            String thirdPartyCode,
            @NotEmpty @Valid List<LetterLine> lines,
            Boolean partial,
            Boolean withDifference
    ) {
    }
}
