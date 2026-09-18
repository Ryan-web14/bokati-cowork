package com.sni.bokaticowork.features.payment.control.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * L'etat de rapprochement entre l'encours des portefeuilles et la tresorerie.
 *
 * <p>Un portefeuille credite est une dette de l'etablissement envers son titulaire. Personne
 * n'impose de cantonner ces fonds · c'est la contrepartie du choix de ne pas etre emetteur de
 * monnaie electronique · donc c'est a l'etablissement de verifier qu'il peut honorer les prestations
 * deja payees. Un ecart persistant signifie que des avances clients ont finance autre chose que ce
 * pour quoi elles ont ete versees.</p>
 *
 * <p>L'encours est calcule par le systeme. Le solde du compte d'avances et la tresorerie disponible
 * sont saisis : ils vivent dans la comptabilite et la banque, pas ici. Le rapprochement est
 * precisement le moment ou les trois se rencontrent.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_treasury_reconciliation")
public class WalletTreasuryReconciliation {

    public enum Status {
        BALANCED, VARIANCE, UNDER_REVIEW
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "reconciliation_number", nullable = false, unique = true, length = 100)
    private String reconciliationNumber;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    /** Somme des soldes comptables · ce que l'etablissement doit a ses titulaires. */
    @Column(name = "total_wallet_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalWalletBalance;

    @Column(name = "total_held_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalHeldBalance = BigDecimal.ZERO;

    @Column(name = "wallet_count", nullable = false)
    @Builder.Default
    private Integer walletCount = 0;

    /** Solde du compte d'avances au passif · saisi depuis la comptabilite. */
    @Column(name = "ledger_account_balance", precision = 19, scale = 4)
    private BigDecimal ledgerAccountBalance;

    /** Tresorerie reellement disponible · saisie depuis la banque. */
    @Column(name = "available_cash", precision = 19, scale = 4)
    private BigDecimal availableCash;

    /** Tresorerie rapportee a l'encours · en dessous du seuil, la direction est alertee. */
    @Column(name = "coverage_ratio", precision = 9, scale = 4)
    private BigDecimal coverageRatio;

    @Column(name = "variance", precision = 19, scale = 4)
    private BigDecimal variance;

    @Column(name = "variance_explained", nullable = false)
    @Builder.Default
    private Boolean varianceExplained = Boolean.FALSE;

    @Column(name = "explained_by", length = 500)
    private String explainedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private Status status = Status.BALANCED;

    @Column(name = "prepared_by", nullable = false, length = 120)
    private String preparedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
