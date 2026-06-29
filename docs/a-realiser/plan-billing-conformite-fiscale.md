# Plan d'implémentation — Mise en conformité fiscale du module Billing

**Référence spec :** Spécifications Techniques – Mise en Conformité du Module Devis & Facturation  
**Objectif :** Préparer le module billing à une certification DGID en garantissant l'inaltérabilité,
la traçabilité, la continuité des numéros, la sécurité cryptographique et la conformité des calculs fiscaux.  
**Date de rédaction :** 2026-06-17  
**Dernière migration en prod :** V154

---

## 1. Audit de l'existant

### Ce qui existe déjà ✅

| Élément | Fichier(s) |
|---------|-----------|
| Types de documents (QUOTE, INVOICE, CREDIT_NOTE, DEBIT_NOTE, PROFORMA_INVOICE) | `BillingDocumentType` |
| Cycle de vie étendu (DRAFT → ISSUED → SENT → ACCEPTED → PAID…) | `BillingDocumentStatus` |
| Calcul TVA + centime additionnel côté backend | `BillingCalculationService` |
| Règles de taxe dynamiques | `TaxRule`, `BillingTaxRuleResolver` |
| Génération PDF (OpenHtmlToPDF) | `BillingDocumentPdfServiceImpl` |
| Historique des modifications | `BillingDocumentEditHistory` |
| Avoirs, acomptes, notes de débit | `CreateCreditNoteRequest`, `BillingDocumentAdvance` |
| Numérotation sans doublons (séquenceur) | `BillingNumberingSupport`, `SequenceGeneratorFacade` |
| Snapshot client (nom, email, adresse) | `BillingCustomerSnapshotResolver` |
| ZXing QR code (déjà dans le classpath) | `pom.xml` (zxing 3.5.3) |

### Ce qui manque ou est insuffisant ❌

| # | Gap | Impact |
|---|-----|--------|
| 1 | Pas de statut `VALIDATED` ni de **verrouillage** — une facture émise reste modifiable | **Critique** |
| 2 | Format de numéro non conforme — actuel `INV-MEM-20260617-000042`, requis `FAC-2026-000001` | **Critique** |
| 3 | Pas de **snapshot vendeur** (nom commercial, NIU, adresse, téléphone, email) | **Critique** |
| 4 | Pas de **NIU client** stocké sur le document | **Critique** |
| 5 | Pas de **date fiscale** (`fiscal_date`) ni d'horodatage de validation (`validated_at`) | **Critique** |
| 6 | Pas de **journal d'audit fiscal** dédié (IP, user-agent, ancienne/nouvelle valeur) | **Critique** |
| 7 | Pas de **suppression logique** (`deleted_at`, `deleted_by`, `delete_reason`) | **Critique** |
| 8 | Pas de **chaînage cryptographique** entre factures (`previous_hash`, `current_hash`) | **Sécurité** |
| 9 | Pas de **signature HMAC** (`fiscal_signature`, `signed_at`) | **Sécurité** |
| 10 | Pas de **QR code fiscal** sur le PDF | **Sécurité** |
| 11 | Pas de **service de contrôle d'intégrité** | **Conformité** |
| 12 | PDF sans mention "Facture validée – Document non modifiable" | **Conformité** |

---

## 2. Architecture cible

```
billing/
├── model/
│   ├── BillingDocument.java          ← +12 champs fiscaux (voir §3)
│   ├── DocumentSequence.java         ← NOUVEAU — séquence par type+année
│   └── FiscalAuditLog.java           ← NOUVEAU — journal d'audit fiscal
│
├── enums/
│   └── BillingDocumentStatus.java    ← +VALIDATED
│
├── service/
│   ├── support/
│   │   ├── BillingLifecycleSupport.java     ← +ensureNotLocked()
│   │   ├── BillingCalculationService.java   ← inchangé (déjà correct)
│   │   └── BillingCustomerSnapshotResolver  ← +snapshot vendeur
│   └── fiscal/                              ← NOUVEAU package
│       ├── DocumentSequenceService.java
│       ├── FiscalHashService.java
│       ├── FiscalSignatureService.java
│       ├── FiscalAuditService.java
│       ├── FiscalQrCodeService.java
│       └── FiscalIntegrityCheckService.java
│
└── controller/
    └── FiscalIntegrityController.java       ← NOUVEAU (admin only)
```

---

## 3. Phase 1 — Fondation fiscale (Critique)

### 3.1 Statut VALIDATED + verrouillage des factures

**Principe :** Une facture validée devient immuable. Toute tentative de modification
doit être rejetée et tracée.

#### `BillingDocumentStatus.java`

Ajouter `VALIDATED` entre `ISSUED` et `SENT` :

