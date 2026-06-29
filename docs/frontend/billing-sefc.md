# API Billing SEFC — Documentation Backend & Frontend Admin

**Module :** `billing/`  
**Base URL :** `/sni/api/v1/billing`  
**Auth :** JWT Bearer — rôle ADMIN ou SUPER_ADMIN requis sur toutes les routes  
**Date :** 2026-06-17

---

## Nouveautés Phase 1 SEFC

### Champs fiscaux ajoutés sur `BillingDocumentResponse`

| Champ | Type | Description |
|-------|------|-------------|
| `locked` | `boolean` | `true` = document validé, immuable |
| `fiscalNumber` | `string \| null` | Numéro SEFC définitif (`FAC20260617-00001abc-BKTWK`) |
| `fiscalDate` | `date \| null` | Date fiscale officielle (YYYY-MM-DD) |
| `validatedAt` | `datetime \| null` | Horodatage ISO 8601 de la validation |
| `sellerName` | `string \| null` | Raison sociale de l'émetteur (snapshot) |
| `sellerNiu` | `string \| null` | NIU de l'émetteur (null si non enregistré) |
| `sellerPhone` | `string \| null` | Téléphone émetteur |
| `sellerEmail` | `string \| null` | Email émetteur |
| `customerNiu` | `string \| null` | NIU client (null si particulier) |
| `customerCategory` | `string \| null` | `ENTREPRISE` ou `PARTICULIER` |

---

## Endpoints

### `POST /billing/documents`
Créer un document (devis, facture, avoir…).

**Body** `CreateBillingDocumentRequest`

```json
{
  "documentType": "INVOICE",
  "customerType": "MEMBER",
  "customerCode": "MEM-2026-000001",
  "customerName": null,
  "currency": "XAF",
  "issueDate": "2026-06-17",
  "dueDate": "2026-07-17",
  "lines": [
    {
      "description": "Location salle de réunion — 2h",
      "quantity": 1,
      "unitPrice": 25000,
      "taxable": true
    }
  ]
}
```

**Réponse `200`** → `BillingDocumentResponse`  
Le document est créé en statut `DRAFT`. Le champ `locked` est `false`. Le `fiscalNumber` est `null`.

---

### `PATCH /billing/documents/{documentNumber}/issue`
Passe le document de `DRAFT` → `ISSUED`.  
Le document reste modifiable après cette étape.

**Réponse `200`** → `BillingDocumentResponse` avec `status: "ISSUED"`

---

### `PATCH /billing/documents/{documentNumber}/validate` ⭐ NOUVEAU
**Valide fiscalement le document (SEFC).**

- Assigne le **numéro fiscal définitif** (format `FAC20260617-00001abc-BKTWK`)
- Fixe la **date fiscale** au jour de la validation
- Passe le statut à `VALIDATED`
- **Verrouille** le document — toute modification ultérieure est refusée (HTTP 400)
- Écrit une entrée dans le **journal d'audit fiscal**

> Seuls les documents de type `INVOICE`, `CREDIT_NOTE`, `DEBIT_NOTE`, `PROFORMA_INVOICE`
> en statut `DRAFT` ou `ISSUED` peuvent être validés.

**Requête :** `PATCH /sni/api/v1/billing/documents/INV-MEM-20260617-000001/validate`  
**Body :** aucun

**Réponse `200`**
```json
{
  "documentNumber": "INV-MEM-20260617-000001",
  "documentType": "INVOICE",
  "status": "VALIDATED",
  "locked": true,
  "fiscalNumber": "FAC20260617-00001abc-BKTWK",
  "fiscalDate": "2026-06-17",
  "validatedAt": "2026-06-17T14:23:00.000Z",
  "customerNiu": null,
  "customerCategory": "PARTICULIER",
  "sellerName": "SAS Elle a Osé",
  "sellerNiu": null,
  ...
}
```

**Erreurs**
| Code | Cause |
|------|-------|
| `400` | Document déjà validé, ou statut incompatible |
| `400` | Type de document non fiscal (ex : QUOTE) |
| `404` | Document introuvable |

---

### `PUT /billing/documents/{documentNumber}`
Modifier un document.

