# Billing — Immutabilité fiscale : Guide Frontend

**Module :** `billing/`  
**Base URL :** `/sni/api/v1/billing`  
**Auth :** JWT Bearer — rôle ADMIN ou SUPER_ADMIN requis  
**Date :** 2026-06-30

---

## Nouveaux champs sur `BillingDocumentResponse`

Les trois champs suivants sont disponibles sur **tous** les documents de correction (avoir, rectificative) :

| Champ | Type | Description |
|-------|------|-------------|
| `originalDocumentNumber` | `string \| null` | Numéro du document corrigé (`INV-...`, `PRF-...`) |
| `originalDocumentType` | `string \| null` | Type du document corrigé (`INVOICE`, `PROFORMA_INVOICE`) |
| `creditNoteReason` | `string \| null` | Motif de l'avoir / de la rectificative |

Le champ `documentType` peut maintenant valoir `CORRECTIVE_INVOICE` en plus des valeurs existantes.

---

## Nouveau type de document

| `documentType` | Label à afficher | Préfixe numéro |
|----------------|-----------------|----------------|
| `CORRECTIVE_INVOICE` | Facture rectificative | `REC-` |

---

## Nouveaux endpoints

### 1. Créer un avoir sur une facture validée

```
POST /sni/api/v1/billing/invoices/{invoiceNumber}/credit-note
```

**Corps de la requête** (tous les champs sont optionnels sauf `reason`) :

```json
{
  "amount": 50000,
  "reason": "Erreur sur la ligne TVA",
  "validateImmediately": true,
  "applyImmediately": true,
  "lines": null
}
```

| Champ | Type | Défaut | Description |
|-------|------|--------|-------------|
| `amount` | `number \| null` | `balanceDue` de la facture | Montant de l'avoir |
| `reason` | `string` | requis | Motif visible sur le document |
| `validateImmediately` | `boolean \| null` | `false` | Sceller l'avoir immédiatement |
| `applyImmediately` | `boolean \| null` | `false` | Appliquer l'avoir sur la facture source |
| `lines` | `array \| null` | `null` | Lignes manuelles (null = ligne auto) |

**Réponse** : `200 BillingDocumentResponse` — le document créé est l'**avoir** (`documentType: CREDIT_NOTE`).

**Cas d'usage typique** (annulation totale d'une facture) :
```json
{
  "amount": null,
  "reason": "Annulation commande client",
  "validateImmediately": true,
  "applyImmediately": true
}
```

---

### 2. Ré-appliquer un avoir sur une autre facture

Utile quand un avoir n'a pas été entièrement consommé sur sa facture source et qu'on veut le déduire d'une autre facture du même client.

```
PATCH /sni/api/v1/billing/credit-notes/{creditNoteNumber}/apply-to/{targetInvoiceNumber}
```

**Paramètres URL :**
- `creditNoteNumber` — numéro de l'avoir (`CRN-...`)
- `targetInvoiceNumber` — numéro de la facture cible (`INV-...` ou `PRF-...`)

**Prérequis backend (la requête échoue si non respectés) :**
- L'avoir doit être `VALIDATED` (locked=true)
- L'avoir ne doit pas encore être `ISSUED` (statut = déjà appliqué)
- Le client de l'avoir et de la facture cible doit être le même

**Réponse** : `200 BillingDocumentResponse` — l'avoir (statut passe à `ISSUED`).

---

### 3. Créer une facture rectificative

Corrige une erreur de contenu (article incorrect, taux TVA erroné) sur une facture déjà validée. La facture d'origine n'est pas modifiée.

```
POST /sni/api/v1/billing/invoices/{originalInvoiceNumber}/corrective
```

**Corps de la requête** (même structure que `CreateManualBillingDocumentRequest`) :

```json
{
  "currency": "XAF",
  "title": "Facture rectificative INV-20260617-...",
  "description": "Correction de la TVA applicable",
  "lines": [
    {
      "lineType": "SERVICE",
      "itemCode": "DESK-FLEX",
      "description": "Bureau flexible — TVA corrigée 19.25%",
      "quantity": 1,
      "unitPrice": 50000,
      "taxable": true,
      "taxIncluded": false,
      "vatRate": 19.25
    }
  ]
}
```

