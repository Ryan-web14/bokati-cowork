# Billing — Immutabilité fiscale (SEFC) : Guide d'implémentation

> Module : `features/billing/`  
> Date : 2026-06-30  
> Statut : **Implémenté**

---

## 1. Principe

Dès qu'une facture est **validée** (sealed), elle devient fiscalement immuable :

- Son contenu financier (montants, client, numéro fiscal, hashes) ne peut plus être modifié
- Toute annulation ou correction passe obligatoirement par un **avoir** (`CREDIT_NOTE`) ou une **facture rectificative** (`CORRECTIVE_INVOICE`)
- Trois niveaux de protection : applicatif (`ensureNotLocked`), base de données (triggers), chaîne cryptographique (SHA-256 + HMAC)

---

## 2. Cycle de vie des documents

```
DRAFT ──────────── issue() ─────► ISSUED
  │                                  │
  │                                validate()
  │                                  │
  └──────────────────────────────► VALIDATED (locked=true, fiscalNumber assigné)
                                      │
                      ┌───────────────┼───────────────┐
                      │               │               │
               applyPayment()  createCreditNote()  createCorrectiveInvoice()
                      │               │               │
                   PAID           CREDIT_NOTE(DRAFT)  CORRECTIVE_INVOICE(DRAFT)
                                      │               │
                                  validate()       validate()
                                      │               │
                                  VALIDATED        VALIDATED
```

**Règle fondamentale** : `cancelAndArchive()` sur un document `locked=true` émet automatiquement un avoir — la facture originale n'est jamais mutée.

---

## 3. Types de documents

| Type | Préfixe interne | Préfixe fiscal | Usage |
|------|-----------------|----------------|-------|
| `INVOICE` | `INV-` | `FAC` | Facture standard |
| `CREDIT_NOTE` | `CRN-` | `AVR` | Avoir — annule tout ou partie |
| `CORRECTIVE_INVOICE` | `REC-` | `REC` | Rectifie une erreur (TVA, article) |
| `QUOTE` | `QUO-` | `DEV` | Devis |
| `PROFORMA_INVOICE` | `PRF-` | `PRF` | Facture proforma |
| `DEBIT_NOTE` | `DBN-` | `DBN` | Note de débit |

---

## 4. API — Endpoints de correction

### 4.1 Créer un avoir sur une facture

```
POST /api/v1/billing/invoices/{invoiceNumber}/credit-note
Content-Type: application/json

{
  "amount": 50000,           // null = balanceDue de la facture
  "reason": "Erreur de facturation",
  "validateImmediately": true,   // scelle l'avoir dans la même transaction
  "applyImmediately": true,      // applique l'avoir sur la facture source
  "lines": null                  // null = ligne ADJUSTMENT auto-générée
}
```

**Réponse** : `200 BillingDocumentResponse` avec `documentType: CREDIT_NOTE`

**Champs de liaison** :
```json
{
  "originalDocumentNumber": "INV-20260617-00001abc-MBR",
  "originalDocumentType": "INVOICE",
  "creditNoteReason": "Erreur de facturation"
}
```

### 4.2 Ré-appliquer un avoir sur une autre facture du même client

```
PATCH /api/v1/billing/credit-notes/{creditNoteNumber}/apply-to/{targetInvoiceNumber}
```

**Prérequis** :
- L'avoir doit être `VALIDATED` (locked=true)
- L'avoir ne doit pas être déjà `ISSUED` (consommé)
- Les deux documents doivent partager le même `customerCode`

**Réponse** : `200 BillingDocumentResponse` (avoir passé à ISSUED)

### 4.3 Créer une facture rectificative

```
POST /api/v1/billing/invoices/{originalInvoiceNumber}/corrective
Content-Type: application/json

{
  "currency": "XAF",
  "lines": [
    {
      "lineType": "SERVICE",
      "itemCode": "DESK-FLEX",
      "description": "Forfait bureau — TVA corrigée",
      "quantity": 1,
      "unitPrice": 50000,
      "taxable": true,
      "taxIncluded": false,
      "vatRate": 19.25
    }
  ],
  "title": "Facture rectificative INV-2026...",
  "description": "Correction TVA sur ligne 1"
}
```

**Prérequis** : La facture d'origine doit être `VALIDATED` (locked=true).

**Réponse** : `201 BillingDocumentResponse` avec `documentType: CORRECTIVE_INVOICE` en statut DRAFT.