```java
public enum BillingDocumentStatus {
    DRAFT,
    ISSUED,
    VALIDATED,   // ← NOUVEAU : facture fiscalement scellée
    SENT,
    VIEWED,
    NEGOTIATION,
    ACCEPTED,
    DEPOSIT_REQUESTED,
    DEPOSIT_PAID,
    REJECTED,
    EXPIRED,
    CONVERTED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED,
    VOIDED,
    REFUNDED,
    WRITTEN_OFF
}
```

**Règle de cycle de vie :**
- `QUOTE` : `DRAFT → SENT → ACCEPTED → CONVERTED` (devis non fiscal, modifiable jusqu'à conversion)
- `INVOICE` : `DRAFT → VALIDATED` — après validation, aucun champ n'est modifiable
- `CREDIT_NOTE` / `DEBIT_NOTE` : `DRAFT → VALIDATED`

#### `BillingLifecycleSupport.java`

Ajouter :

```java
public void ensureNotLocked(BillingDocument document) {
    if (Boolean.TRUE.equals(document.getLocked())) {
        throw new BadRequestException(
            "Facture " + document.getDocumentNumber() + " validée — document non modifiable"
        );
    }
}

public void ensureCanValidate(BillingDocument document) {
    if (document.getDocumentType() != BillingDocumentType.INVOICE
            && document.getDocumentType() != BillingDocumentType.CREDIT_NOTE
            && document.getDocumentType() != BillingDocumentType.DEBIT_NOTE) {
        throw new BadRequestException("Seules les factures, avoirs et notes de débit peuvent être validés");
    }
    if (document.getStatus() != BillingDocumentStatus.DRAFT
            && document.getStatus() != BillingDocumentStatus.ISSUED) {
        throw new BadRequestException("Le document doit être en statut DRAFT ou ISSUED pour être validé");
    }
}
```

Appeler `ensureNotLocked(doc)` en tête de chaque méthode mutante dans `BillingDocumentServiceImpl`
(update lignes, remises, taxes, client, conditions de paiement).

#### Migration V155

```sql
-- V155__billing_document_validation_lock.sql
ALTER TABLE billing_document
    ADD COLUMN locked        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN validated_at  TIMESTAMPTZ,
    ADD COLUMN fiscal_date   DATE;

CREATE INDEX idx_billing_document_locked ON billing_document (document_type, locked, validated_at);
```

---

### 3.2 Séquence fiscale compatible SEFC — numérotation continue annuelle

**Référence SEFC :** le système d'encaissement et de facturation certifié de la DGID
produit des numéros de la forme `FAC20260227-47qqc-CBFOS`. Notre format s'aligne
exactement sur cette structure.

**Décomposition du format SEFC de référence :**

```
FAC  20260227  -  47  qqc  -  CBFOS
 │      │          │   │        │
 │      │          │   │        └─ Code machine/caisse (5 chars, enregistré DGID)
 │      │          │   └─ Nonce anti-contrefaçon (3 chars alphanumériques bas de casse)
 │      │          └─ Compteur séquentiel annuel (numérotation continue)
 │      └─ Date d'émission YYYYMMDD
 └─ Type de document
```

**Format cible :** `{TYPE}{YYYYMMDD}-{NNNNN}{nnn}-{MACHINE}`

| Segment | Longueur | Exemple | Règle |
|---------|---------|---------|-------|
| `TYPE` | 3 | `FAC` | Préfixe type de document |
| `YYYYMMDD` | 8 | `20260617` | Date fiscale d'émission, collée au type (pas de tiret) |
| `NNNNN` | 5 min | `00047` | Compteur annuel par type — **ne repart jamais à 1 en cours d'année** |
| `nnn` | 3 | `qqc` | Nonce SHA-256 déterministe bas de casse (base36) |
| `MACHINE` | 5 | `CBFOS` | Code caisse/terminal configuré (`app.company.machine-code`) |

**Exemples concrets :**

```
FAC20260617-00001abc-BKTWK   ← 1ère facture de l'année 2026, émise le 17 juin
FAC20260617-00002def-BKTWK   ← 2ème facture, même jour
FAC20260618-00003ghi-BKTWK   ← 3ème facture, lendemain (compteur continue de 3, pas de reset)
FAC20270101-00001jkl-BKTWK   ← 1ère facture 2027 (reset uniquement en changement d'année)
DEV20260617-00001mno-BKTWK   ← 1er devis 2026 (séquence indépendante par type)
AVR20260617-00001pqr-BKTWK   ← 1er avoir 2026
```

**Propriété de numérotation continue :** la séquence `NNNNN` ne contient aucun saut
sur toute l'année fiscale, quel que soit le mois. La DGID peut vérifier l'absence
de trous en comparant les compteurs sans tenir compte des dates individuelles.

#### Configuration `application.yml`

```yaml
app:
  company:
    name: "Elle a Osé"
    legal-name: "SAS Elle a Osé"
    niu: "${COMPANY_NIU:}"
    fiscal-id: "${COMPANY_FISCAL_ID:000000}"   # Optionnel (NIU non obligatoire)
    machine-code: "${COMPANY_MACHINE_CODE:BKTWK}"  # Code caisse enregistré DGID (5 chars)
    address: "Rue 1234, Brazzaville, Congo"
    phone: "+242 06 XXX XX XX"
    email: "contact@elleaose.com"
```

> `machine-code` est le code attribué par la DGID lors de l'enregistrement du système
> de facturation. Par défaut `BKTWK` (à remplacer par le code officiel reçu).

#### Migration V156

```sql
-- V156__document_sequence_table.sql
CREATE TABLE document_sequence (
    id             BIGINT       PRIMARY KEY,
    document_type  VARCHAR(40)  NOT NULL,
    year           INT          NOT NULL,
    prefix         VARCHAR(10)  NOT NULL,
    current_value  BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- Clé unique par type + année fiscale (numérotation continue sur l'année entière)
    CONSTRAINT uq_document_sequence_type_year UNIQUE (document_type, year)
);

-- Amorçage pour l'année courante
INSERT INTO document_sequence (id, document_type, year, prefix, current_value)
VALUES
    (1, 'QUOTE',            2026, 'DEV', 0),
    (2, 'INVOICE',          2026, 'FAC', 0),
    (3, 'CREDIT_NOTE',      2026, 'AVR', 0),
    (4, 'DEBIT_NOTE',       2026, 'DBN', 0),
    (5, 'PROFORMA_INVOICE', 2026, 'PRF', 0);
```

#### `DocumentSequence.java` (entité)

```java
@Entity
@Table(name = "document_sequence")
public class DocumentSequence {
    @Id
    @IdGeneration
    private Long id;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

#### `DocumentSequenceService.java` (nouveau)

```java
@Service
@RequiredArgsConstructor
public class DocumentSequenceService {

    private final DocumentSequenceRepository sequenceRepository;

    /**
     * Code machine enregistré auprès de la DGID (5 chars).
     * Fallback "00000" si non configuré.
     */
    @Value("${app.company.machine-code:00000}")
    private String machineCode;

    /**
     * Génère le prochain numéro fiscal compatible SEFC.
     * SELECT FOR UPDATE sérialise les appels concurrents pour un même type+année.
     *
     * Format : FAC20260617-00001abc-BKTWK
     *           │          │    │    │
     *           type+date  seq  nonce machine
     */
    @Transactional
    public String nextNumber(BillingDocumentType type, LocalDate fiscalDate) {
        int year = fiscalDate.getYear();
        DocumentSequence seq = sequenceRepository
                .findLockedByTypeAndYear(type.name(), year)
                .orElseGet(() -> createSequence(type, year));

        long next = seq.getCurrentValue() + 1;
        seq.setCurrentValue(next);
        seq.setUpdatedAt(Instant.now());
        sequenceRepository.save(seq);

        String nonce = computeNonce(type, fiscalDate, next, machineCode);
        String machine = (machineCode != null && !machineCode.isBlank())
                ? machineCode.toUpperCase()
                : "00000";

        // FAC20260617-00001abc-BKTWK
        return String.format("%s%s-%05d%s-%s",
                seq.getPrefix(),
                fiscalDate.format(DateTimeFormatter.BASIC_ISO_DATE),  // YYYYMMDD
                next,
                nonce,
                machine);
    }

    /**
     * Nonce déterministe anti-contrefaçon : 3 chars base36 dérivés de SHA-256.
     * Reproductible à partir des mêmes paramètres — vérifiable sans état externe.
     */
    private String computeNonce(BillingDocumentType type, LocalDate date, long seq, String machine) {
        String payload = type.name() + date + seq + machine;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            // 3 octets → entier 0–16 777 215, réduit à 0–46 655 (3 chars base36 max = 46 655)
            int val = ((hash[0] & 0xFF) << 16 | (hash[1] & 0xFF) << 8 | (hash[2] & 0xFF))
                      % (36 * 36 * 36);
            String encoded = Integer.toString(val, 36); // base36 lowercase
            return String.format("%3s", encoded).replace(' ', '0'); // zero-pad à gauche
        } catch (NoSuchAlgorithmException e) {
            return "000";
        }
    }

    private DocumentSequence createSequence(BillingDocumentType type, int year) {
        String prefix = switch (type) {
            case QUOTE            -> "DEV";
            case INVOICE          -> "FAC";
            case CREDIT_NOTE      -> "AVR";
            case DEBIT_NOTE       -> "DBN";
            case PROFORMA_INVOICE -> "PRF";
        };
        DocumentSequence seq = new DocumentSequence();
        seq.setDocumentType(type.name());
        seq.setYear(year);
        seq.setPrefix(prefix);
        seq.setCurrentValue(0L);
        seq.setCreatedAt(Instant.now());
        seq.setUpdatedAt(Instant.now());
        return sequenceRepository.save(seq);
    }
}
```

> **Appel :** `nextNumber(type, fiscalDate)` reçoit la date fiscale calculée par le
> service de validation (pas `LocalDate.now()` dans la méthode elle-même) pour que
> la date dans le numéro corresponde exactement à la date d'émission officielle.

#### `DocumentSequenceRepository.java`

```java
@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    @Query(nativeQuery = true, value = """
            SELECT * FROM document_sequence
            WHERE document_type = :type
              AND year = :year
            FOR UPDATE
            """)
    Optional<DocumentSequence> findLockedByTypeAndYear(
            @Param("type") String type,
            @Param("year") int year);
}
```

#### Vérification d'intégrité — extraction de séquence et de date

Format à analyser : `FAC20260617-00001abc-BKTWK`

```java
private static final Pattern SEFC_PATTERN =
        Pattern.compile("^([A-Z]+)(\\d{8})-(\\d+)([a-z0-9]{3})-([A-Z0-9]+)$");

