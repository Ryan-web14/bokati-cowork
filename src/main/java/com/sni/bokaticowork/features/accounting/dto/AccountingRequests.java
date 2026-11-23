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
            Boolean validate,
            /** Lot K1 (S4) : motif d'une modification d'une piece automatique, garde avec la trace. */
            String changeReason
    ) {
        public Entry(String journalCode, LocalDate entryDate, LocalDate documentDate, String label, String sourceType, String sourceCode,
                     List<EntryLine> lines, Boolean validate) {
            this(journalCode, entryDate, documentDate, label, sourceType, sourceCode, lines, validate, null);
        }
    }

    public record Reverse(@NotBlank String reason, LocalDate date) {
    }

    /**
     * Ecriture complementaire qui regularise une piece acceptee desequilibree. Lot K1 (S6) : date
     * choisie (defaut : aujourd'hui), dans une periode ouverte.
     */
    public record Regularize(@NotBlank String label, @NotEmpty @Valid List<EntryLine> lines, LocalDate date) {
        public Regularize(String label, List<EntryLine> lines) {
            this(label, lines, null);
        }
    }

    /**
     * Lot K1 (S2) : reclassement de lignes d'une piece validee. Chaque ligne sort du compte (ou du
     * tiers) errone et va sur le bon, dans une piece OD liee a la piece d'origine.
     */
    public record Reclassify(@NotBlank String reason, LocalDate date, @NotEmpty @Valid List<ReclassLine> lines) {
    }

    /**
     * Une ligne a reclasser : numero de ligne de la piece d'origine, nouveau compte et/ou nouveau
     * tiers, montant (defaut : toute la ligne), ventilation analytique de la nouvelle ligne (defaut :
     * celle d'origine).
     */
    public record ReclassLine(@NotNull Integer lineNo, String accountNumber, String thirdPartyCode, BigDecimal amount,
                              java.util.Map<String, String> analytic) {
    }

    /**
     * Lot K1 (S2) : correction d'une piece validee entierement fausse, contre-passation et piece
     * juste en une action. Piece manuelle : lignes de la piece juste obligatoires. Piece automatique
     * sans lignes : recomptabilisation depuis l'evenement, donnees relues a la source.
     */
    public record Correct(@NotBlank String reason, LocalDate date, String journalCode, String label, @Valid List<EntryLine> lines) {
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