> La rectificative doit ensuite être validée séparément via `PATCH /documents/{number}/validate`.

### 4.4 Annuler une facture validée

```
// Endpoint existant — comportement modifié automatiquement
DELETE /api/v1/billing/documents/{documentNumber}?reason=Annulation+client
```

Si `document.locked = true` → crée automatiquement un avoir pour le `balanceDue`, le valide, et l'applique. La facture originale reste VALIDATED et passe en PAID.

Si `document.locked = false` → annulation directe (comportement précédent).

---

## 5. Architecture — Composants modifiés

### 5.1 `BillingDocument` — Nouveaux champs

```java
@Column(name = "original_document_number", length = 100)
private String originalDocumentNumber;  // Numéro de la facture corrigée

@Column(name = "original_document_type", length = 40)
private String originalDocumentType;    // "INVOICE", "PROFORMA_INVOICE"

@Column(name = "credit_note_reason", columnDefinition = "TEXT")
private String creditNoteReason;        // Motif de l'avoir/rectificative
```

### 5.2 `BillingDocumentType` — Nouveau type

```java
CORRECTIVE_INVOICE  // Facture rectificative — préfixe fiscal REC
```

### 5.3 `FiscalAuditService` — Nouvelles constantes

```java
public static final String CREDIT_NOTE_APPLIED        = "CREDIT_NOTE_APPLIED";
public static final String CORRECTIVE_INVOICE_CREATED = "CORRECTIVE_INVOICE_CREATED";
```

### 5.4 `CreateCreditNoteRequest` — Nouveau champ

```java
Boolean validateImmediately  // Scelle l'avoir dans la même transaction
```

---

## 6. Triggers PostgreSQL (V188)

Trois triggers protègent les documents scellés au niveau base de données :

| Trigger | Table | Déclencheur | Effet |
|---------|-------|-------------|-------|
| `trg_billing_document_immutable` | `billing_document` | `BEFORE UPDATE WHERE locked=TRUE` | Bloque toute modification des champs financiers |
| `trg_billing_document_no_delete` | `billing_document` | `BEFORE DELETE WHERE locked=TRUE` | Bloque la suppression physique |
| `trg_billing_line_immutable` | `billing_document_line` | `BEFORE UPDATE OR DELETE` | Bloque si le document parent est locked |

**Champs autorisés** sur un document locked :
`status`, `paid_amount`, `balance_due`, `paid_at`, `cancelled_at`, `archived_at`, `deleted_at`, `deleted_by`, `delete_reason`, `updated_at`

---

## 7. Clôtures périodiques (V189)

Worker automatique qui calcule et persiste les totaux par période :

| Cron | Fréquence | Période calculée |
|------|-----------|-----------------|
| `0 30 23 * * *` | Quotidien à 23h30 | Journée J-1 |
| `0 15 0 1 * *` | 1er du mois à 00h15 | Mois M-1 |
| `0 0 1 1 1 *` | 1er janvier à 01h00 | Année N-1 |

Chaque clôture est **append-only** (trigger DB interdit UPDATE/DELETE) et chaîne son hash SHA-256 avec la clôture précédente.

Variables d'environnement pour surcharger les crons :
- `BILLING_DAILY_CLOSURE_CRON`
- `BILLING_MONTHLY_CLOSURE_CRON`
- `BILLING_ANNUAL_CLOSURE_CRON`

---

## 8. Migrations Flyway

| Version | Fichier | Contenu |
|---------|---------|---------|
| V191 | `V191__billing_document_corrective_link.sql` | 3 colonnes + index sur `billing_document` |
| V192 | `V192__billing_immutability_triggers.sql` | 3 triggers PL/pgSQL |
| V193 | `V193__billing_period_closure.sql` | Table `billing_period_closure` + trigger append-only |

Les fichiers existent en version `migration/` (dev, BIGSERIAL) et `migration-prod/` (prod, BIGINT).

---

## 9. Invariants garantis

| # | Invariant | Mécanisme |
|---|-----------|-----------|
| I1 | Document DRAFT = liberté totale | Aucune restriction |
| I2 | Validation = commit atomique irréversible | `validate()` en `@Transactional` |
| I3 | Post-validation : contenu financier immuable | `ensureNotLocked()` + triggers V188 |
| I4 | Toute correction passe par un avoir ou rectificative | `cancelAndArchive()` + nouveaux endpoints |
| I5 | Chaîne cryptographique intègre | SHA-256 + HMAC + worker nocturne |