private long extractSeq(String documentNumber) {
    Matcher m = SEFC_PATTERN.matcher(documentNumber);
    if (!m.matches()) return -1;
    try { return Long.parseLong(m.group(3)); }  // groupe 3 = partie numérique du compteur
    catch (Exception e) { return -1; }
}

private LocalDate extractFiscalDate(String documentNumber) {
    Matcher m = SEFC_PATTERN.matcher(documentNumber);
    if (!m.matches()) return null;
    try { return LocalDate.parse(m.group(2), DateTimeFormatter.BASIC_ISO_DATE); }
    catch (Exception e) { return null; }
}

private int extractYear(String documentNumber) {
    LocalDate d = extractFiscalDate(documentNumber);
    return d != null ? d.getYear() : -1;
}
```

La continuité est vérifiée **par année** : `00001, 00002, …, 00047` sans saut,
indépendamment des dates individuelles dans chaque numéro.

**Note :** `BillingNumberingSupport` reste en place pour les anciens documents.
`DocumentSequenceService` est utilisé pour tous les nouveaux documents créés après
l'activation de cette phase. Les numéros existants sont conservés.

---

### 3.3 Snapshot vendeur + identification client (NIU optionnel)

Le NIU n'existe que pour les entités immatriculées. Un particulier (personne physique)
n'en a pas. Le modèle doit refléter cette réalité sans imposer un champ obligatoire.

#### Règle de résolution du NIU client

| Profil client | Champ `customer_niu` | Mention sur la facture |
|--------------|---------------------|----------------------|
| Entreprise / membre professionnel avec NIU | NIU renseigné | "NIU : MXXXXXXXXXX" |
| Particulier / client sans NIU | `null` | "Client particulier" |
| Entreprise sans NIU enregistré encore | `null` | *(aucune mention)* |

**La facture est valide dans les deux cas.** La DGID n'exige le NIU client que pour
les transactions inter-entreprises au-dessus d'un seuil défini par décret. En dessous
du seuil ou pour les particuliers, la facture reste conforme sans ce champ.

#### Migration V157

```sql
-- V157__billing_document_fiscal_fields.sql
ALTER TABLE billing_document
    ADD COLUMN seller_name          VARCHAR(255),
    ADD COLUMN seller_niu           VARCHAR(100),
    ADD COLUMN seller_address_json  JSONB,
    ADD COLUMN seller_phone         VARCHAR(60),
    ADD COLUMN seller_email         VARCHAR(255),
    -- Optionnel : renseigné uniquement si le client est une entité immatriculée
    ADD COLUMN customer_niu         VARCHAR(100),
    -- Permet d'afficher "ENTREPRISE" ou "PARTICULIER" sur la facture/PDF
    ADD COLUMN customer_category    VARCHAR(40);
