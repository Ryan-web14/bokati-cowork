package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_register")
public class CashRegister {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "register_code", nullable = false, unique = true, length = 100)
    private String registerCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location_code", length = 120)
    private String locationCode;

    @Column(name = "business_entity_code", length = 120)
    private String businessEntityCode;

    @Column(name = "device_code", length = 120)
    private String deviceCode;

    /**
     * Caisse tenue par le systeme · aucune saisie manuelle n'y est acceptee.
     *
     * <p>Une caisse automatique n'a pas de caissier a qui demander des comptes. Y laisser entrer une
     * ecriture a la main reviendrait a creer de l'argent dont personne ne repond, dans le seul
     * endroit ou le rapprochement tient precisement parce que tout y est ecrit par la machine.</p>
     */
    @Column(name = "system_managed", nullable = false)
    @Builder.Default
    private Boolean systemManaged = Boolean.FALSE;

    /**
     * Unique moyen de paiement admis sur cette caisse · nul si elle les accepte tous.
     *
     * <p>C'est ce qui fait de {@code CSR-AUTO-WALLET} une caisse de portefeuille et rien d'autre :
     * son total est celui des mouvements de portefeuille, et il n'a pas a etre demele d'especes ou
     * de mobile money pour etre rapproche.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "restricted_to_method", length = 40)
    private com.sni.bokaticowork.features.payment.enums.PaymentMethod restrictedToMethod;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "cash_control_enabled", nullable = false)
    private Boolean cashControlEnabled;

    @Column(name = "max_cash_amount", precision = 19, scale = 4)
    private java.math.BigDecimal maxCashAmount;

    @Column(name = "manager_email", length = 180)
    private String managerEmail;

    @Column(name = "last_anomaly_alert_sent_at")
    private Instant lastAnomalyAlertSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (active == null) {
            active = true;
        }
        if (cashControlEnabled == null) {
            cashControlEnabled = true;
        }
        if (systemManaged == null) {
            systemManaged = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
