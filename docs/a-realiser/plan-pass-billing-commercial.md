# Implementation Document: Pass Plan System & Commercial Module

**Version:** 2.0  
**Date:** 2026-06-16  
**Migration Flyway actuelle:** V154 → prochaine: V155  
**Branche:** `feature/payment-facturation`

---

## Table des matières

1. [Vision & Architecture](#1-vision--architecture)
2. [Partie 1 — Modèle Pass Plan (refonte)](#2-partie-1--modèle-pass-plan-refonte)
   - [2.1 Comparaison avec Subscription](#21-comparaison-avec-subscription)
   - [2.2 Nouvelles entités de plan](#22-nouvelles-entités-de-plan)
   - [2.3 Entité Pass modifiée](#23-entité-pass-modifiée)
   - [2.4 Entités d'audit et renouvellement](#24-entités-daudit-et-renouvellement)
3. [Migrations Flyway (V155 → V162)](#3-migrations-flyway-v155--v162)
4. [Couche Service — Gestion des plans](#4-couche-service--gestion-des-plans)
5. [Couche Service — Cycle de vie du pass](#5-couche-service--cycle-de-vie-du-pass)
   - [5.1 PassPlanResolver](#51-passplanresolver)
   - [5.2 PassPeriodCalculator](#52-passperiodcalculator)
   - [5.3 PassCodeFactory](#53-passcodefactory)
   - [5.4 PassCreationOperator (refonte)](#54-passcreationoperator-refonte)
   - [5.5 PassBillingSupport](#55-passbillingsupport)
   - [5.6 PassLifecycleOperator (extensions)](#56-passlifecycleoperator-extensions)
   - [5.7 PassRenewalOperator](#57-passrenewaloperator)
   - [5.8 PassEventWriter](#58-passeventwriter)
   - [5.9 PassEmailNotifier](#59-passemailnotifier)
6. [Workers](#6-workers)
7. [Intégration PaymentTransactionWorkflowProcessor](#7-intégration-paymenttransactionworkflowprocessor)
8. [Controllers & DTOs](#8-controllers--dtos)
   - [8.1 PassPlanController](#81-passplancontroller)
   - [8.2 PassController (update)](#82-passcontroller-update)
   - [8.3 DTOs](#83-dtos)
9. [Partie 2 — Module Commercial (extension CRM)](#9-partie-2--module-commercial-extension-crm)
10. [Ordre d'exécution](#10-ordre-dexécution)
11. [Stratégie de test](#11-stratégie-de-test)

---

## 1. Vision & Architecture

### Objectif

Remplacer la création ad-hoc de passes (un pass = un utilisateur, sans structure de plan) par un modèle plan-centré identique à celui des subscriptions :

```
PassPlan       → définit le type de pass (ex. "Day Pass Flex", "Pack 10h")
  └── PassPlanVersion  → version tarifée + durée (ex. "v1 : 1 jour, 5 000 XAF")
        ├── PassPlanPrice        → prix par devise
        └── PassPlanEntitlement  → entitlements accordés par ce plan

Pass (instance) → achat d'un plan par un member/customer/business_entity
  ├── BillableItem → Invoice → PaymentIntent → Paiement
  ├── ContractPDF (déjà implémenté)
  ├── EntitlementGrant (déjà implémenté)
  ├── PassEvent + PassStatusHistory
  └── PassRenewalSchedule (si autoRenew = true)
```

### Différences Pass vs Subscription

| Aspect | Subscription | Pass |
|--------|-------------|------|
| Durée de période | `billingCycle` (MONTHLY, QUARTERLY…) | `duration` + `durationUnit` (1 DAY, 30 DAY, 1 MONTH…) |
| Renouvellement | Rebasing sur calendrier | Décalage de `duration` depuis `validUntil` courant |
| Quota d'usage | Aucun | `maxUses` / `usedCount` |
| Essai | `trialDays` | Non (optionnel futur) |
| Pause | `pausedAt` / `pauseUntil` | Non (scope simplifié) |
| Billing schedule | `billing_schedule` (by subscription) | `pass_renewal_schedule` (by pass) |
| Entitlement source | `subscription_plan_entitlement` | `pass_plan_entitlement` |

### Flux de création (résumé)

```
POST /api/v1/pass-plans/{planCode}/purchase
  ├── Résoudre PassPlanVersion + PassPlanPrice
  ├── Valider owner (KYC si requis)
  ├── Calculer montants (HT, TVA, TTC)
  ├── Créer Pass (status = PENDING_ACTIVATION)
  ├── Créer BillableItem → Invoice (DRAFT → ISSUED) → PaymentIntent
  ├── Publier ContractGenerationEvent (async)
  └── Si totalAmount = 0 → activer immédiatement

[Paiement PawaPay / Wallet / Cash]
  └── Callback → PaymentTransactionWorkflowProcessor
        └── source.type = "PASS" → passService.activate()
              ├── PENDING_ACTIVATION → ACTIVE
              ├── Grant entitlements (depuis PassPlanVersion.entitlements)
              ├── Créer PassRenewalSchedule si autoRenew
              ├── Email confirmation
              └── PassEvent PASS_ACTIVATED
```

---

## 2. Partie 1 — Modèle Pass Plan (refonte)

### 2.1 Comparaison avec Subscription

| Subscription | Pass |
|---|---|
| `subscription_plan` | `pass_plan` *(nouveau)* |
| `subscription_plan_version` | `pass_plan_version` *(nouveau)* |
| `subscription_plan_price` | `pass_plan_price` *(nouveau)* |
| `subscription_plan_entitlement` | `pass_plan_entitlement` *(nouveau)* |
| `subscription` | `subscription_pass` *(modifié)* |
| `subscription_item` | *non nécessaire (pass = 1 item)* |
| `subscription_status_history` | `pass_status_history` *(nouveau)* |
| `subscription_event` | `pass_event` *(nouveau)* |
| `billing_schedule` | `pass_renewal_schedule` *(nouveau)* |

---

### 2.2 Nouvelles entités de plan

#### PassPlan.java
**Table :** `pass_plan`  
**Package :** `features.subscription.subscription.model`

```java
@Entity
@Table(name = "pass_plan", indexes = {
    @Index(name = "idx_pass_plan_code",   columnList = "code"),
    @Index(name = "idx_pass_plan_status", columnList = "status")
})
@SQLDelete(sql = "UPDATE pass_plan SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class PassPlan {
    @Id @IdGeneration
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_type", nullable = false, length = 60)
    private PassType passType;          // DAY_PASS, TIME_PACK, MEETING_ROOM_PACK…

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false, length = 60)
    private TargetAudience targetAudience;  // réutiliser enum existant

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT;   // réutiliser enum existant

    @Column(name = "visible", nullable = false)
    @Builder.Default
    private Boolean visible = Boolean.FALSE;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "required_kyc_level", nullable = false)
    @Builder.Default
    private Integer requiredKycLevel = 1;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist  public void prePersist()  { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   public void preUpdate()   { updatedAt = Instant.now(); }
}
```

---

#### PassPlanVersion.java
**Table :** `pass_plan_version`

```java
@Entity
@Table(name = "pass_plan_version", indexes = {
    @Index(name = "idx_pass_plan_version_plan",   columnList = "plan_id"),
    @Index(name = "idx_pass_plan_version_status", columnList = "status")
})
public class PassPlanVersion {
    @Id @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pass_plan_version_plan"))
    private PassPlan plan;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT;

    // ── durée de validité du pass ──────────────────────────────────
    @Column(name = "duration", nullable = false)
    private Integer duration;               // ex. 1, 30, 90

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false, length = 20)
    private PassDurationUnit durationUnit;  // DAY, WEEK, MONTH, YEAR

    @Column(name = "max_uses")
    private Integer maxUses;               // null = accès illimité

    @Column(name = "auto_renewable", nullable = false)
    @Builder.Default
    private Boolean autoRenewable = Boolean.FALSE;  // ce plan supporte-t-il l'auto-renouvellement?

    @Column(name = "required_kyc_level", nullable = false)
    @Builder.Default
    private Integer requiredKycLevel = 1;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "terms_json", columnDefinition = "jsonb")
    private String termsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist  public void prePersist()  { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   public void preUpdate()   { updatedAt = Instant.now(); }
}
```

**Nouveau enum :** `PassDurationUnit` (même package que `PassStatus`)

```java
public enum PassDurationUnit {
    DAY, WEEK, MONTH, YEAR
}
```

---

#### PassPlanPrice.java
**Table :** `pass_plan_price`

```java
@Entity
@Table(name = "pass_plan_price", indexes = {
    @Index(name = "idx_pass_plan_price_version", columnList = "pass_plan_version_id")
})
public class PassPlanPrice {
    @Id @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_plan_version_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pass_plan_price_version"))
    private PassPlanVersion passVersion;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;          // prix de base (HT ou TTC)

    @Column(name = "setup_fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal setupFee = BigDecimal.ZERO;

    @Column(name = "deposit_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "tax_included", nullable = false)
    @Builder.Default
    private Boolean taxIncluded = Boolean.TRUE;

    @Column(name = "tax_code", length = 80)
    private String taxCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist  public void prePersist()  { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   public void preUpdate()   { updatedAt = Instant.now(); }
}
```

> Pas de `billingCycle` : la durée est portée par `PassPlanVersion.duration` + `durationUnit`.  
> Un pass peut avoir plusieurs prix selon la devise (XAF, EUR…).

---

#### PassPlanEntitlement.java
**Table :** `pass_plan_entitlement`

Analogique à `subscription_plan_entitlement` — définit ce que chaque version de plan accorde.

```java
@Entity
@Table(name = "pass_plan_entitlement", indexes = {
    @Index(name = "idx_pass_plan_ent_version", columnList = "pass_plan_version_id")
})
public class PassPlanEntitlement {
    @Id @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_plan_version_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pass_plan_ent_version"))
    private PassPlanVersion passVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_definition_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pass_plan_ent_definition"))
    private EntitlementDefinition entitlementDefinition;

    @Column(name = "quantity", precision = 19, scale = 4)
    private BigDecimal quantity;         // null si unlimited

    @Column(name = "unlimited", nullable = false)
    @Builder.Default
    private Boolean unlimited = Boolean.FALSE;

    @Column(name = "rollover_allowed", nullable = false)
    @Builder.Default
    private Boolean rolloverAllowed = Boolean.FALSE;

    @Column(name = "rollover_limit", precision = 19, scale = 4)
    private BigDecimal rolloverLimit;

    @Column(name = "valid_for_days")
    private Integer validForDays;       // null = validité = validité du pass

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist  public void prePersist()  { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   public void preUpdate()   { updatedAt = Instant.now(); }
}
```

---

### 2.3 Entité Pass modifiée

**Fichier :** `features/subscription/subscription/model/Pass.java`

Champs à **ajouter** (les champs existants restent inchangés) :

```java
// ── Lien vers le plan ─────────────────────────────────────────────
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "pass_plan_version_id",
            foreignKey = @ForeignKey(name = "fk_subscription_pass_plan_version_new"))
private PassPlanVersion passVersion;   // nouveau — remplace à terme plan_version_id

// ── Billing ───────────────────────────────────────────────────────
@Column(name = "currency", length = 3)
private String currency;

@Column(name = "subtotal_amount", precision = 19, scale = 4)
@Builder.Default
private BigDecimal subtotalAmount = BigDecimal.ZERO;

@Column(name = "tax_amount", precision = 19, scale = 4)
@Builder.Default
private BigDecimal taxAmount = BigDecimal.ZERO;

@Column(name = "total_amount", precision = 19, scale = 4)
@Builder.Default
private BigDecimal totalAmount = BigDecimal.ZERO;

// ── Renouvellement ────────────────────────────────────────────────
@Column(name = "auto_renew", nullable = false)
@Builder.Default
private Boolean autoRenew = Boolean.FALSE;

@Column(name = "next_renewal_date")
private Instant nextRenewalDate;

@Column(name = "renewal_count", nullable = false)
@Builder.Default
private Integer renewalCount = 0;

// ── Annulation ────────────────────────────────────────────────────
@Column(name = "cancelled_at")
private Instant cancelledAt;

@Column(name = "cancellation_reason", columnDefinition = "text")
private String cancellationReason;
```

**PassStatus** — nouveaux statuts :

```java
public enum PassStatus {
    DRAFT,
    PENDING_ACTIVATION,   // pass créé, paiement attendu
    ACTIVE,
    PARTIALLY_USED,
    CONSUMED,
    PAST_DUE,             // renouvellement échoué
    EXPIRED,
    CANCELLED,
    SUSPENDED
}
```

---

### 2.4 Entités d'audit et renouvellement

#### PassEvent.java — `pass_event`

Analogique à `subscription_event` :

```java
@Entity
@Table(name = "pass_event", indexes = {
    @Index(name = "idx_pass_event_pass", columnList = "pass_id"),
    @Index(name = "idx_pass_event_type", columnList = "event_type")
})
public class PassEvent {
    @Id @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pass_event_pass"))
    private Pass pass;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;   // PASS_CREATED, PASS_ACTIVATED, PASS_RENEWED…

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist  public void prePersist()  { occurredAt = Instant.now(); }
}
```

**Enum PassEventType :**

```java
public enum PassEventType {
    PASS_CREATED,
    PASS_ACTIVATED,
    PASS_RENEWED,
    PASS_SUSPENDED,
    PASS_CANCELLED,
    PASS_EXPIRED,
    PASS_PAST_DUE,
    ENTITLEMENTS_GRANTED,
    BILLING_SCHEDULED,
    RENEWAL_SCHEDULED
}
```

---

#### PassStatusHistory.java — `pass_status_history`

```java
@Entity
@Table(name = "pass_status_history", indexes = {
    @Index(name = "idx_pass_status_history_pass", columnList = "pass_id")
})
public class PassStatusHistory {
    @Id @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pass_status_history_pass"))
    private Pass pass;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 40)
    private String toStatus;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "changed_by", length = 120)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist  public void prePersist()  { changedAt = Instant.now(); }
}
```

---

#### PassRenewalSchedule.java — `pass_renewal_schedule`

Analogique à `billing_schedule` mais pour les passes :

```java
@Entity
@Table(name = "pass_renewal_schedule", indexes = {
    @Index(name = "idx_pass_renewal_pass",   columnList = "pass_id"),
    @Index(name = "idx_pass_renewal_status", columnList = "status,next_renewal_date")
})
public class PassRenewalSchedule {
    @Id @IdGeneration
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_pass_renewal_pass"))
    private Pass pass;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false, length = 20)
    private PassDurationUnit durationUnit;

    @Column(name = "next_renewal_date", nullable = false)
    private Instant nextRenewalDate;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private BillingScheduleStatus status = BillingScheduleStatus.ACTIVE;  // réutiliser enum

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist  public void prePersist()  { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   public void preUpdate()   { updatedAt = Instant.now(); }
}
```

---

## 3. Migrations Flyway (V155 → V162)

### V155 — pass_plan et pass_plan_version

```sql
-- Tables du catalogue de plans de passes

CREATE TABLE IF NOT EXISTS pass_plan (
    id                  BIGINT          PRIMARY KEY,
    code                VARCHAR(80)     NOT NULL UNIQUE,
    name                VARCHAR(255)    NOT NULL,
    description         TEXT,
    pass_type           VARCHAR(60)     NOT NULL,
    target_audience     VARCHAR(60)     NOT NULL,
    status              VARCHAR(40)     NOT NULL DEFAULT 'DRAFT',
    visible             BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order          INTEGER,
    required_kyc_level  INTEGER         NOT NULL DEFAULT 1,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pass_plan_code   ON pass_plan(code)   WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_pass_plan_status ON pass_plan(status) WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS pass_plan_version (
    id                  BIGINT          PRIMARY KEY,
    plan_id             BIGINT          NOT NULL REFERENCES pass_plan(id),
    version_number      INTEGER         NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         TEXT,
    status              VARCHAR(40)     NOT NULL DEFAULT 'DRAFT',
    duration            INTEGER         NOT NULL,
    duration_unit       VARCHAR(20)     NOT NULL,
    max_uses            INTEGER,
    auto_renewable      BOOLEAN         NOT NULL DEFAULT FALSE,
    required_kyc_level  INTEGER         NOT NULL DEFAULT 1,
    effective_from      DATE,
    effective_to        DATE,
    terms_json          JSONB,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pass_plan_version_number UNIQUE (plan_id, version_number),
    CONSTRAINT ck_pass_plan_version_duration_unit CHECK (
        duration_unit IN ('DAY', 'WEEK', 'MONTH', 'YEAR')
    )
);

CREATE INDEX IF NOT EXISTS idx_pass_plan_version_plan   ON pass_plan_version(plan_id);
CREATE INDEX IF NOT EXISTS idx_pass_plan_version_status ON pass_plan_version(status);
```

---

### V156 — pass_plan_price et pass_plan_entitlement

```sql
CREATE TABLE IF NOT EXISTS pass_plan_price (
    id                    BIGINT          PRIMARY KEY,
    pass_plan_version_id  BIGINT          NOT NULL REFERENCES pass_plan_version(id),
    currency              VARCHAR(3)      NOT NULL,
    amount                NUMERIC(19,4)   NOT NULL,
    setup_fee             NUMERIC(19,4)   NOT NULL DEFAULT 0,
    deposit_amount        NUMERIC(19,4)   NOT NULL DEFAULT 0,
    tax_included          BOOLEAN         NOT NULL DEFAULT TRUE,
    tax_code              VARCHAR(80),
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pass_plan_price_version_currency UNIQUE (pass_plan_version_id, currency),
    CONSTRAINT ck_pass_plan_price_amounts CHECK (amount >= 0 AND setup_fee >= 0 AND deposit_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pass_plan_price_version ON pass_plan_price(pass_plan_version_id);

CREATE TABLE IF NOT EXISTS pass_plan_entitlement (
    id                          BIGINT          PRIMARY KEY,
    pass_plan_version_id        BIGINT          NOT NULL REFERENCES pass_plan_version(id),
    entitlement_definition_id   BIGINT          NOT NULL REFERENCES entitlement_definition(id),
    quantity                    NUMERIC(19,4),
    unlimited                   BOOLEAN         NOT NULL DEFAULT FALSE,
    rollover_allowed            BOOLEAN         NOT NULL DEFAULT FALSE,
    rollover_limit              NUMERIC(19,4),
    valid_for_days              INTEGER,
    priority                    INTEGER         NOT NULL DEFAULT 100,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_pass_plan_ent_qty CHECK (unlimited = TRUE OR quantity IS NOT NULL),
    CONSTRAINT ck_pass_plan_ent_non_neg CHECK (quantity IS NULL OR quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pass_plan_ent_version    ON pass_plan_entitlement(pass_plan_version_id);
CREATE INDEX IF NOT EXISTS idx_pass_plan_ent_definition ON pass_plan_entitlement(entitlement_definition_id);
```

---

### V157 — Modification subscription_pass

```sql
-- Nouveaux champs sur subscription_pass

ALTER TABLE subscription_pass
    ADD COLUMN IF NOT EXISTS pass_plan_version_id  BIGINT,
    ADD COLUMN IF NOT EXISTS currency              VARCHAR(3),
    ADD COLUMN IF NOT EXISTS subtotal_amount       NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_amount            NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS auto_renew            BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS next_renewal_date     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS renewal_count         INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cancelled_at          TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cancellation_reason   TEXT;

ALTER TABLE subscription_pass
    ADD CONSTRAINT fk_subscription_pass_plan_version_new
        FOREIGN KEY (pass_plan_version_id) REFERENCES pass_plan_version(id);

-- Nouveau statut dans la contrainte
ALTER TABLE subscription_pass
    DROP CONSTRAINT IF EXISTS ck_subscription_pass_status;

ALTER TABLE subscription_pass
    ADD CONSTRAINT ck_subscription_pass_status CHECK (status IN (
        'DRAFT', 'PENDING_ACTIVATION', 'ACTIVE',
        'PARTIALLY_USED', 'CONSUMED', 'PAST_DUE',
        'EXPIRED', 'CANCELLED', 'SUSPENDED'
    ));

CREATE INDEX IF NOT EXISTS idx_pass_plan_version_ref
    ON subscription_pass(pass_plan_version_id);
CREATE INDEX IF NOT EXISTS idx_pass_auto_renew
    ON subscription_pass(auto_renew, next_renewal_date)
    WHERE auto_renew = TRUE AND status = 'ACTIVE';
```

---

### V158 — pass_status_history et pass_event

```sql
CREATE TABLE IF NOT EXISTS pass_status_history (
    id          BIGINT          PRIMARY KEY,
    pass_id     BIGINT          NOT NULL REFERENCES subscription_pass(id),
    from_status VARCHAR(40),
    to_status   VARCHAR(40)     NOT NULL,
    reason      TEXT,
    changed_by  VARCHAR(120),
    changed_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pass_status_history_pass ON pass_status_history(pass_id);

CREATE TABLE IF NOT EXISTS pass_event (
    id           BIGINT          PRIMARY KEY,
    pass_id      BIGINT          NOT NULL REFERENCES subscription_pass(id),
    event_type   VARCHAR(80)     NOT NULL,
    payload_json JSONB,
    occurred_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pass_event_pass ON pass_event(pass_id);
CREATE INDEX IF NOT EXISTS idx_pass_event_type ON pass_event(event_type);
```

---

### V159 — pass_renewal_schedule

```sql
CREATE TABLE IF NOT EXISTS pass_renewal_schedule (
    id                   BIGINT          PRIMARY KEY,
    pass_id              BIGINT          NOT NULL UNIQUE REFERENCES subscription_pass(id),
    duration             INTEGER         NOT NULL,
    duration_unit        VARCHAR(20)     NOT NULL,
    next_renewal_date    TIMESTAMPTZ     NOT NULL,
    current_period_start TIMESTAMPTZ,
    current_period_end   TIMESTAMPTZ,
    status               VARCHAR(40)     NOT NULL DEFAULT 'ACTIVE',
    retry_count          INTEGER         NOT NULL DEFAULT 0,
    last_attempt_at      TIMESTAMPTZ,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pass_renewal_pass   ON pass_renewal_schedule(pass_id);
CREATE INDEX IF NOT EXISTS idx_pass_renewal_status ON pass_renewal_schedule(status, next_renewal_date);
```

---

### V160 — Séquences

```sql
INSERT INTO sequence_definition
    (sequence_code, prefix, suffix, padding_length, increment_step, reset_strategy, last_value, created_at, updated_at)
VALUES
    ('pass_plan',         'PPL-',  NULL, 5, 1, 'NEVER',  0, NOW(), NOW()),
    ('pass_plan_version', 'PPV-',  NULL, 5, 1, 'NEVER',  0, NOW(), NOW()),
    ('pass_purchase',     'PASS-', NULL, 6, 1, 'YEARLY', 0, NOW(), NOW()),
    ('pass_event',        'PEV-',  NULL, 6, 1, 'YEARLY', 0, NOW(), NOW()),
    ('pass_history',      'PSH-',  NULL, 6, 1, 'YEARLY', 0, NOW(), NOW())
ON CONFLICT (sequence_code) DO NOTHING;
```

---

## 4. Couche Service — Gestion des plans

### PassPlanService (interface)

**Package :** `features.subscription.subscription.service.interfaces`

```java
public interface PassPlanService {
    // Plans
    PassPlanResponse createPlan(CreatePassPlanRequest request);
    PassPlanResponse updatePlan(String planCode, UpdatePassPlanRequest request);
    PassPlanResponse getPlan(String planCode);
    PassPlanResponse publishPlan(String planCode);
    PassPlanResponse archivePlan(String planCode);
    PaginatedResponse<PassPlanResponse> searchPlans(PassType passType, PlanStatus status,
                                                     String searchText, Pageable pageable);

    // Versions
    PassPlanVersionResponse createVersion(String planCode, CreatePassPlanVersionRequest request);
    PassPlanVersionResponse getVersion(Long versionId);
    List<PassPlanVersionResponse> listVersions(String planCode);
    PassPlanVersionResponse publishVersion(Long versionId);

    // Prix
    PassPlanPriceResponse setPrice(Long versionId, SetPassPlanPriceRequest request);
    List<PassPlanPriceResponse> listPrices(Long versionId);

    // Entitlements du plan
    PassPlanEntitlementResponse addEntitlement(Long versionId, AddPassPlanEntitlementRequest request);
    void removeEntitlement(Long entitlementId);
    List<PassPlanEntitlementResponse> listEntitlements(Long versionId);
}
```

**PassPlanController** → `/api/v1/pass-plans`  
Permissions : `PASS_PLAN:WRITE`, `PASS_PLAN:READ`

---

## 5. Couche Service — Cycle de vie du pass

### 5.1 PassPlanResolver

**Package :** `features.subscription.subscription.service.support.pass`

Analogique à `SubscriptionPlanResolver` :

```java
@Component
@RequiredArgsConstructor
public class PassPlanResolver {

    private final PassPlanRepository passPlansRepository;
    private final PassPlanVersionRepository passVersionRepository;
    private final PassPlanPriceRepository passPriceRepository;

    public PassPlanVersion resolveVersion(String planCode, Long versionId) {
        PassPlan plan = passPlansRepository.findByCode(planCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Pass plan not found: " + planCode));
        if (plan.getStatus() != PlanStatus.ACTIVE) {
            throw new BadRequestException("Pass plan " + planCode + " is not active");
        }
        if (versionId != null) {
            return passVersionRepository.findById(versionId)
                    .filter(v -> v.getPlan().getId().equals(plan.getId()))
                    .filter(v -> v.getStatus() == PlanStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Pass plan version not found"));
        }
        return passVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(plan, PlanStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active version for pass plan " + planCode));
    }

    public PassPlanPrice resolvePrice(PassPlanVersion version, String currency) {
        String targetCurrency = StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "XAF";
        return passPriceRepository.findByPassVersionAndCurrency(version, targetCurrency)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No price defined for pass plan version " + version.getId()
                        + " in currency " + targetCurrency));
    }
}
```

---

### 5.2 PassPeriodCalculator

**Package :** `features.subscription.subscription.service.support.pass`

```java
@Component
public class PassPeriodCalculator {

    public Instant periodEnd(Instant from, Integer duration, PassDurationUnit unit) {
        if (from == null) throw new BadRequestException("Pass start date is required");
        ZonedDateTime zdt = from.atZone(ZoneOffset.UTC);
        return switch (unit) {
            case DAY   -> zdt.plusDays(duration).toInstant();
            case WEEK  -> zdt.plusWeeks(duration).toInstant();
            case MONTH -> zdt.plusMonths(duration).toInstant();
            case YEAR  -> zdt.plusYears(duration).toInstant();
        };
    }

    public Instant nextRenewalDate(Instant currentEnd, Integer duration, PassDurationUnit unit) {
        // Renouvellement démarre là où la période courante se termine
        return periodEnd(currentEnd, duration, unit);
    }
}
```

---

### 5.3 PassCodeFactory

**Package :** `features.subscription.subscription.service.support.pass`

```java
@Component
@RequiredArgsConstructor
public class PassCodeFactory {

    private final SequenceGeneratorFacade sequenceGenerator;

    public String nextPassNumber(SubscriberType subscriberType, PassPlanVersion version) {
        return sequenceGenerator.next("pass_purchase");
    }
}
```

---

### 5.4 PassCreationOperator (refonte)

**Fichier :** `features/subscription/subscription/service/support/pass/PassCreationOperator.java`

Refonte complète — création depuis un plan :

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PassCreationOperator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PassRepository passRepository;
    private final PassEntitlementRepository passEntitlementRepository;
    private final PassTransactionRepository passTransactionRepository;
    private final PassPlanEntitlementRepository planEntitlementRepository;
    private final PassPlanVersionRepository passVersionRepository;
    private final PassCodeFactory codeFactory;
    private final PassPlanResolver planResolver;
    private final PassPeriodCalculator periodCalculator;
    private final PassBillingSupport billingSupport;
    private final PassLifecycleOperator lifecycleOperator;
    private final PassEventWriter eventWriter;
    private final PassEmailNotifier emailNotifier;
    private final SubscriptionOwnerResolver ownerResolver;
    private final BillingTaxRuleResolver taxRuleResolver;
    private final KycCaseRepository kycCaseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WalletService walletService;
    private final WalletHoldService walletHoldService;

    public Pass create(CreatePassPurchaseRequest request) {
        // 1. Résoudre le plan
        PassPlanVersion version = planResolver.resolveVersion(request.planCode(), request.planVersionId());
        PassPlanPrice price = planResolver.resolvePrice(version, request.currency());

        // 2. Valider le owner
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(
                request.ownerType(), request.ownerCode());
        validateKycLevel(version, owner);

        // 3. Calculer les montants
        PriceAmounts amounts = calculateAmounts(price);

        // 4. Calculer les dates de validité
        Instant validFrom = request.validFrom() != null
                ? parseInstant(request.validFrom())
                : Instant.now();
        Instant validUntil = periodCalculator.periodEnd(
                validFrom, version.getDuration(), version.getDurationUnit());

        // 5. Créer l'entité pass
        PassStatus initialStatus = amounts.total().signum() > 0
                ? PassStatus.PENDING_ACTIVATION
                : PassStatus.ACTIVE;

        Pass pass = passRepository.save(Pass.builder()
                .passNumber(codeFactory.nextPassNumber(request.ownerType(), version))
                .passType(version.getPlan().getPassType())
                .ownerType(request.ownerType())
                .ownerCode(owner.code())
                .member(owner.member())
                .customer(owner.customer())
                .businessEntity(owner.businessEntity())
                .passVersion(version)
                .status(initialStatus)
                .name(version.getName())
                .description(version.getDescription())
                .validFrom(validFrom)
                .validUntil(validUntil)
                .transferable(Boolean.FALSE)
                .shareable(Boolean.FALSE)
                .maxUses(version.getMaxUses())
                .autoRenew(Boolean.TRUE.equals(request.autoRenew())
                        && Boolean.TRUE.equals(version.getAutoRenewable()))
                .currency(price.getCurrency())
                .subtotalAmount(amounts.subtotal())
                .taxAmount(amounts.tax())
                .totalAmount(amounts.total())
                .metadataJson(trim(request.metadataJson()))
                .build());

        // 6. Copier les entitlements du plan vers l'instance
        copyPlanEntitlements(pass, version, validFrom, validUntil);

        // 7. Deposit hold (si applicable)
        createDepositHold(pass, price, owner);

        // 8. Billing (si payant)
        if (amounts.total().signum() > 0) {
            billingSupport.createAndInvoice(pass);
        } else {
            // Gratuit → activer immédiatement
            lifecycleOperator.activate(pass, "Auto-activation gratuite", "SYSTEM");
            return pass;
        }

        // 9. Événements & email
        eventWriter.writeHistory(pass, null, initialStatus, "Création", "SYSTEM");
        eventWriter.writeEvent(pass, PassEventType.PASS_CREATED, null);
        emailNotifier.notifyCreated(pass);

        // 10. Contrat PDF (async)
        passRepository.save(pass);
        eventPublisher.publishEvent(ContractGenerationEvent.forPass(pass.getId()));

        return pass;
    }

    private void copyPlanEntitlements(Pass pass, PassPlanVersion version,
                                       Instant validFrom, Instant validUntil) {
        List<PassPlanEntitlement> planEntitlements =
                planEntitlementRepository.findAllByPassVersion(version);
        planEntitlements.forEach(pe -> passEntitlementRepository.save(
                PassEntitlement.builder()
                        .pass(pass)
                        .entitlementDefinition(pe.getEntitlementDefinition())
                        .quantity(pe.getQuantity())
                        .unlimited(pe.getUnlimited())
                        .validFrom(validFrom)
                        .validUntil(pe.getValidForDays() != null
                                ? validFrom.plus(pe.getValidForDays(), ChronoUnit.DAYS)
                                : validUntil)
                        .build()
        ));
    }

    private PriceAmounts calculateAmounts(PassPlanPrice price) {
        BigDecimal base = nonNegative(price.getAmount());
        BigDecimal setup = nonNegative(price.getSetupFee());
        BigDecimal deposit = nonNegative(price.getDepositAmount());
        BigDecimal taxable = money(base.add(setup));
        BillingTaxRuleResolver.TaxProfile tax = taxRuleResolver.defaultTaxProfile();

        if (Boolean.TRUE.equals(price.getTaxIncluded())) {
            BigDecimal factor = taxFactor(tax.vatRate(), tax.additionalCentRate());
            BigDecimal subtotal = money(taxable.divide(factor, 8, RoundingMode.HALF_UP));
            BigDecimal taxAmt   = money(taxable.subtract(subtotal));
            return new PriceAmounts(subtotal, taxAmt, money(taxable.add(deposit)));
        }
        BigDecimal vatAmt  = percentage(taxable, tax.vatRate());
        BigDecimal centAmt = percentage(vatAmt, tax.additionalCentRate());
        BigDecimal taxAmt  = money(vatAmt.add(centAmt));
        return new PriceAmounts(taxable, taxAmt, money(taxable.add(taxAmt).add(deposit)));
    }

    private void createDepositHold(Pass pass, PassPlanPrice price,
                                    SubscriptionOwnerResolver.Owner owner) {
        if (price.getDepositAmount() == null || price.getDepositAmount().signum() <= 0) return;
        try {
            WalletResponse wallet = walletService.getOrCreate(
                    pass.getOwnerType().name(), owner.code(), price.getCurrency());
            walletHoldService.create(new CreateWalletHoldRequest(
                    wallet.walletNumber(), price.getDepositAmount(),
                    "PASS", pass.getPassNumber(), null, "SYSTEM"));
        } catch (Exception ex) {
            log.warn("Failed to place deposit hold for pass {}", pass.getPassNumber(), ex);
        }
    }

    private void validateKycLevel(PassPlanVersion version, SubscriptionOwnerResolver.Owner owner) {
        int required = version.getRequiredKycLevel() != null ? version.getRequiredKycLevel() : 1;
        if (required <= 1) return;
        // même logique que SubscriptionCreationOperator.validateRequiredKycLevel()
        // ... (code identique, non répété ici pour concision)
    }

    // ── Helpers mathématiques identiques à SubscriptionCreationOperator ──

    private BigDecimal taxFactor(BigDecimal vatRate, BigDecimal additionalCentRate) {
        BigDecimal vat  = rateFactor(vatRate);
        BigDecimal cent = rateFactor(additionalCentRate);
        return BigDecimal.ONE.add(vat).add(vat.multiply(cent));
    }

    private BigDecimal rateFactor(BigDecimal rate) {
        if (rate == null || rate.signum() == 0) return BigDecimal.ZERO;
        return rate.divide(HUNDRED, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.signum() == 0) return BigDecimal.ZERO;
        return money(amount.multiply(rate).divide(HUNDRED, 4, RoundingMode.HALF_UP));
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        if (v.signum() < 0) throw new BadRequestException("Pass price amounts cannot be negative");
        return v;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Instant parseInstant(String value) {
        // même logique que l'opérateur existant
        if (!StringUtils.hasText(value)) return null;
        try { return Instant.parse(value.trim()); } catch (Exception ignored) {}
        try { return OffsetDateTime.parse(value.trim()).toInstant(); } catch (Exception ignored) {}
        throw new BadRequestException("Invalid date-time format: " + value);
    }

    private record PriceAmounts(BigDecimal subtotal, BigDecimal tax, BigDecimal total) {}
}
```

---

### 5.5 PassBillingSupport

**Fichier :** `features/subscription/subscription/service/support/pass/PassBillingSupport.java`

```java
@Component
@RequiredArgsConstructor
public class PassBillingSupport {

    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BillableItemInvoiceSupport billableItemInvoiceSupport;
    private final PassRenewalScheduleRepository renewalScheduleRepository;

    public BillingDocumentResponse createAndInvoice(Pass pass) {
        return createAndInvoice(pass, "PASS_SETUP");
    }

    public BillingDocumentResponse createAndInvoice(Pass pass, String sourceType) {
        if (pass.getTotalAmount() == null || pass.getTotalAmount().signum() <= 0) return null;
        PassPlanVersion version = pass.getPassVersion();
        String description = "Émission du pass " + pass.getPassNumber()
                + " — " + pass.getName()
                + (version != null ? " (v" + version.getVersionNumber() + ")" : "");

        BillableItem item = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType(sourceType)
                .sourceId(pass.getPassNumber())
                .subscriberType(pass.getOwnerType())
                .subscriberCode(pass.getOwnerCode())
                .description(description)
                .amount(pass.getTotalAmount())
                .currency(pass.getCurrency())
                .billingPeriodStart(toLocalDate(pass.getValidFrom()))
                .billingPeriodEnd(toLocalDate(pass.getValidUntil()))
                .status(BillableItemStatus.PENDING)
                .build());

        return billableItemInvoiceSupport.ensureInvoiced(
                item,
                "Facture pass " + pass.getPassNumber(),
                description
        );
    }

    public void upsertRenewalSchedule(Pass pass) {
        PassPlanVersion version = pass.getPassVersion();
        if (version == null || !Boolean.TRUE.equals(pass.getAutoRenew())) return;

        PassRenewalSchedule schedule = renewalScheduleRepository.findByPassId(pass.getId())
                .orElseGet(() -> PassRenewalSchedule.builder().pass(pass).retryCount(0).build());
        schedule.setDuration(version.getDuration());
        schedule.setDurationUnit(version.getDurationUnit());
        schedule.setNextRenewalDate(pass.getNextRenewalDate());
        schedule.setCurrentPeriodStart(pass.getValidFrom());
        schedule.setCurrentPeriodEnd(pass.getValidUntil());
        schedule.setStatus(BillingScheduleStatus.ACTIVE);
        renewalScheduleRepository.save(schedule);
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }
}
```

---

### 5.6 PassLifecycleOperator (extensions)

**Nouvelles méthodes à ajouter au `PassLifecycleOperator` existant :**

```java
// ── Injection nouvelle ────────────────────────────────────────────
private final PassEventWriter eventWriter;
private final PassEmailNotifier emailNotifier;
private final PassBillingSupport billingSupport;
private final @Lazy EntitlementService entitlementService;

// ── Nouvelle méthode : activate() ────────────────────────────────

public Pass activate(Pass pass, String reason, String changedBy) {
    if (pass.getStatus() == PassStatus.ACTIVE) return pass;
    if (pass.getStatus() != PassStatus.PENDING_ACTIVATION) {
        throw new BadRequestException("Cannot activate pass " + pass.getPassNumber()
                + " in status " + pass.getStatus());
    }
    PassStatus prev = pass.getStatus();
    pass.setStatus(PassStatus.ACTIVE);
    passRepository.save(pass);

    entitlementService.grantForPass(pass);
    billingSupport.upsertRenewalSchedule(pass);

    eventWriter.writeHistory(pass, prev, PassStatus.ACTIVE, reason, changedBy);
    eventWriter.writeEvent(pass, PassEventType.PASS_ACTIVATED, null);
    eventWriter.writeEvent(pass, PassEventType.ENTITLEMENTS_GRANTED, null);
    emailNotifier.notifyActivated(pass);

    if (Boolean.TRUE.equals(pass.getAutoRenew())) {
        eventWriter.writeEvent(pass, PassEventType.RENEWAL_SCHEDULED, null);
    }

    passTransactionRepository.save(PassTransaction.builder()
            .pass(pass).transactionType("ACTIVATED")
            .referenceType("PAYMENT").referenceId(reason).build());
    return pass;
}

// ── Nouvelle méthode : markPastDue() ─────────────────────────────

public Pass markPastDue(Pass pass, String reason) {
    PassStatus prev = pass.getStatus();
    pass.setStatus(PassStatus.PAST_DUE);
    passRepository.save(pass);
    eventWriter.writeHistory(pass, prev, PassStatus.PAST_DUE, reason, "SYSTEM");
    eventWriter.writeEvent(pass, PassEventType.PASS_PAST_DUE, null);
    passTransactionRepository.save(PassTransaction.builder()
            .pass(pass).transactionType("PAST_DUE")
            .referenceType("RENEWAL").referenceId(reason).build());
    return pass;
}

// ── Modification cancel() — ajouter les champs ───────────────────

public Pass cancel(Pass pass, String reason) {
    pass.setStatus(PassStatus.CANCELLED);
    pass.setCancelledAt(Instant.now());
    pass.setCancellationReason(reason);
    passRepository.save(pass);
    // ... (reste de la logique existante) ...
    eventWriter.writeHistory(pass, PassStatus.ACTIVE, PassStatus.CANCELLED, reason, "SYSTEM");
    eventWriter.writeEvent(pass, PassEventType.PASS_CANCELLED, null);
    emailNotifier.notifyCancelled(pass, reason);
    passTransactionRepository.save(PassTransaction.builder()
            .pass(pass).transactionType("CANCELLED")
            .referenceType("PASS").referenceId(pass.getPassNumber())
            .payloadJson(StringUtils.hasText(reason) ? "{\"reason\":\"" + reason.replace("\"", "'") + "\"}" : null)
            .build());
    return pass;
}
```

---

### 5.7 PassRenewalOperator

**Fichier :** `features/subscription/subscription/service/support/pass/PassRenewalOperator.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PassRenewalOperator {

    private final PassRepository passRepository;
    private final PassRenewalScheduleRepository scheduleRepository;
    private final PassBillingSupport billingSupport;
    private final PassLifecycleOperator lifecycleOperator;
    private final PassEventWriter eventWriter;
    private final PassPeriodCalculator periodCalculator;
    private final ApplicationEventPublisher eventPublisher;

    public int renewDuePasses() {
        List<PassRenewalSchedule> due = scheduleRepository
                .findAllByStatusAndNextRenewalDateBefore(BillingScheduleStatus.ACTIVE, Instant.now());
        int count = 0;
        for (PassRenewalSchedule schedule : due) {
            try {
                renew(schedule);
                count++;
            } catch (Exception ex) {
                log.error("Failed to renew pass {} : {}", schedule.getPass().getPassNumber(), ex.getMessage());
            }
        }
        return count;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renew(PassRenewalSchedule schedule) {
        Pass pass = schedule.getPass();
        if (pass.getStatus() != PassStatus.ACTIVE) {
            log.info("Skipping renewal for pass {} in status {}", pass.getPassNumber(), pass.getStatus());
            return;
        }

        // 1. Calculer la nouvelle période
        Instant newFrom  = pass.getValidUntil();
        Instant newUntil = periodCalculator.periodEnd(newFrom, schedule.getDuration(), schedule.getDurationUnit());

        // 2. Mettre à jour le pass
        pass.setValidFrom(newFrom);
        pass.setValidUntil(newUntil);
        pass.setNextRenewalDate(newUntil);
        pass.setRenewalCount(pass.getRenewalCount() + 1);
        pass.setUsedCount(0);  // reset des usages pour la nouvelle période
        passRepository.save(pass);

        // 3. Mettre à jour le schedule
        schedule.setCurrentPeriodStart(newFrom);
        schedule.setCurrentPeriodEnd(newUntil);
        schedule.setNextRenewalDate(newUntil);
        schedule.setLastAttemptAt(Instant.now());
        schedule.setRetryCount(0);
        scheduleRepository.save(schedule);

        // 4. Facturation du renouvellement
        billingSupport.createAndInvoice(pass, "PASS_RENEWAL");

        // 5. Regrant entitlements pour la nouvelle période
        // (les anciens grants expirent avec l'ancienne validUntil)
        // → déclenchés après paiement via PaymentTransactionWorkflowProcessor

        // 6. Événement
        eventWriter.writeEvent(pass, PassEventType.PASS_RENEWED, null);
        eventWriter.writeEvent(pass, PassEventType.BILLING_SCHEDULED, null);
        log.info("Renewed pass {} — new period {} to {}", pass.getPassNumber(), newFrom, newUntil);
    }

    public void handleRenewalPaymentSucceeded(Pass pass, String transactionNumber) {
        // Regrant les entitlements pour la nouvelle période
        // (appelé depuis PaymentTransactionWorkflowProcessor)
        eventWriter.writeEvent(pass, PassEventType.ENTITLEMENTS_GRANTED, null);
        log.info("Renewal payment received for pass {} — transaction {}",
                pass.getPassNumber(), transactionNumber);
    }
}
```

---

### 5.8 PassEventWriter

**Fichier :** `features/subscription/subscription/service/support/pass/PassEventWriter.java`

```java
@Component
@RequiredArgsConstructor
public class PassEventWriter {

    private final PassEventRepository eventRepository;
    private final PassStatusHistoryRepository historyRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    public void writeEvent(Pass pass, PassEventType type, String payloadJson) {
        eventRepository.save(PassEvent.builder()
                .pass(pass)
                .eventType(type.name())
                .payloadJson(payloadJson)
                .build());
    }

    public void writeHistory(Pass pass, PassStatus from, PassStatus to,
                              String reason, String changedBy) {
        historyRepository.save(PassStatusHistory.builder()
                .pass(pass)
                .fromStatus(from == null ? null : from.name())
                .toStatus(to.name())
                .reason(reason)
                .changedBy(changedBy)
                .build());
    }
}
```

---

### 5.9 PassEmailNotifier

**Fichier :** `features/subscription/subscription/service/support/pass/PassEmailNotifier.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PassEmailNotifier {

    private final EmailService emailService;
    private final SubscriptionOwnerResolver ownerResolver;

    @Async
    public void notifyCreated(Pass pass) { send(pass, "pass-created",   "Votre pass a été créé — "); }

    @Async
    public void notifyActivated(Pass pass) { send(pass, "pass-activated", "Votre pass est actif — "); }

    @Async
    public void notifyRenewed(Pass pass) { send(pass, "pass-renewed",   "Votre pass a été renouvelé — "); }

    @Async
    public void notifyCancelled(Pass pass, String reason) {
        Map<String, Object> ctx = buildContext(pass);
        ctx.put("reason", reason);
        sendWithContext(pass, "pass-cancelled", "Votre pass a été annulé — ", ctx);
    }

    @Async
    public void notifyPastDue(Pass pass) { send(pass, "pass-past-due",  "Échec renouvellement pass — "); }

    private void send(Pass pass, String template, String subjectPrefix) {
        sendWithContext(pass, template, subjectPrefix, buildContext(pass));
    }

    private void sendWithContext(Pass pass, String template, String subjectPrefix,
                                  Map<String, Object> context) {
        String email = resolveEmail(pass);
        if (!StringUtils.hasText(email)) return;
        try {
            emailService.sendHtml(email, subjectPrefix + pass.getName(), template, context);
        } catch (Exception ex) {
            log.warn("Failed to send {} email for pass {}", template, pass.getPassNumber(), ex);
        }
    }

    private String resolveEmail(Pass pass) {
        try {
            var owner = ownerResolver.resolve(pass.getOwnerType(), pass.getOwnerCode());
            if (owner.member()         != null) return owner.member().getEmail();
            if (owner.customer()       != null) return owner.customer().getEmail();
            if (owner.businessEntity() != null) return owner.businessEntity().getEmail();
        } catch (Exception ignored) {}
        return null;
    }

    private Map<String, Object> buildContext(Pass pass) {
        Map<String, Object> ctx = new java.util.LinkedHashMap<>();
        ctx.put("passNumber",    pass.getPassNumber());
        ctx.put("passName",      pass.getName());
        ctx.put("passType",      pass.getPassType() == null ? "" : pass.getPassType().name());
        ctx.put("validFrom",     pass.getValidFrom());
        ctx.put("validUntil",    pass.getValidUntil());
        ctx.put("status",        pass.getStatus().name());
        ctx.put("currency",      pass.getCurrency() == null ? "" : pass.getCurrency());
        ctx.put("totalAmount",   pass.getTotalAmount());
        ctx.put("autoRenew",     Boolean.TRUE.equals(pass.getAutoRenew()));
        ctx.put("renewalCount",  pass.getRenewalCount());
        return ctx;
    }
}
```

---

## 6. Workers

### PassRenewalWorker.java (nouveau)

**Package :** `features.subscription.subscription.worker`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PassRenewalWorker {

    private final PassRenewalOperator renewalOperator;

    @Scheduled(fixedDelayString = "${bokati.pass.workers.renewal-delay-ms:900000}")
    public void renewDuePasses() {
        int renewed = renewalOperator.renewDuePasses();
        if (renewed > 0) {
            log.info("Renewed {} due passes", renewed);
        }
    }
}
```

### PassExpiryWorker.java (modifier)

Le worker existant appelle `passService.expirePasses()`. Modifier `PassLifecycleOperator.expirePasses()` pour inclure les statuts `PENDING_ACTIVATION` et `PAST_DUE` en plus de `ACTIVE` :

```java
public int expirePasses() {
    Instant now = Instant.now();
    List<String> expirableStatuses = List.of(
            PassStatus.ACTIVE.name(),
            PassStatus.PENDING_ACTIVATION.name(),
            PassStatus.PAST_DUE.name()
    );
    List<Pass> toExpire = passRepository.findAllByStatusInAndValidUntilBefore(expirableStatuses, now);
    toExpire.forEach(pass -> {
        pass.setStatus(PassStatus.EXPIRED);
        passRepository.save(pass);
        eventWriter.writeHistory(pass, pass.getStatus(), PassStatus.EXPIRED, "Expiration automatique", "WORKER");
        eventWriter.writeEvent(pass, PassEventType.PASS_EXPIRED, null);
        passTransactionRepository.save(PassTransaction.builder()
                .pass(pass).transactionType("EXPIRED")
                .referenceType("WORKER").referenceId("PASS_EXPIRY").build());
    });
    return toExpire.size();
}
```

Mettre à jour `PassRepository` :

```java
List<Pass> findAllByStatusInAndValidUntilBefore(List<String> statuses, Instant now);
```

---

## 7. Intégration PaymentTransactionWorkflowProcessor

**Fichier :** `features/payment/service/support/PaymentTransactionWorkflowProcessor.java`

### Modifier `handleSucceededTransaction()`

```java
private void handleSucceededTransaction(PaymentTransaction transaction,
                                         TransactionContextResolver.SourceView source) {
    String sourceType = source.type().trim().toUpperCase(Locale.ROOT);
    switch (sourceType) {
        case "SUBSCRIPTION" -> handleSubscriptionPaymentSucceeded(transaction, source.code());
        case "PASS"         -> handlePassPaymentSucceeded(transaction, source.code());
        case "PASS_RENEWAL" -> handlePassRenewalPaymentSucceeded(transaction, source.code());
        default -> log.debug("No activation workflow for source type {}", source.type());
    }
}
```

### Nouvelles méthodes

```java
private void handlePassPaymentSucceeded(PaymentTransaction transaction, String passNumber) {
    Pass pass = passService.getForService(passNumber);
    if (pass.getStatus() != PassStatus.PENDING_ACTIVATION) {
        log.debug("Pass {} already in status {}, skipping activation", passNumber, pass.getStatus());
        return;
    }
    passService.activate(passNumber,
            "Activation après paiement " + transaction.getTransactionNumber());
}

private void handlePassRenewalPaymentSucceeded(PaymentTransaction transaction, String passNumber) {
    Pass pass = passService.getForService(passNumber);
    // Redonner les entitlements pour la nouvelle période
    passRenewalOperator.handleRenewalPaymentSucceeded(pass, transaction.getTransactionNumber());
}
```

**Injecter `PassRenewalOperator` et `PassService`** dans le processor (les deux existent déjà ou seront créés).

---

## 8. Controllers & DTOs

### 8.1 PassPlanController

**Route :** `/api/v1/pass-plans`  
**Package :** `features.subscription.subscription.controller`

```
POST   /api/v1/pass-plans                        → créer un plan
GET    /api/v1/pass-plans                        → lister/rechercher
GET    /api/v1/pass-plans/{planCode}             → détail
PATCH  /api/v1/pass-plans/{planCode}/publish     → publier
PATCH  /api/v1/pass-plans/{planCode}/archive     → archiver

POST   /api/v1/pass-plans/{planCode}/versions               → créer version
GET    /api/v1/pass-plans/{planCode}/versions               → lister versions
PATCH  /api/v1/pass-plans/{planCode}/versions/{id}/publish  → publier version

POST   /api/v1/pass-plans/versions/{versionId}/prices       → définir prix
GET    /api/v1/pass-plans/versions/{versionId}/prices       → lister prix

POST   /api/v1/pass-plans/versions/{versionId}/entitlements → ajouter entitlement
DELETE /api/v1/pass-plans/versions/{versionId}/entitlements/{id} → supprimer
GET    /api/v1/pass-plans/versions/{versionId}/entitlements → lister
```

Permissions : `PASS_PLAN:WRITE`, `PASS_PLAN:READ`

---

### 8.2 PassController (update)

**Fichier :** `features/subscription/subscription/controller/PassController.java`

**Nouvel endpoint principal** (remplace le `POST /api/v1/passes` actuel) :

```
POST   /api/v1/pass-plans/{planCode}/purchase   → acheter un pass depuis un plan
```

Garder l'ancien `POST /api/v1/passes` en parallèle avec dépréciation (backward compat pour les passes ad-hoc existants).

**Nouveaux endpoints :**

```
POST   /api/v1/passes/{passNumber}/activate     → activation manuelle (admin)
GET    /api/v1/passes/{passNumber}/events       → historique des événements
GET    /api/v1/passes/{passNumber}/history      → historique des statuts
GET    /api/v1/passes/{passNumber}/renewal      → schedule de renouvellement
```

**Mettre à jour :**

```java
@PostMapping("/{planCode}/purchase")
@PreAuthorize("hasAnyAuthority('PASS:WRITE','PASS_WRITE')")
public ResponseEntity<PassResponse> purchase(
        @PathVariable String planCode,
        @Valid @RequestBody CreatePassPurchaseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(passService.purchase(planCode, request));
}

@PostMapping("/{passNumber}/activate")
@PreAuthorize("hasAnyAuthority('PASS:ADMIN','PASS_ADMIN')")
public ResponseEntity<PassResponse> activate(@PathVariable String passNumber) {
    return ResponseEntity.ok(passService.activate(passNumber, "Activation manuelle"));
}
```

---

### 8.3 DTOs

**Nouveau :** `CreatePassPurchaseRequest`

```java
public record CreatePassPurchaseRequest(
        // Qui achète ?
        @NotNull SubscriberType ownerType,
        @NotBlank String ownerCode,

        // Quelle version ? (null = dernière version active)
        Long planVersionId,

        // Devise (null = XAF par défaut)
        String currency,

        // Début de validité (null = maintenant)
        String validFrom,

        // Renouvellement auto (ignoré si passVersion.autoRenewable = false)
        Boolean autoRenew,

        // Métadonnées optionnelles
        String metadataJson
) {}
```

**Mise à jour de `PassResponse`** :

```java
public record PassResponse(
        String passNumber,
        PassType passType,
        SubscriberType ownerType,
        String ownerCode,
        String subscriptionNumber,
        PassStatus status,
        String name,
        String description,
        Instant validFrom,
        Instant validUntil,
        Boolean transferable,
        Boolean shareable,
        Integer maxUses,
        Integer usedCount,
        String contractCode,
        // ── Plan ───────────────────────────────────────────
        String planCode,
        String planName,
        Integer planVersion,
        // ── Billing ────────────────────────────────────────
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String invoiceNumber,
        String paymentIntentNumber,
        BigDecimal balanceDue,
        // ── Renouvellement ─────────────────────────────────
        Boolean autoRenew,
        Instant nextRenewalDate,
        Integer renewalCount,
        // ── Entitlements ───────────────────────────────────
        List<EntitlementGrantResponse> entitlements,
        // ── Audit ──────────────────────────────────────────
        Instant createdAt,
        Instant updatedAt
) {}
```

**Mise à jour de `PassService` interface :**

```java
public interface PassService {
    PassResponse purchase(String planCode, CreatePassPurchaseRequest request);  // nouveau
    PassResponse create(CreatePassRequest request);   // ancien (deprecated)
    PassResponse get(String passNumber);
    Pass getForService(String passNumber);
    PaginatedResponse<PassResponse> list(PassSearchCriteria criteria, Pageable pageable);
    PassResponse activate(String passNumber, String reason);                     // nouveau
    PassResponse cancel(String passNumber, String reason);
    int expirePasses();
    int renewDuePasses();                                                         // nouveau
}
```

---

## 9. Partie 2 — Module Commercial (extension CRM)

### Vision

Le module CRM existant (`features/crm/`) gère déjà Lead, Opportunity, Kanban, Analytics, et la génération de devis simples via `generateQuote()`. Ce module reste intact.

On ajoute une **proposition commerciale structurée** (`CommercialProposal`) avec lignes de service détaillées, cycle de vie complet (DRAFT → SENT → ACCEPTED → CONVERTED), génération PDF, et conversion automatique en facture / souscription / pass.

### V161 — crm_commercial_proposal

```sql
CREATE TABLE IF NOT EXISTS crm_commercial_proposal (
    id                          BIGSERIAL       PRIMARY KEY,
    proposal_number             VARCHAR(80)     NOT NULL UNIQUE,
    opportunity_id              BIGINT          REFERENCES crm_opportunity(id),
    lead_id                     BIGINT          REFERENCES crm_lead(id),
    title                       VARCHAR(300)    NOT NULL,
    recipient_type              VARCHAR(60),
    recipient_code              VARCHAR(120),
    recipient_name              VARCHAR(200),
    recipient_email             VARCHAR(200),
    status                      VARCHAR(40)     NOT NULL DEFAULT 'DRAFT',
    valid_until                 DATE,
    subtotal_amount             NUMERIC(19,4)   NOT NULL DEFAULT 0,
    tax_amount                  NUMERIC(19,4)   NOT NULL DEFAULT 0,
    total_amount                NUMERIC(19,4)   NOT NULL DEFAULT 0,
    currency                    VARCHAR(10),
    notes                       TEXT,
    internal_notes              TEXT,
    converted_invoice_number    VARCHAR(120),
    converted_contract_code     VARCHAR(120),
    converted_subscription_number VARCHAR(120),
    converted_pass_number       VARCHAR(120),
    sent_at                     TIMESTAMPTZ,
    viewed_at                   TIMESTAMPTZ,
    accepted_at                 TIMESTAMPTZ,
    rejected_at                 TIMESTAMPTZ,
    rejection_reason            TEXT,
    document_code               VARCHAR(120),
    created_by                  BIGINT,
    assigned_to                 BIGINT,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_crm_proposal_status CHECK (status IN (
        'DRAFT','SENT','VIEWED','ACCEPTED','REJECTED','EXPIRED','CONVERTED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_crm_proposal_number      ON crm_commercial_proposal(proposal_number);
CREATE INDEX IF NOT EXISTS idx_crm_proposal_opportunity ON crm_commercial_proposal(opportunity_id);
CREATE INDEX IF NOT EXISTS idx_crm_proposal_lead        ON crm_commercial_proposal(lead_id);
CREATE INDEX IF NOT EXISTS idx_crm_proposal_status      ON crm_commercial_proposal(status);
```

### V162 — crm_proposal_line + séquences

```sql
CREATE TABLE IF NOT EXISTS crm_proposal_line (
    id                  BIGSERIAL       PRIMARY KEY,
    proposal_id         BIGINT          NOT NULL REFERENCES crm_commercial_proposal(id) ON DELETE CASCADE,
    sort_order          INTEGER         NOT NULL DEFAULT 0,
    service_type        VARCHAR(60),
    service_ref_code    VARCHAR(120),
    label               VARCHAR(300)    NOT NULL,
    description         TEXT,
    quantity            NUMERIC(10,2)   NOT NULL DEFAULT 1,
    unit_price          NUMERIC(19,4)   NOT NULL DEFAULT 0,
    discount_percent    NUMERIC(5,2)    NOT NULL DEFAULT 0,
    tax_rate            NUMERIC(5,4)    NOT NULL DEFAULT 0,
    tax_included        BOOLEAN         NOT NULL DEFAULT FALSE,
    subtotal_amount     NUMERIC(19,4)   NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(19,4)   NOT NULL DEFAULT 0,
    total_amount        NUMERIC(19,4)   NOT NULL DEFAULT 0,
    currency            VARCHAR(10)
);

CREATE INDEX IF NOT EXISTS idx_crm_proposal_line_proposal ON crm_proposal_line(proposal_id);

INSERT INTO sequence_definition
    (sequence_code, prefix, suffix, padding_length, increment_step, reset_strategy, last_value, created_at, updated_at)
VALUES
    ('crm_proposal', 'PROP-', NULL, 5, 1, 'YEARLY', 0, NOW(), NOW())
ON CONFLICT (sequence_code) DO NOTHING;
```

### Entités, DTOs, Service, Controller

Voir spécification détaillée de la Partie 2 dans les sections du document précédent (structure identique, réutiliser `ProposalDtos`, `ProposalService`, `ProposalServiceImpl`, `ProposalController`).

**Conversions clés :**
- `convertToInvoice()` → `BillingDocumentService.convertQuoteToInvoice()`
- `convertToSubscription()` → `SubscriptionService.create()`
- `convertToPass()` → `PassService.purchase()` (nouveau endpoint)

---

## 10. Ordre d'exécution

### Phase A — Données (migrations)

```
① V155 — pass_plan + pass_plan_version
② V156 — pass_plan_price + pass_plan_entitlement
③ V157 — ALTER subscription_pass (nouveaux champs + statuts)
④ V158 — pass_status_history + pass_event
⑤ V159 — pass_renewal_schedule
⑥ V160 — séquences pass
⑦ V161 — crm_commercial_proposal
⑧ V162 — crm_proposal_line + séquence
```

### Phase B — Modèles Java

```
⑨  PassDurationUnit.java                    (enum)
⑩  PassEventType.java                       (enum)
⑪  PassStatus.java                          (add PENDING_ACTIVATION, PAST_DUE)
⑫  PassPlan.java                            (entité)
⑬  PassPlanVersion.java                     (entité)
⑭  PassPlanPrice.java                       (entité)
⑮  PassPlanEntitlement.java                 (entité)
⑯  PassEvent.java                           (entité)
⑰  PassStatusHistory.java                   (entité)
⑱  PassRenewalSchedule.java                 (entité)
⑲  Pass.java                                (add passVersion, billing, autoRenew, renewal fields)
⑳  Tous les nouveaux repositories (x8)
```

### Phase C — Services

```
㉑  PassEventWriter.java
㉒  PassPlanResolver.java
㉓  PassPeriodCalculator.java
㉔  PassCodeFactory.java
㉕  PassBillingSupport.java
㉖  PassEmailNotifier.java
㉗  PassCreationOperator.java               (refonte)
㉘  PassLifecycleOperator.java              (add activate, markPastDue, update cancel, update expirePasses)
㉙  PassRenewalOperator.java
㉚  PassPlanServiceImpl.java + interface
㉛  PassServiceImpl.java                    (add purchase, activate, renewDuePasses)
㉜  PaymentTransactionWorkflowProcessor.java (add PASS + PASS_RENEWAL cases)
```

### Phase D — Workers & API

```
㉝  PassRenewalWorker.java
㉞  PassExpiryWorker.java                   (appel inchangé, logique dans l'opérateur)
㉟  CreatePassPurchaseRequest.java
㊱  PassResponse.java                       (update)
㊲  PassPlanController.java
㊳  PassController.java                     (add purchase, activate endpoints)
㊴  SubscriptionPassMapper.java             (update)
㊵  Templates email : pass-created, pass-activated, pass-renewed, pass-cancelled, pass-past-due
```

### Phase E — CRM / Commercial

```
㊶  ProposalStatus.java
㊷  CommercialProposal.java
㊷  ProposalLineItem.java
㊸  ProposalRepository.java + ProposalLineRepository.java
㊹  ProposalDtos.java
㊺  ProposalService.java + ProposalServiceImpl.java
㊻  ProposalController.java
㊼  Extension CrmEmailService → sendProposalEmail()
```

---

## 11. Stratégie de test

### Pass Plan (nouveaux tests)

| Scénario | Classe | Méthode |
|---|---|---|
| Acheter un pass payant depuis un plan → PENDING_ACTIVATION | `PassCreationOperatorTest` | `givenActivePlan_shouldCreatePendingActivationPass` |
| Acheter un pass gratuit → ACTIVE immédiat | `PassCreationOperatorTest` | `givenFreePlan_shouldActivateAndGrantEntitlements` |
| BillableItem + Invoice créés si total > 0 | `PassBillingSupportTest` | `shouldCreateBillableItemAndIssueInvoice` |
| Tax included → extraction HT correcte | `PassCreationOperatorTest` | `givenTaxIncludedPrice_shouldExtractSubtotal` |
| Tax excluded → ajout TVA correct | `PassCreationOperatorTest` | `givenNetPrice_shouldAddVatCorrectly` |
| Activation après paiement PawaPay | `PaymentTransactionWorkflowProcessorTest` | `givenPassPaymentSucceeded_shouldActivatePass` |
| Entitlements copiés du plan vers l'instance | `PassCreationOperatorTest` | `shouldCopyPlanEntitlementsToPassInstance` |
| Calcul dates validité (DAY, MONTH, YEAR) | `PassPeriodCalculatorTest` | `shouldCalculatePeriodEndForAllDurationUnits` |
| autoRenew → PassRenewalSchedule créé | `PassBillingSupportTest` | `givenAutoRenewPass_shouldCreateRenewalSchedule` |
| Worker renouvelle les passes dus | `PassRenewalOperatorTest` | `shouldRenewAllDuePasses` |
| Renouvellement → nouvelle période + reset usedCount | `PassRenewalOperatorTest` | `shouldExtendValidityAndResetUsageOnRenewal` |
| Expiration inclut PENDING_ACTIVATION et PAST_DUE | `PassLifecycleOperatorTest` | `shouldExpirePassesInAllExpirableStatuses` |
| KYC requis → rejet si niveau insuffisant | `PassCreationOperatorTest` | `givenInsufficientKyc_shouldRejectPurchase` |

### Commercial / CRM (nouveaux tests)

| Scénario | Classe | Méthode |
|---|---|---|
| Créer proposition avec lignes | `ProposalServiceImplTest` | `shouldCreateProposalAndCalculateTotals` |
| Calcul par ligne (TVA + remise) | `ProposalServiceImplTest` | `shouldApplyTaxAndDiscountPerLine` |
| Envoyer proposition → SENT + opportunité PROPOSAL_SENT | `ProposalServiceImplTest` | `shouldSendProposalAndUpdateOpportunityStage` |
| Accepter → ACCEPTED | `ProposalServiceImplTest` | `shouldAcceptSentProposal` |
| Convertir en pass → CONVERTED + passNumber | `ProposalServiceImplTest` | `shouldConvertAcceptedProposalToPass` |
| Conversion refusée si non-ACCEPTED | `ProposalServiceImplTest` | `givenDraftProposal_shouldRejectConversion` |
| Pipeline metrics corrects | `ProposalServiceImplTest` | `shouldReturnCorrectPipelineStats` |