**Réponse** : `201 BillingDocumentResponse` avec `documentType: CORRECTIVE_INVOICE` en statut `DRAFT`.

> La rectificative doit ensuite être validée manuellement via `PATCH /documents/{number}/validate`.

---

### 4. Annuler une facture (comportement modifié)

```
DELETE /sni/api/v1/billing/documents/{documentNumber}?reason=Annulation+client
```

**Comportement selon l'état** :

| État du document | Comportement |
|-----------------|-------------|
| `locked = false` | Annulation directe (`status → CANCELLED`) — comportement inchangé |
| `locked = true` | Crée un avoir pour le `balanceDue`, le valide, et l'applique automatiquement. La facture originale reste `VALIDATED` et passe en `PAID`. |

**Depuis le frontend**, il est désormais inutile de vérifier `locked` avant d'appeler ce endpoint — la logique est gérée côté backend.

---

## Règles d'affichage

### Identifier un document de correction

```ts
function isCorrectionDocument(doc: BillingDocumentResponse): boolean {
  return doc.documentType === 'CREDIT_NOTE' || doc.documentType === 'CORRECTIVE_INVOICE';
}
```

### Afficher le lien vers le document source

```ts
if (doc.originalDocumentNumber) {
  // Afficher un lien "Correction de {originalDocumentNumber}"
}
```

### Badge de type

| `documentType` | Badge | Couleur suggérée |
|----------------|-------|-----------------|
| `INVOICE` | Facture | Bleu |
| `CREDIT_NOTE` | Avoir | Orange |
| `CORRECTIVE_INVOICE` | Rectificative | Violet |
| `QUOTE` | Devis | Gris |
| `PROFORMA_INVOICE` | Pro forma | Gris clair |
| `DEBIT_NOTE` | Note de débit | Rouge |

### Actions disponibles selon `locked`

| Action | `locked=false` | `locked=true` |
|--------|----------------|---------------|
| Modifier le contenu | ✅ | ❌ |
| Valider | ✅ | ❌ déjà validé |
| Annuler | ✅ (direct) | ✅ (via avoir auto) |
| Créer un avoir | ❌ | ✅ |
| Créer une rectificative | ❌ | ✅ |

---

## Clôtures périodiques (lecture seule)

Les clôtures sont calculées automatiquement (daily/monthly/annual) — aucune action frontend requise. Si un futur endpoint de consultation est exposé, les champs disponibles seront :

| Champ | Description |
|-------|-------------|
| `periodType` | `DAY`, `MONTH` ou `YEAR` |
| `periodLabel` | `2026-06-29`, `2026-06` ou `2026` |
| `documentType` | Type des documents agrégés |
| `totalDocuments` | Nombre de documents scellés |
| `totalInvoiced` | Somme des montants facturés |
| `totalPaid` | Somme des paiements reçus |
| `totalCreditNotes` | Somme des avoirs émis |
| `cumulativeTotal` | Total cumulatif depuis l'origine |
| `closureHash` | Empreinte SHA-256 de la clôture |

---

## Flux complet — Annulation d'une facture validée

```
Frontend                          Backend
   │                                 │
   │  DELETE /documents/INV-xxx      │
   │─────────────────────────────►   │
   │                                 │  locked=true → émet avoir CRN-xxx
   │                                 │  validate(CRN-xxx)
   │                                 │  applyCreditNote(CRN-xxx)
   │                                 │  INV-xxx.status → PAID
   │◄────────────────────────────    │
   │  200 { documentType: INVOICE,   │
   │         status: PAID, ... }     │
   │                                 │
```

Afficher le message : "La facture a été annulée via avoir **{creditNoteNumber}**."

---

## Flux complet — Rectification d'une erreur

```
1. POST /invoices/INV-xxx/corrective  →  REC-xxx (DRAFT)
2. Afficher REC-xxx pour revue admin
3. PATCH /documents/REC-xxx/validate  →  REC-xxx (VALIDATED, locked=true)
4. Afficher "Facture rectificative validée — INV-xxx conservée à titre d'historique"
```