```

> `customer_niu` et `customer_category` sont nullable sans DEFAULT — aucune contrainte
> NOT NULL, aucune valeur par défaut. Un document sans ces champs reste légalement valide.

#### Configuration applicative (`application.yml`)

```yaml
app:
  company:
    name: "Elle a Osé"
    legal-name: "SAS Elle a Osé"
    # Valeurs vides acceptées avant immatriculation définitive
    niu: "${COMPANY_NIU:}"
    fiscal-id: "${COMPANY_FISCAL_ID:000000}"
    address: "Rue 1234, Brazzaville, Congo"
    phone: "+242 06 XXX XX XX"
    email: "contact@elleaose.com"
```

#### `BillingCustomerSnapshotResolver.java` — logique de résolution

```java
// Résolution du NIU client et de la catégorie
private void resolveCustomerFiscalInfo(BillingDocument doc, String customerType, String customerCode) {
    // Exemple : lookup depuis l'entité Customer ou Member
    // Si le client a un champ "companyNiu" renseigné → on le prend
    // Sinon → customer_niu reste null, customer_category = "PARTICULIER"
    Optional<String> niu = resolveNiuFromCustomer(customerType, customerCode);
    if (niu.isPresent()) {
        doc.setCustomerNiu(niu.get());
        doc.setCustomerCategory("ENTREPRISE");
    } else {
        doc.setCustomerNiu(null);
        doc.setCustomerCategory("PARTICULIER");
    }
}

