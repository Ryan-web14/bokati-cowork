package com.sni.bokaticowork.features.payment.limit.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ce qu'un portefeuille a le droit de recevoir et de depenser, selon le niveau de verification.
 *
 * <p>Le lien avec le niveau de verification n'est pas une formalite : c'est ce qui donne au client
 * une raison de completer son dossier. Un plafond qui se leve quand on fournit une piece est une
 * invitation ; un refus sans explication est un mur.</p>
 *
 * <p>Un plafond nul signifie « pas de limite », et c'est different de zero. Les confondre
 * bloquerait tout au niveau le plus eleve, qui est precisement celui ou l'on veut laisser passer.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_limit_policy", indexes = {
        @Index(name = "idx_wallet_limit_kyc", columnList = "kyc_level,active")
})
public class WalletLimitPolicy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "kyc_level", nullable = false)
    @Builder.Default
    private Integer kycLevel = 1;

    @Column(name = "max_balance", precision = 19, scale = 4)
    private BigDecimal maxBalance;

    @Column(name = "max_single_topup", precision = 19, scale = 4)
    private BigDecimal maxSingleTopup;

    @Column(name = "max_daily_topup", precision = 19, scale = 4)
    private BigDecimal maxDailyTopup;

    @Column(name = "max_monthly_topup", precision = 19, scale = 4)
    private BigDecimal maxMonthlyTopup;

    @Column(name = "max_single_transfer", precision = 19, scale = 4)
    private BigDecimal maxSingleTransfer;

    @Column(name = "max_daily_transfer", precision = 19, scale = 4)
    private BigDecimal maxDailyTransfer;

    @Column(name = "max_monthly_transfer", precision = 19, scale = 4)
    private BigDecimal maxMonthlyTransfer;

    @Column(name = "max_daily_operations")
    private Integer maxDailyOperations;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
