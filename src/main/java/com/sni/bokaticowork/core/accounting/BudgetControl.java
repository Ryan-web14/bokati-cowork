package com.sni.bokaticowork.core.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controle budgetaire des operations (lot F16, analytique-et-budget section 4). Les modules metier
 * (demandes de fonds, achats) ne dependent que de ce contrat ; la comptabilite l'implemente. Sans
 * budget actif pour l'exercice, tout est couvert.
 */
public interface BudgetControl {

    /** Une ligne de depense a controler : nature d'operation (ou compte), section, montant. */
    record Spend(String natureCode, String accountNumber, String sectionCode, BigDecimal amount) {
    }

    /** Couvert, dans la tolerance, en depassement. */
    enum Status { GREEN, ORANGE, RED }

    /** Impact sur une ligne de budget. */
    record LineImpact(Long lineId, String itemCode, String itemLabel, String sectionCode, String controlLevel, BigDecimal budgeted,
                      BigDecimal committed, BigDecimal realized, BigDecimal available, BigDecimal requested, BigDecimal availableAfter,
                      Status status, List<String> openForecasts) {
    }

    /**
     * @param blocked        une ligne bloquante est depassee au-dela de sa tolerance
     * @param unbudgeted     des lignes ne trouvent aucun poste ou aucune ligne de budget
     */
    record Impact(boolean budgetActive, List<LineImpact> lines, boolean blocked, List<String> unbudgeted, List<String> messages) {
    }

    Impact check(List<Spend> spends, LocalDate date);

    /** Engage les montants (verrou sur les lignes) ; rejouable : un engagement existant n'est pas double. */
    void commit(String sourceType, String reference, List<Spend> spends, LocalDate date, String forecastNumber, boolean unplanned,
                String unplannedCode, String unplannedText, String actor);

    /** Solde : la depense est realisee pour {@code realized} ; l'ecart est libere. */
    void settle(String sourceType, String reference, BigDecimal realized);

    /** Libere sans realisation (annulation, expiration, rejet). */
    void release(String sourceType, String reference, boolean expired);

    /** Derogation tracee au blocage (droit BUDGET:OVERRIDE). */
    void recordOverride(String reference, Impact impact, String reason, String actor);
}