// Résolution des infos vendeur depuis la configuration — toujours présentes
private void resolveSellerInfo(BillingDocument doc) {
    doc.setSellerName(sellerName);                  // @Value("${app.company.legal-name:}")
    doc.setSellerNiu(sellerNiu.isBlank() ? null : sellerNiu);  // @Value("${app.company.niu:}")
    doc.setSellerPhone(sellerPhone);
    doc.setSellerEmail(sellerEmail);
    doc.setSellerAddressJson(sellerAddressJson);
}
```

**Sur le PDF :** le template ne doit afficher le bloc NIU client que si `customer_niu != null`.
Le bloc NIU vendeur est affiché uniquement si `seller_niu != null`.
La mention `customer_category` (`ENTREPRISE` / `PARTICULIER`) est toujours visible.

---

### 3.4 Journal d'audit fiscal (`FiscalAuditLog`)

#### Migration V158

```sql
-- V158__fiscal_audit_log.sql
CREATE TABLE fiscal_audit_log (
    id           BIGINT        PRIMARY KEY,
    actor_code   VARCHAR(120),
    action       VARCHAR(80)   NOT NULL,
    entity_type  VARCHAR(60)   NOT NULL,
    entity_id    VARCHAR(120)  NOT NULL,
    old_value    TEXT,
    new_value    TEXT,
    ip_address   VARCHAR(60),
    user_agent   TEXT,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fiscal_audit_entity ON fiscal_audit_log (entity_type, entity_id);
CREATE INDEX idx_fiscal_audit_action ON fiscal_audit_log (action, created_at);
```

#### `FiscalAuditLog.java` (entité)

```java
@Entity
@Table(name = "fiscal_audit_log")
public class FiscalAuditLog {
    @Id @IdGeneration
    private Long id;

    @Column(name = "actor_code",  length = 120)
    private String actorCode;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 120)
    private String entityId;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
```

#### `FiscalAuditService.java` (nouveau)

```java
@Service
@RequiredArgsConstructor
public class FiscalAuditService {

    private final FiscalAuditLogRepository repository;
    private final HttpServletRequest httpRequest;  // @RequestScope via proxy

    public void log(String action, String entityType, String entityId,
                    String oldValue, String newValue) {
        FiscalAuditLog entry = new FiscalAuditLog();
        entry.setActorCode(resolveActor());
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        entry.setIpAddress(resolveIp());
        entry.setUserAgent(httpRequest.getHeader("User-Agent"));
        entry.setCreatedAt(Instant.now());
        repository.save(entry);
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    private String resolveIp() {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        return StringUtils.hasText(forwarded)
                ? forwarded.split(",")[0].trim()
                : httpRequest.getRemoteAddr();
    }
}
```

**Actions à tracer :**

| Action (constante) | Déclencheur |
|-------------------|-------------|
| `QUOTE_CREATED` | Création d'un devis |
| `QUOTE_UPDATED` | Modification d'un devis |
| `QUOTE_DELETED` | Suppression logique d'un devis |
| `QUOTE_CONVERTED` | Conversion devis → facture |
| `INVOICE_VALIDATED` | Validation d'une facture |
| `CREDIT_NOTE_CREATED` | Création d'un avoir |
| `DEBIT_NOTE_CREATED` | Création d'une note de débit |
| `TAX_RULE_UPDATED` | Modification d'une règle de taxe |
| `PDF_EXPORTED` | Impression / export PDF |
| `TAMPER_ATTEMPT` | Tentative de modification d'une facture validée |

---

### 3.5 Suppression logique des documents fiscaux

#### Migration V159

```sql
-- V159__billing_document_soft_delete.sql
ALTER TABLE billing_document
    ADD COLUMN deleted_at     TIMESTAMPTZ,
    ADD COLUMN deleted_by     VARCHAR(120),
    ADD COLUMN delete_reason  TEXT;
```

#### `BillingDocument.java` — ajouter l'annotation soft delete

```java
@SQLRestriction("deleted_at IS NULL")
```

**Règles :**
- `QUOTE` en `DRAFT` : suppression logique autorisée (pas de conséquence fiscale)
- `INVOICE` en `VALIDATED` : suppression **interdite** — utiliser un avoir (`CREDIT_NOTE`)
- Toute suppression est tracée dans `FiscalAuditLog`

---

## 4. Phase 2 — Sécurité cryptographique

### 4.1 Chaînage cryptographique

Chaque facture validée est liée à la précédente par son hash. Toute rupture de
chaîne est détectable lors du contrôle d'intégrité.

```
FAC-2026-000001  →  hash_1 = SHA256("FAC-2026-000001|2026-06-17|CLIENT-001|250000.0000|GENESIS")
FAC-2026-000002  →  hash_2 = SHA256("FAC-2026-000002|2026-06-17|CLIENT-002|180000.0000|hash_1")
FAC-2026-000003  →  hash_3 = SHA256("FAC-2026-000003|2026-06-17|CLIENT-001|320000.0000|hash_2")
```

#### Migration V160

```sql
-- V160__billing_document_fiscal_hash.sql
ALTER TABLE billing_document
    ADD COLUMN previous_hash   VARCHAR(512),
    ADD COLUMN current_hash    VARCHAR(512),
    ADD COLUMN hash_algorithm  VARCHAR(40)  DEFAULT 'SHA-256';

CREATE INDEX idx_billing_document_hash ON billing_document (document_type, validated_at)
    WHERE locked = TRUE;
```

#### `FiscalHashService.java` (nouveau)

```java
@Service
public class FiscalHashService {

    private static final String GENESIS = "GENESIS";
    private static final String ALGORITHM = "SHA-256";

    /**
     * Calcule le hash d'une facture à partir de ses données immuables
     * et du hash de la facture précédente.
     * Appelé exclusivement lors de validate(), dans la même transaction.
     */
    public String compute(BillingDocument doc, String previousHash) {
        String payload = String.join("|",
                doc.getDocumentNumber(),
                doc.getFiscalDate().toString(),
                doc.getCustomerCode(),
                doc.getTotalAmount().toPlainString(),
                previousHash == null ? GENESIS : previousHash
        );
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String algorithm() {
        return ALGORITHM;
    }
}
```

#### `BillingDocumentRepository` — requête pour le dernier hash

```java
@Query(nativeQuery = true, value = """
    SELECT current_hash
    FROM billing_document
    WHERE document_type = :type
      AND locked = TRUE
    ORDER BY validated_at DESC
    LIMIT 1
    FOR UPDATE SKIP LOCKED
    """)
Optional<String> findLastValidatedHash(@Param("type") String type);
```

Le `FOR UPDATE SKIP LOCKED` garantit la sérialisation des validations concurrentes
pour un même type de document.

---

### 4.2 Signature numérique HMAC

La signature est calculée sur le hash de la facture avec une clé secrète stockée
**uniquement** dans les variables d'environnement — jamais en base de données.

#### Migration V161

```sql
-- V161__billing_document_fiscal_signature.sql
ALTER TABLE billing_document
    ADD COLUMN fiscal_signature     VARCHAR(512),
    ADD COLUMN signature_algorithm  VARCHAR(40)  DEFAULT 'HMAC-SHA256',
    ADD COLUMN signed_at            TIMESTAMPTZ;
```

#### Variable d'environnement — ajouter immédiatement

```bash
# .env (dev) et variable système (prod) — clé HMAC 256 bits
BILLING_FISCAL_SIGNING_KEY=<générer avec : openssl rand -hex 32>
```

#### `FiscalSignatureService.java` (nouveau)

```java
@Service
public class FiscalSignatureService {

    private static final String ALGORITHM = "HmacSHA256";

    @Value("${billing.fiscal.signing-key}")
    private String signingKey;

    public String sign(String hash) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(hash.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign fiscal hash", e);
        }
    }

    public boolean verify(String hash, String signature) {
        return MessageDigest.isEqual(
                sign(hash).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String algorithm() {
        return ALGORITHM;
    }
}
```

#### `application.yml` — ajouter

```yaml
billing:
  fiscal:
    signing-key: ${BILLING_FISCAL_SIGNING_KEY}
```

---

### 4.3 Séquence d'opérations lors de `validate()`

L'ordre d'exécution lors de la validation d'une facture doit être strict et atomique :

```
1. ensureCanValidate(doc)                     — vérif statut
2. recalcul final des montants                — BillingCalculationService
3. attribution fiscal_date = today            — date fiscale officielle
4. attribution du numéro définitif            — DocumentSequenceService.nextNumber()
5. previousHash = findLastValidatedHash()     — SELECT FOR UPDATE
6. currentHash = FiscalHashService.compute()  — SHA-256
7. fiscalSignature = FiscalSignatureService.sign(currentHash)
8. doc.setLocked(true)
9. doc.setStatus(VALIDATED)
10. doc.setValidatedAt(Instant.now())
11. billingDocumentRepository.save(doc)       — tout dans la même @Transactional
12. FiscalAuditService.log(INVOICE_VALIDATED)
```

---

### 4.4 QR code fiscal sur le PDF

Réutilise ZXing déjà présent dans le classpath.

#### `FiscalQrCodeService.java` (nouveau)

```java
@Service
public class FiscalQrCodeService {

    private static final int SIZE = 200;

    /**
     * Génère un QR code base64 PNG contenant les données fiscales essentielles.
     */
    public String generateBase64(BillingDocument doc) {
        String payload = buildPayload(doc);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, SIZE, SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPayload(BillingDocument doc) {
        // Format compact pour maximiser la lisibilité du QR
        return String.format(
                "{\"n\":\"%s\",\"d\":\"%s\",\"t\":%s,\"h\":\"%s\",\"s\":\"%s\"}",
                doc.getDocumentNumber(),
                doc.getFiscalDate(),
                doc.getTotalAmount().toPlainString(),
                abbrev(doc.getCurrentHash(), 16),
                abbrev(doc.getFiscalSignature(), 16)
        );
    }

    private String abbrev(String value, int len) {
        if (value == null) return "";
        return value.length() <= len ? value : value.substring(0, len) + "...";
    }
}
```

Injecter `FiscalQrCodeService` dans `BillingDocumentPdfServiceImpl` et passer le
QR code au template HTML/JTE uniquement pour les documents `VALIDATED`.

---

## 5. Phase 3 — Conformité avancée

### 5.1 Service de contrôle d'intégrité

#### `FiscalIntegrityCheckService.java` (nouveau)

```java
@Service
@RequiredArgsConstructor
public class FiscalIntegrityCheckService {

    private final BillingDocumentRepository documentRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final FiscalHashService hashService;
    private final FiscalSignatureService signatureService;
    private final BillingCalculationService calculationService;

    @Transactional(readOnly = true)
    public FiscalIntegrityReport check() {
        List<BillingDocument> invoices = documentRepository
                .findAllValidatedOrderByValidatedAt(BillingDocumentType.INVOICE.name());

        int checked = 0, brokenChains = 0, missingSignatures = 0,
            numberingGaps = 0, amountMismatches = 0;

        String previousHash = null;
        long previousSeq = 0;

        for (BillingDocument doc : invoices) {
            checked++;

            // 1 — Continuité numérotation
            long seq = extractSeq(doc.getDocumentNumber());
            if (seq != previousSeq + 1) numberingGaps++;
            previousSeq = seq;

            // 2 — Chaînage hash
            String expectedHash = hashService.compute(doc, previousHash);
            if (!expectedHash.equals(doc.getCurrentHash())) brokenChains++;
            previousHash = doc.getCurrentHash();

            // 3 — Signature
            if (doc.getFiscalSignature() == null ||
                    !signatureService.verify(doc.getCurrentHash(), doc.getFiscalSignature())) {
                missingSignatures++;
            }

            // 4 — Cohérence montants (à implémenter selon métier)
        }

        return new FiscalIntegrityReport(
                checked == 0 || (brokenChains + missingSignatures + numberingGaps + amountMismatches == 0),
                checked, brokenChains, missingSignatures, numberingGaps, amountMismatches,
                Instant.now()
        );
    }

    private long extractSeq(String documentNumber) {
        // "FAC-2026-000042" → 42
        try {
            String[] parts = documentNumber.split("-");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return -1;
        }
    }

    public record FiscalIntegrityReport(
            boolean valid,
            int checkedInvoices,
            int brokenChains,
            int missingSignatures,
            int numberingGaps,
            int amountMismatches,
            Instant checkedAt
    ) {}
}
```

#### `FiscalIntegrityController.java` (nouveau — accès ADMIN uniquement)

```java
@RestController
@RequestMapping(ApiPath.V1 + "/admin/fiscal-integrity")
@RequiredArgsConstructor
public class FiscalIntegrityController {

    private final FiscalIntegrityCheckService integrityCheckService;

    @GetMapping("/check")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<FiscalIntegrityCheckService.FiscalIntegrityReport> check() {
        return ResponseEntity.ok(integrityCheckService.check());
    }
}
```

---

### 5.2 Mise à niveau du template PDF

Éléments à ajouter dans le template facture (`billing-invoice.jte` ou `.html`) :

**En-tête :**
- Bloc vendeur : nom commercial, raison sociale, NIU, adresse, téléphone, email
- NIU client si renseigné
- Heure d'émission (`validated_at` formatée `dd/MM/yyyy HH:mm`)

**Corps :**
- Colonnes : Désignation / Qté / Prix unitaire HT / Remise / Montant HT / TVA / Montant TTC

**Pied de page :**
```
Facture validée – Document non modifiable
Hash : [16 premiers caractères du current_hash]
Signature : [16 premiers caractères de fiscal_signature]
```

**Zone QR (haut droite ou bas droit) :**
```html
<img src="data:image/png;base64,${qrCodeBase64}" width="120" height="120"
     alt="QR de vérification fiscale" />
```

---

## 6. Récapitulatif des migrations Flyway

| Migration | Contenu | Phase |
|-----------|---------|-------|
| **V155** | `locked`, `validated_at`, `fiscal_date` sur `billing_document` | 1 |
| **V156** | Nouvelle table `document_sequence` + amorçage 2026 | 1 |
| **V157** | Snapshot vendeur + `customer_niu` sur `billing_document` | 1 |
| **V158** | Nouvelle table `fiscal_audit_log` | 1 |
| **V159** | `deleted_at`, `deleted_by`, `delete_reason` sur `billing_document` | 1 |
| **V160** | `previous_hash`, `current_hash`, `hash_algorithm` | 2 |
| **V161** | `fiscal_signature`, `signature_algorithm`, `signed_at` | 2 |

---

## 7. Nouveaux services à créer (résumé)

| Service | Package | Dépendances |
|---------|---------|-------------|
| `DocumentSequenceService` | `billing/service/fiscal/` | `DocumentSequenceRepository` |
| `FiscalAuditService` | `billing/service/fiscal/` | `FiscalAuditLogRepository`, `HttpServletRequest` |
| `FiscalHashService` | `billing/service/fiscal/` | — (algorithme pur) |
| `FiscalSignatureService` | `billing/service/fiscal/` | `@Value signing-key` |
| `FiscalQrCodeService` | `billing/service/fiscal/` | ZXing (déjà présent) |
| `FiscalIntegrityCheckService` | `billing/service/fiscal/` | tous les services ci-dessus |

---

## 8. Ordre de développement recommandé

```
Sprint 1 — Phase 1 : fondation (2–3 jours)
 ├── V155 + VALIDATED + ensureNotLocked()
 ├── V156 + DocumentSequenceService
 ├── V157 + snapshot vendeur dans BillingCustomerSnapshotResolver
 ├── V158 + FiscalAuditService (+ wiring dans BillingDocumentServiceImpl)
 └── V159 + soft delete

Sprint 2 — Phase 2 : cryptographie (2 jours)
 ├── Générer BILLING_FISCAL_SIGNING_KEY et l'ajouter aux envs
 ├── V160 + FiscalHashService
 ├── V161 + FiscalSignatureService
 ├── Câblage dans la méthode validate() (séquence §4.3)
 └── FiscalQrCodeService + intégration template PDF

Sprint 3 — Phase 3 : conformité (1–2 jours)
 ├── FiscalIntegrityCheckService + FiscalIntegrityController
 └── Finalisation template PDF (mentions obligatoires, QR, pied de page)
```

---

## 9. Points d'attention

### Sécurité de la clé de signature
La clé `BILLING_FISCAL_SIGNING_KEY` ne doit jamais apparaître dans le code source,
les logs ou la base de données. En production, la stocker dans le gestionnaire de
secrets de l'hébergeur (variable d'environnement système, vault, ou secret manager).

### Rétrocompatibilité des numéros
Les documents existants conservent leur numéro au format `INV-MEM-YYYYMMDD-NNNNN`.
Seuls les documents créés après l'activation de V156 utilisent le nouveau format
`FAC-YYYY-NNNNNN`. Le service `FiscalIntegrityCheckService` ne vérifie la
continuité que sur les documents du nouveau format.

### Chaînage — démarrage du genesis
La première facture validée dans le nouveau système aura `previousHash = NULL`
(valeur sentinel `"GENESIS"` dans le calcul). Le contrôle d'intégrité doit
accepter cette valeur de départ.

### Performance du contrôle d'intégrité
Sur un volume élevé, l'endpoint `/fiscal-integrity/check` peut être lent.
Prévoir une exécution en tâche de fond nocturne planifiée et un cache du dernier
rapport (table `fiscal_integrity_report` ou Redis).