> **Si `locked: true`** → HTTP 400 "Document validé (SEFC) — il ne peut plus être modifié"

---

### `POST /billing/invoices/manual`
Créer une facture manuelle.

**Body** `CreateManualBillingDocumentRequest`

```json
{
  "customerType": "CUSTOMER",
  "customerCode": "CUS-2026-000042",
  "currency": "XAF",
  "lines": [...]
}
```

---

### `POST /billing/invoices/from-billable-items`
Facturer des éléments facturables (abonnements, passes).

**Body** `CreateInvoiceFromBillableItemsRequest`

---

### `POST /billing/invoices/from-reservation`
Générer une facture depuis une réservation.

**Body** `CreateReservationInvoiceRequest`

---

### `POST /billing/quotes/manual`
Créer un devis manuel.

---

### `PATCH /billing/documents/{documentNumber}/send`
Envoyer le document par email.

---

### `GET /billing/documents`
Lister les documents avec filtres.

**Query params**
| Paramètre | Type | Description |
|-----------|------|-------------|
| `documentType` | `INVOICE \| QUOTE \| CREDIT_NOTE \| DEBIT_NOTE \| PROFORMA_INVOICE` | Filtre par type |
| `status` | `BillingDocumentStatus` | Filtre par statut |
| `customerType` | `string` | `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |
| `customerCode` | `string` | Code client |
| `sourceType` | `string` | Type de source (ex : `BOOKING`) |
| `sourceCode` | `string` | Code source |
| `fromDate` | `date` | Date d'émission ≥ |
| `toDate` | `date` | Date d'émission ≤ |
| `searchText` | `string` | Recherche texte libre |
| `page` | `int` | Page (0-based) |
| `size` | `int` | Taille de page (défaut 20) |

---

### `GET /billing/documents/{documentNumber}`
Détail d'un document.

---

### `GET /billing/documents/{documentNumber}/pdf`
Télécharger le PDF du document.

**Headers réponse**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="FAC20260617-00001abc-BKTWK.pdf"
```

---

### `POST /billing/quotes/{quoteNumber}/request-signature`
Demander une signature électronique sur un devis.

---

### `POST /billing/quotes/{quoteNumber}/convert-to-invoice`
Convertir un devis accepté en facture.

---

### `POST /billing/invoices/{invoiceNumber}/credit-note`
Créer un avoir sur une facture validée.

> Un avoir (`CREDIT_NOTE`) doit lui aussi être validé via `/validate` pour être fiscal.

---

### `POST /billing/invoices/pay`
Enregistrer un paiement.

---

## Statuts possibles d'un document

```
DRAFT
  │
  ├─ ISSUED ──→ VALIDATED (SEFC scellé, locked=true)
  │               │
  │               └─ SENT → VIEWED → ACCEPTED / REJECTED
  │
  ├─ CANCELLED
  └─ VOIDED

VALIDATED peut encore évoluer vers :
  SENT, PAID, PARTIALLY_PAID, OVERDUE, REFUNDED, WRITTEN_OFF
  (via paiements et transitions comptables, pas via édition du contenu)
```

---

## Cycle de vie recommandé pour une facture SEFC

```
1. POST /billing/invoices/manual          → statut DRAFT
2. PUT  /billing/documents/{n}            → révisions (tant que non validé)
3. PATCH /billing/documents/{n}/issue     → statut ISSUED (optionnel)
4. PATCH /billing/documents/{n}/validate  → statut VALIDATED + fiscalNumber assigné
5. PATCH /billing/documents/{n}/send      → envoi email client
6. POST  /billing/invoices/pay            → enregistrement paiement
```

---

## Format du numéro fiscal SEFC

```
FAC20260617-00001abc-BKTWK
 │    │       │    │    │
 │    │       │    │    └─ Code machine DGID (5 chars, configuré)
 │    │       │    └─ Nonce 3 chars base36 (anti-contrefaçon, déterministe)
 │    │       └─ Compteur annuel continu 5+ chiffres (ex : 00001)
 │    └─ Date d'émission YYYYMMDD
 └─ Préfixe type : FAC / DEV / AVR / DBN / PRF
```

