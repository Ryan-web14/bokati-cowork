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

    // ---- Referentiel (lot C1) ----

    /** Deduit a l'enregistrement si absent : caisse automatique si tenue par le systeme, guichet sinon. */
    @Enumerated(EnumType.STRING)
    @Column(name = "register_type", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashRegisterType registerType;

    /** Statut de la caisse ; {@code active} en est le reflet ({@code ACTIVE} seulement). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashRegisterStatus status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** Caisse qui alimente celle-ci et recoit ses remises ; par defaut la caisse principale du site. */
    @Column(name = "parent_register_code", length = 100)
    private String parentRegisterCode;

    /** Moyens acceptes, separes par des virgules (valeurs de {@code PaymentMethod}) ; vide = tous. */
    @Column(name = "accepted_methods", length = 255)
    private String acceptedMethods;

    /** Devises acceptees, separees par des virgules ; XAF par defaut. */
    @Column(name = "accepted_currencies", length = 100)
    private String acceptedCurrencies;

    @Enumerated(EnumType.STRING)
    @Column(name = "cap_behavior", length = 10)
    private com.sni.bokaticowork.features.payment.enums.CashCapBehavior capBehavior;

    @Column(name = "max_out_per_operation", precision = 19, scale = 4)
    private java.math.BigDecimal maxOutPerOperation;

    @Column(name = "max_out_per_day", precision = 19, scale = 4)
    private java.math.BigDecimal maxOutPerDay;

    /** Fonds de caisse standard : ce qu'on garde a la cloture, le reste se remet. */
    @Column(name = "standard_float", precision = 19, scale = 4)
    private java.math.BigDecimal standardFloat;

    /** Destinataires des alertes, separes par des virgules ; remplace {@code managerEmail}. */
    @Column(name = "alert_recipients", length = 1000)
    private String alertRecipients;

    /** Compte de tresorerie (571x) ; vide tant que le socle comptable n'est pas livre. */
    @Column(name = "treasury_account_number", length = 30)
    private String treasuryAccountNumber;

    @Column(name = "default_journal_code", length = 30)
    private String defaultJournalCode;

    @Column(name = "last_count_at")
    private Instant lastCountAt;

    @Column(name = "last_count_amount", precision = 19, scale = 4)
    private java.math.BigDecimal lastCountAmount;

    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    // ---- Fonctionnement des sessions (lot C2) ----

    /** Cloture a l'aveugle : l'attendu n'est montre qu'apres le comptage. */
    @Column(name = "blind_close")
    private Boolean blindClose;

    /** Billetage par coupure obligatoire aux comptages (sinon un total suffit). */
    @Column(name = "count_lines_required")
    private Boolean countLinesRequired;

    /** Tolerance d'ecart en montant : en dessous, l'ecart est comptabilise sans revue. */
    @Column(name = "count_tolerance", precision = 19, scale = 4)
    private java.math.BigDecimal countTolerance;

    /** Tolerance d'ecart en pourcentage de l'attendu (facultative). */
    @Column(name = "count_tolerance_percent", precision = 7, scale = 4)
    private java.math.BigDecimal countTolerancePercent;

    /** Au-dela de cet ecart, un second compteur different du premier est obligatoire. */
    @Column(name = "double_count_threshold", precision = 19, scale = 4)
    private java.math.BigDecimal doubleCountThreshold;

    @Column(name = "max_session_hours")
    private Integer maxSessionHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "month_end_policy", length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashMonthEndPolicy monthEndPolicy;

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
        if (registerType == null) {
            registerType = Boolean.TRUE.equals(systemManaged)
                    ? com.sni.bokaticowork.features.payment.enums.CashRegisterType.AUTOMATIC : com.sni.bokaticowork.features.payment.enums.CashRegisterType.COUNTER;
        }
        if (status == null) {
            status = Boolean.FALSE.equals(active) ? com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.SUSPENDED : com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.ACTIVE;
        }
        active = status == com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.ACTIVE;
        if (acceptedCurrencies == null) {
            acceptedCurrencies = "XAF";
        }
        // Une caisse creee maintenant suit le plan des caisses (lot C2).
        if (blindClose == null) {
            blindClose = !Boolean.TRUE.equals(systemManaged);
        }
        if (countLinesRequired == null) {
            countLinesRequired = !Boolean.TRUE.equals(systemManaged);
        }
        if (countTolerance == null) {
            countTolerance = java.math.BigDecimal.ZERO;
        }
        if (doubleCountThreshold == null) {
            doubleCountThreshold = new java.math.BigDecimal("10000");
        }
        if (maxSessionHours == null) {
            maxSessionHours = 24;
        }
        if (monthEndPolicy == null) {
            monthEndPolicy = com.sni.bokaticowork.features.payment.enums.CashMonthEndPolicy.REQUIRE_COUNT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (status != null) {
            active = status == com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.ACTIVE;
        }
    }
}