**Règle de continuité :** le compteur ne repart jamais à 1 en cours d'année fiscale.
La DGID peut vérifier l'absence de trous : 00001, 00002, …, 00047.

---

## Guide d'intégration frontend admin

### Afficher le badge de statut fiscal

```typescript
function getStatusBadge(doc: BillingDocumentResponse) {
  if (doc.locked) {
    return { label: 'Validé SEFC', color: 'green', icon: 'shield-check' };
  }
  switch (doc.status) {
    case 'DRAFT':   return { label: 'Brouillon', color: 'gray' };
    case 'ISSUED':  return { label: 'Émis', color: 'blue' };
    case 'SENT':    return { label: 'Envoyé', color: 'indigo' };
    case 'PAID':    return { label: 'Payé', color: 'green' };
    case 'OVERDUE': return { label: 'En retard', color: 'red' };
    default:        return { label: doc.status, color: 'gray' };
  }
}
```

### Afficher le numéro de référence

```typescript
// Afficher le numéro fiscal si validé, sinon le numéro système
function getDisplayNumber(doc: BillingDocumentResponse): string {
  return doc.fiscalNumber ?? doc.documentNumber;
}
```

### Désactiver les actions d'édition

```typescript
// Désactiver le bouton "Modifier" si le document est validé
const canEdit = !doc.locked;

// Afficher le bouton "Valider (SEFC)" uniquement si éligible
const canValidate =
  !doc.locked &&
  ['INVOICE', 'CREDIT_NOTE', 'DEBIT_NOTE', 'PROFORMA_INVOICE'].includes(doc.documentType) &&
  ['DRAFT', 'ISSUED'].includes(doc.status);
```

### Bloc informations fiscales dans le détail

```tsx
{doc.locked && (
  <FiscalInfoCard>
    <Field label="Numéro fiscal" value={doc.fiscalNumber} mono />
    <Field label="Date fiscale" value={formatDate(doc.fiscalDate)} />
    <Field label="Validé le" value={formatDatetime(doc.validatedAt)} />
    <Divider />
    <Field label="Émetteur" value={doc.sellerName ?? '—'} />
    {doc.sellerNiu && <Field label="NIU émetteur" value={doc.sellerNiu} />}
    <Divider />
    <Field label="Catégorie client" value={doc.customerCategory ?? '—'} />
    {doc.customerNiu && <Field label="NIU client" value={doc.customerNiu} />}
  </FiscalInfoCard>
)}
```

### Appel API — validation SEFC

```typescript
async function validateDocument(documentNumber: string): Promise<BillingDocumentResponse> {
  const res = await fetch(
    `/sni/api/v1/billing/documents/${documentNumber}/validate`,
    {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${token}` },
    }
  );
  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.message);
  }
  return res.json();
}
```

### Confirmation avant validation

> La validation est **irréversible**. Afficher une modale de confirmation :

```
⚠️ Valider ce document SEFC ?

Cette action est définitive. Une fois validé :
• Le numéro fiscal sera assigné et ne pourra plus changer
• Le document sera verrouillé et ne pourra plus être modifié
• Un avoir (avoir annulateur) devra être créé pour toute correction

Numéro actuel : INV-MEM-20260617-000001
Type : Facture

[Annuler]  [Confirmer la validation]
```

---

## Codes d'erreur billing

| `errorCode` | `status` | Cause |
|------------|---------|-------|
| `RESOURCE_NOT_FOUND` | 404 | Document introuvable |
| `BAD_REQUEST` | 400 | Document déjà validé (locked) |
| `BAD_REQUEST` | 400 | Statut incompatible pour la transition |
| `BAD_REQUEST` | 400 | Type de document non fiscal |
| `CONFLICT` | 409 | Conflit sur une ressource liée |

---

---

## Phase 3 — Contrôle d'intégrité SEFC

### `GET /admin/fiscal-integrity/check`

**Rôle requis :** `ADMIN` ou `SUPER_ADMIN`  
**Description :** Lance un audit immédiat de la chaîne fiscale et persiste le résultat (déclenché `"MANUAL"`).

Contrôles effectués :
1. **Numérotation continue** — absence de trous dans la séquence annuelle
2. **Chaînage SHA-256** — chaque `current_hash` est recalculé et comparé
3. **Signatures HMAC-SHA256** — chaque signature est vérifiée avec la clé `BILLING_FISCAL_SIGNING_KEY`

**Réponse `200`**
```json
{
  "valid": true,
  "checkedInvoices": 47,
  "brokenChains": 0,
  "missingSignatures": 0,
  "numberingGaps": 0,
  "checkedAt": "2026-06-17T14:30:00.000Z"
}
```

**Cas d'alerte :** Si `valid: false`, investiguer immédiatement :
- `brokenChains > 0` → altération de contenu ou de hash (fraude potentielle)
- `missingSignatures > 0` → document signé avec une clé différente ou non signé
- `numberingGaps > 0` → trous de numérotation (documents supprimés ou mal séquencés)

---

## Phase 4 — Audit nocturne automatique

### Tâche planifiée `FiscalIntegrityWorker`

Lance automatiquement le contrôle d'intégrité chaque nuit à **02h00** et persiste le résultat dans `fiscal_integrity_report` (déclenché `"SCHEDULER"`).

**Configuration** (variable d'environnement ou `application.yml`) :
```
BILLING_FISCAL_INTEGRITY_CRON=0 0 2 * * *
```

Format Spring cron : `seconde minute heure jour mois jour-semaine`

---

### `GET /admin/fiscal-integrity/last-report`

**Rôle requis :** `ADMIN` ou `SUPER_ADMIN`  
**Description :** Retourne le dernier rapport d'intégrité enregistré (nocturne ou manuel) sans relancer le calcul.

**Réponse `200`**
```json
{
  "id": 1234567890,
  "valid": true,
  "checkedInvoices": 47,
  "brokenChains": 0,
  "missingSignatures": 0,
  "numberingGaps": 0,
  "checkedAt": "2026-06-17T02:00:12.000Z",
  "triggeredBy": "SCHEDULER",
  "createdAt": "2026-06-17T02:00:13.000Z"
}
```

**Réponse `204 No Content`** si aucun rapport n'a encore été généré.

**Intégration frontend admin**

```typescript
// Charger le dernier rapport sans bloquer l'UI
async function loadLastIntegrityReport() {
  const res = await fetch('/sni/api/v1/admin/fiscal-integrity/last-report', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (res.status === 204) return null;
  return res.json();
}

// Lancer un audit manuel
async function runManualCheck() {
  const res = await fetch('/sni/api/v1/admin/fiscal-integrity/check', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}

// Widget dashboard
function FiscalIntegrityWidget({ report }) {
  if (!report) return <p>Aucun rapport disponible.</p>;
  return (
    <Card>
      <Badge color={report.valid ? 'green' : 'red'}>
        {report.valid ? 'Intégrité OK' : 'Anomalies détectées'}
      </Badge>
      <p>{report.checkedInvoices} factures vérifiées</p>
      <p>Dernière vérification : {formatDatetime(report.checkedAt)}</p>
      <p>Déclenchée par : {report.triggeredBy}</p>
      {!report.valid && (
        <ul>
          {report.brokenChains > 0 && <li>🔴 {report.brokenChains} chaînages brisés</li>}
          {report.missingSignatures > 0 && <li>🔴 {report.missingSignatures} signatures manquantes</li>}
          {report.numberingGaps > 0 && <li>🟡 {report.numberingGaps} trous de numérotation</li>}
        </ul>
      )}
    </Card>
  );
}
```

---

## Variables d'environnement à configurer (ops)

| Variable | Exemple | Description |
|----------|---------|-------------|
| `COMPANY_NIU` | `M2023000000000` | NIU officiel de l'entreprise |
| `COMPANY_FISCAL_ID` | `ELAOSE` | Code court fiscal (6 chars max) |
| `COMPANY_MACHINE_CODE` | `CBFOS` | Code caisse DGID (5 chars) |
| `BILLING_FISCAL_SIGNING_KEY` | *(générer avec `openssl rand -hex 32`)* | Clé HMAC Phase 2 |
