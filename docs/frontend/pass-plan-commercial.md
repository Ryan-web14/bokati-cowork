# Module Pass Plan & Propositions Commerciales — Guide Frontend

Ce document couvre les deux nouveaux modules ajoutés au backend :
1. **Pass Plan System** — catalogue de plans de passes, achat, facturation, activation et renouvellement automatique
2. **Module Commercial CRM** — propositions commerciales structurées avec lignes de service, cycle de vie et conversion

> Préfixe API commun : `ApiPath.V1` = `/sni/api/v1`  
> Auth : Bearer JWT requis sur tous les endpoints.  
> Content-Type : `application/json`

---

## Table des matières

### PARTIE 1 — Pass Plan System

1. [Modèle de données](#1-modèle-de-données)
2. [Cycle de vie du pass](#2-cycle-de-vie-du-pass)
3. [Catalogue de plans](#3-catalogue-de-plans--pass-plans)
4. [Versions de plan](#4-versions-de-plan)
5. [Prix](#5-prix)
6. [Entitlements de plan](#6-entitlements-de-plan)
7. [Achat d'un pass](#7-achat-dun-pass--purchase)
8. [Gestion des passes](#8-gestion-des-passes--passes)
9. [Flux d'activation par paiement](#9-flux-dactivation-par-paiement)
10. [Renouvellement automatique](#10-renouvellement-automatique)

### PARTIE 2 — Propositions Commerciales CRM

11. [Modèle de données — Propositions](#11-modèle-de-données--propositions)
12. [Cycle de vie d'une proposition](#12-cycle-de-vie-dune-proposition)
13. [Endpoints — Propositions](#13-endpoints--propositions)
14. [Gestion des lignes](#14-gestion-des-lignes)
15. [Calcul des montants](#15-calcul-des-montants)
16. [Workflow complet](#16-workflow-complet)

### Annexes

17. [Autorités requises](#17-autorités-requises)
18. [Enums de référence](#18-enums-de-référence)

---

# PARTIE 1 — Pass Plan System

## 1. Modèle de données

### 1.1 Hiérarchie

```
PassPlan (catalogue)
  └── PassPlanVersion (version tarifée + durée)
        ├── PassPlanPrice (prix par devise)
        └── PassPlanEntitlement (droits accordés)

Pass (instance achetée)
  ├── BillableItem → Invoice → PaymentIntent → Paiement
  ├── ContractPDF (généré async)
  ├── EntitlementGrant (droits actifs)
  ├── PassEvent + PassStatusHistory (audit)
  └── PassRenewalSchedule (si autoRenew)
```

### 1.2 PassPlanResponse

```json
{
  "id": 1,
  "code": "PPL-00001",
  "name": "Day Pass Flex",
  "description": "Accès journalier flexible",
  "passType": "DAY_PASS",
  "targetAudience": "INDIVIDUAL",
  "status": "ACTIVE",
  "visible": true,
  "sortOrder": 1,
  "requiredKycLevel": 1,
  "createdAt": "2026-06-20T10:00:00Z",
  "updatedAt": "2026-06-20T10:00:00Z"
}
```

### 1.3 PassPlanVersionResponse

```json
{
  "id": 10,
  "planCode": "PPL-00001",
  "versionNumber": 1,
  "name": "v1 — 1 jour, 5 000 XAF",
  "description": "Première version tarifaire",
  "status": "ACTIVE",
  "duration": 1,
  "durationUnit": "DAY",
  "maxUses": null,
  "autoRenewable": false,
  "requiredKycLevel": 1,
  "effectiveFrom": "2026-06-01",
  "effectiveTo": null,
  "createdAt": "2026-06-20T10:00:00Z",
  "updatedAt": "2026-06-20T10:00:00Z"
}
```

### 1.4 PassResponse (étendu)

```json
{
  "passNumber": "PASS-000001",
  "passType": "DAY_PASS",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-00045",
  "subscriptionNumber": null,
  "status": "ACTIVE",
  "name": "Day Pass Flex v1",
  "description": "...",
  "validFrom": "2026-06-20T08:00:00Z",
  "validUntil": "2026-06-21T08:00:00Z",
  "transferable": false,
  "shareable": false,
  "maxUses": null,
  "usedCount": 0,
  "contractCode": "CTR-2026-00123",
  "planCode": "PPL-00001",
  "planName": "Day Pass Flex",
  "planVersion": 1,
  "currency": "XAF",
  "subtotalAmount": 4201.6807,
  "taxAmount": 798.3193,
  "totalAmount": 5000.0000,
  "autoRenew": false,
  "nextRenewalDate": null,
  "renewalCount": 0,
  "entitlements": [],
  "createdAt": "2026-06-20T08:00:00Z",
  "updatedAt": "2026-06-20T08:00:00Z"
}
```

Les champs `planCode`, `planName`, `planVersion`, `currency`, `*Amount`, `autoRenew`, `nextRenewalDate`,
`renewalCount`, `createdAt`, `updatedAt` sont nouveaux. Pour les passes ad-hoc (anciennes), `planCode` /
`planName` / `planVersion` seront `null`.

---

## 2. Cycle de vie du pass

```
              ┌──────── paiement réussi ────────┐
              │                                  v
DRAFT ──> PENDING_ACTIVATION ──────────────> ACTIVE ──────> EXPIRED
              │                                  │
              │                                  ├──> PARTIALLY_USED ──> CONSUMED
              │                                  │
              │                                  ├──> PAST_DUE (échec renouvellement)
              │                                  │
              └── expiration ──> EXPIRED         └──> CANCELLED
```

| Statut | Description |
|--------|-------------|
| `DRAFT` | Brouillon (création ad-hoc uniquement) |
| `PENDING_ACTIVATION` | Pass créé, en attente de paiement |
| `ACTIVE` | Pass actif, utilisable |
| `PARTIALLY_USED` | Usages entamés (si `maxUses` défini) |
| `CONSUMED` | Tous les usages consommés |
| `PAST_DUE` | Renouvellement échoué |
| `EXPIRED` | Date de validité dépassée |
| `CANCELLED` | Annulé manuellement ou par remboursement |
| `SUSPENDED` | Suspendu administrativement |

---

## 3. Catalogue de plans — `/pass-plans`

### 3.1 Référence des endpoints

| Méthode | Chemin | Autorité | Description |
|---------|--------|----------|-------------|
| POST | `/pass-plans` | `PASS_PLAN:WRITE` | Créer un plan |
| GET | `/pass-plans` | `PASS_PLAN:READ` | Rechercher les plans |
| GET | `/pass-plans/{planCode}` | `PASS_PLAN:READ` | Détail d'un plan |
| PATCH | `/pass-plans/{planCode}` | `PASS_PLAN:WRITE` | Modifier un plan |
| PATCH | `/pass-plans/{planCode}/publish` | `PASS_PLAN:WRITE` | Publier (DRAFT → ACTIVE) |
| PATCH | `/pass-plans/{planCode}/archive` | `PASS_PLAN:WRITE` | Archiver (→ ARCHIVED) |

### 3.2 Création d'un plan

**`POST /pass-plans`**

```json
{
  "name": "Day Pass Flex",
  "description": "Accès journalier flexible à l'espace coworking",
  "passType": "DAY_PASS",
  "targetAudience": "INDIVIDUAL",
  "visible": true,
  "sortOrder": 1,
  "requiredKycLevel": 1
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `name` | String | **Oui** | Nom du plan |
| `description` | String | Non | Description longue |
| `passType` | PassType | **Oui** | Type de pass (voir [Enums](#18-enums-de-référence)) |
| `targetAudience` | TargetAudience | **Oui** | `INDIVIDUAL`, `COMPANY`, `BOTH` |
| `visible` | Boolean | Non | Visible dans le catalogue client (défaut `false`) |
| `sortOrder` | Integer | Non | Ordre d'affichage |
| `requiredKycLevel` | Integer | Non | Niveau KYC requis (défaut `1`) |

### 3.3 Recherche de plans

**`GET /pass-plans`**

| Paramètre | Type | Description |
|-----------|------|-------------|
| `passType` | PassType | Filtrer par type |
| `status` | PlanStatus | Filtrer par statut (`DRAFT`, `ACTIVE`, `ARCHIVED`) |
| `searchText` | String | Recherche texte dans nom/description |
| `page`, `size`, `sort` | Pagination | Défaut : `size=20, sort=createdAt,desc` |

### 3.4 Mise à jour

**`PATCH /pass-plans/{planCode}`**

```json
{
  "name": "Day Pass Premium",
  "visible": true,
  "sortOrder": 2
}
```

Tous les champs sont optionnels — seuls les champs fournis sont mis à jour.

---

## 4. Versions de plan

| Méthode | Chemin | Autorité | Description |
|---------|--------|----------|-------------|
| POST | `/pass-plans/{planCode}/versions` | `PASS_PLAN:WRITE` | Créer une version |
| GET | `/pass-plans/{planCode}/versions` | `PASS_PLAN:READ` | Lister les versions |
| PATCH | `/pass-plans/{planCode}/versions/{id}/publish` | `PASS_PLAN:WRITE` | Publier la version |

### 4.1 Création d'une version

**`POST /pass-plans/{planCode}/versions`**

```json
{
  "name": "v1 — 1 jour, 5 000 XAF",
  "description": "Première version tarifaire",
  "duration": 1,
  "durationUnit": "DAY",
  "maxUses": null,
  "autoRenewable": false,
  "requiredKycLevel": 1,
  "effectiveFrom": "2026-07-01",
  "effectiveTo": null,
  "termsJson": "{\"cancellation\": \"24h avant\"}"
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `name` | String | **Oui** | Nom de la version |
| `duration` | Integer | **Oui** | Nombre d'unités de durée (ex. `1`, `30`, `90`) |
| `durationUnit` | PassDurationUnit | **Oui** | `DAY`, `WEEK`, `MONTH`, `YEAR` |
| `maxUses` | Integer | Non | Nombre max d'utilisations (`null` = illimité) |
| `autoRenewable` | Boolean | Non | Ce plan supporte-t-il le renouvellement auto ? |
| `requiredKycLevel` | Integer | Non | Niveau KYC requis pour cette version |
| `effectiveFrom` | Date | Non | Date de début d'applicabilité |
| `effectiveTo` | Date | Non | Date de fin d'applicabilité |
| `termsJson` | String (JSON) | Non | Conditions particulières (format libre) |

> Le `versionNumber` est auto-incrémenté par le backend.  
> Une version est créée en statut `DRAFT`. Utiliser `/publish` pour l'activer.

---

## 5. Prix

| Méthode | Chemin | Autorité | Description |
|---------|--------|----------|-------------|
| POST | `/pass-plans/versions/{versionId}/prices` | `PASS_PLAN:WRITE` | Définir/mettre à jour un prix |
| GET | `/pass-plans/versions/{versionId}/prices` | `PASS_PLAN:READ` | Lister les prix |

### 5.1 Définir un prix

**`POST /pass-plans/versions/{versionId}/prices`**

```json
{
  "currency": "XAF",
  "amount": 5000,
  "setupFee": 0,
  "depositAmount": 0,
  "taxIncluded": true,
  "taxCode": null
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `currency` | String(3) | **Oui** | Code devise ISO (ex. `XAF`, `EUR`) |
| `amount` | BigDecimal | **Oui** | Prix de base |
| `setupFee` | BigDecimal | Non | Frais d'installation (défaut `0`) |
| `depositAmount` | BigDecimal | Non | Caution / dépôt (défaut `0`) |
| `taxIncluded` | Boolean | Non | Le prix inclut-il la TVA ? (défaut `true`) |
| `taxCode` | String | Non | Code fiscal spécifique |

> Un seul prix par devise par version. Si la devise existe déjà, le prix est mis à jour.

---

## 6. Entitlements de plan

| Méthode | Chemin | Autorité | Description |
|---------|--------|----------|-------------|
| POST | `/pass-plans/versions/{versionId}/entitlements` | `PASS_PLAN:WRITE` | Ajouter un entitlement |
| GET | `/pass-plans/versions/{versionId}/entitlements` | `PASS_PLAN:READ` | Lister les entitlements |
| DELETE | `/pass-plans/versions/{versionId}/entitlements/{id}` | `PASS_PLAN:WRITE` | Supprimer |

### 6.1 Ajouter un entitlement

**`POST /pass-plans/versions/{versionId}/entitlements`**

```json
{
  "entitlementCode": "COWORK_ACCESS",
  "quantity": 8,
  "unlimited": false,
  "rolloverAllowed": false,
  "rolloverLimit": null,
  "validForDays": null,
  "priority": 100
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `entitlementCode` | String | **Oui** | Code de la définition d'entitlement |
| `quantity` | BigDecimal | Conditionnel | Quantité (requis si `unlimited=false`) |
| `unlimited` | Boolean | Non | Accès illimité (défaut `false`) |
| `rolloverAllowed` | Boolean | Non | Report de solde au renouvellement |
| `rolloverLimit` | BigDecimal | Non | Limite de report |
| `validForDays` | Integer | Non | Durée de validité en jours (`null` = durée du pass) |
| `priority` | Integer | Non | Priorité de consommation (défaut `100`) |

---

## 7. Achat d'un pass — Purchase

### 7.1 Endpoint

**`POST /pass-plans/{planCode}/purchase`** — Autorité : `PASS:WRITE`

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-00045",
  "planVersionId": null,
  "currency": "XAF",
  "validFrom": null,
  "autoRenew": false,
  "metadataJson": null
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `ownerType` | SubscriberType | **Oui** | `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |
| `ownerCode` | String | **Oui** | Code du titulaire |
| `planVersionId` | Long | Non | Version spécifique (`null` = dernière version active) |
| `currency` | String | Non | Devise du paiement (`null` = `XAF`) |
| `validFrom` | String (ISO) | Non | Début de validité (`null` = maintenant) |
| `autoRenew` | Boolean | Non | Renouvellement automatique (ignoré si la version ne le supporte pas) |
| `metadataJson` | String | Non | Métadonnées JSON libres |

### 7.2 Réponse

Retourne un `PassResponse` complet (voir section 1.4).

### 7.3 Comportement

- **Pass payant** (`totalAmount > 0`) :
  1. Pass créé en statut `PENDING_ACTIVATION`
  2. `BillableItem` + `Invoice` + `PaymentIntent` créés automatiquement
  3. Contrat PDF généré en asynchrone
  4. Email de création envoyé
  5. → **Le pass s'active automatiquement après paiement** (voir section 9)

- **Pass gratuit** (`totalAmount = 0`) :
  1. Pass créé et activé immédiatement (`ACTIVE`)
  2. Entitlements accordés
  3. Email d'activation envoyé

### 7.4 Flux UI recommandé

```
[Catalogue Plans] → Sélectionner un plan → [Formulaire d'achat]
  → POST /pass-plans/{planCode}/purchase
  → Si PENDING_ACTIVATION → Rediriger vers le paiement (invoice / PaymentIntent)
  → Si ACTIVE → Afficher confirmation
```

---

## 8. Gestion des passes — `/passes`

### 8.1 Endpoints existants (inchangés)

| Méthode | Chemin | Description |
|---------|--------|-------------|
| POST | `/passes` | Créer un pass ad-hoc (ancien flux, backward compat) |
| GET | `/passes/{passNumber}` | Détail d'un pass |
| GET | `/passes` | Recherche paginée |
| PATCH | `/passes/{passNumber}/cancel` | Annuler un pass |

### 8.2 Nouveaux endpoints

| Méthode | Chemin | Description |
|---------|--------|-------------|
| POST | `/passes/{passNumber}/activate` | Activation manuelle (admin) |
| GET | `/passes/{passNumber}/events` | Historique des événements |
| GET | `/passes/{passNumber}/history` | Historique des changements de statut |
| GET | `/passes/{passNumber}/renewal` | Détail du schedule de renouvellement |

### 8.3 Activation manuelle

**`POST /passes/{passNumber}/activate`**

Aucun body requis. Active un pass en statut `PENDING_ACTIVATION` sans attendre le paiement.

### 8.4 Événements

**`GET /passes/{passNumber}/events`**

```json
[
  {
    "id": 1,
    "eventType": "PASS_CREATED",
    "payloadJson": null,
    "occurredAt": "2026-06-20T08:00:00Z"
  },
  {
    "id": 2,
    "eventType": "PASS_ACTIVATED",
    "payloadJson": null,
    "occurredAt": "2026-06-20T08:05:00Z"
  }
]
```

Types d'événements : `PASS_CREATED`, `PASS_ACTIVATED`, `PASS_RENEWED`, `PASS_SUSPENDED`,
`PASS_CANCELLED`, `PASS_EXPIRED`, `PASS_PAST_DUE`, `ENTITLEMENTS_GRANTED`,
`BILLING_SCHEDULED`, `RENEWAL_SCHEDULED`

### 8.5 Historique des statuts

**`GET /passes/{passNumber}/history`**

```json
[
  {
    "id": 1,
    "fromStatus": null,
    "toStatus": "PENDING_ACTIVATION",
    "reason": "Création",
    "changedBy": "SYSTEM",
    "changedAt": "2026-06-20T08:00:00Z"
  },
  {
    "id": 2,
    "fromStatus": "PENDING_ACTIVATION",
    "toStatus": "ACTIVE",
    "reason": "Activation après paiement TXN-2026-000456",
    "changedBy": "SYSTEM",
    "changedAt": "2026-06-20T08:05:00Z"
  }
]
```

### 8.6 Schedule de renouvellement

**`GET /passes/{passNumber}/renewal`**

Retourne `null` si le pass n'a pas de renouvellement automatique.

```json
{
  "id": 1,
  "duration": 30,
  "durationUnit": "DAY",
  "nextRenewalDate": "2026-07-20T08:00:00Z",
  "currentPeriodStart": "2026-06-20T08:00:00Z",
  "currentPeriodEnd": "2026-07-20T08:00:00Z",
  "status": "ACTIVE",
  "retryCount": 0,
  "lastAttemptAt": null,
  "createdAt": "2026-06-20T08:05:00Z",
  "updatedAt": "2026-06-20T08:05:00Z"
}
```

### 8.7 Filtres de recherche

**`GET /passes`**

| Paramètre | Type | Description |
|-----------|------|-------------|
| `ownerType` | SubscriberType | Filtrer par type de titulaire |
| `ownerCode` | String | Filtrer par code titulaire |
| `passType` | PassType | Filtrer par type de pass |
| `status` | PassStatus | Filtrer par statut |
| `expiringBefore` | Instant | Passes expirant avant cette date |
| `page`, `size`, `sort` | Pagination | Défaut : `size=20, sort=createdAt,desc` |

---

## 9. Flux d'activation par paiement

```
Frontend                          Backend
   │                                │
   │  POST /pass-plans/{code}/purchase
   │ ──────────────────────────────>│
   │                                │  Crée Pass (PENDING_ACTIVATION)
   │                                │  Crée BillableItem → Invoice → PaymentIntent
   │  <── PassResponse (status=PENDING_ACTIVATION)
   │                                │
   │  [Écran de paiement]          │
   │  POST /payments/...           │
   │ ──────────────────────────────>│
   │                                │  PaymentTransaction → SUCCEEDED
   │                                │  PaymentTransactionWorkflowProcessor
   │                                │    → source.type = "PASS"
   │                                │    → passService.activate()
   │                                │      ├─ PENDING_ACTIVATION → ACTIVE
   │                                │      ├─ Grant entitlements
   │                                │      ├─ Upsert renewal schedule
   │                                │      └─ Email "pass activé"
   │                                │
   │  GET /passes/{passNumber}     │
   │ ──────────────────────────────>│
   │  <── PassResponse (status=ACTIVE)
```

> **Important :** L'activation est automatique après paiement. Le frontend n'a pas besoin d'appeler
> `/activate` dans ce flux — il suffit de poller `GET /passes/{passNumber}` ou d'écouter un webhook
> pour détecter le changement de statut.

---

## 10. Renouvellement automatique

Un worker (`PassRenewalWorker`) tourne toutes les **15 minutes** et traite les passes dont
`nextRenewalDate` est passée.

### Flux automatique

1. Le worker détecte un `PassRenewalSchedule` dû
2. Calcule la nouvelle période (décalage de `duration` depuis `validUntil`)
3. Met à jour le pass : `validFrom`, `validUntil`, `nextRenewalDate`, `renewalCount++`, `usedCount=0`
4. Crée un `BillableItem` avec `sourceType=PASS_RENEWAL` → Invoice → PaymentIntent
5. Après paiement, le `PaymentTransactionWorkflowProcessor` détecte `PASS_RENEWAL` et re-accorde les entitlements

### Annulation du renouvellement

Pour désactiver le renouvellement automatique, annuler le pass via `PATCH /passes/{passNumber}/cancel`.

---

# PARTIE 2 — Propositions Commerciales CRM

## 11. Modèle de données — Propositions

### 11.1 ProposalResponse

```json
{
  "id": 1,
  "proposalNumber": "PROP-00001",
  "title": "Proposition aménagement espace équipe Tech",
  "opportunityId": 5,
  "leadId": null,
  "recipientType": "CUSTOMER",
  "recipientCode": "CLI-00012",
  "recipientName": "Société ABC",
  "recipientEmail": "contact@abc.cg",
  "status": "DRAFT",
  "validUntil": "2026-07-15",
  "subtotalAmount": 450000.0000,
  "taxAmount": 85500.0000,
  "totalAmount": 535500.0000,
  "currency": "XAF",
  "notes": "Remise fidélité appliquée",
  "internalNotes": "Marge de négociation : 10%",
  "convertedInvoiceNumber": null,
  "convertedContractCode": null,
  "convertedSubscriptionNumber": null,
  "convertedPassNumber": null,
  "sentAt": null,
  "viewedAt": null,
  "acceptedAt": null,
  "rejectedAt": null,
  "rejectionReason": null,
  "createdBy": 12,
  "assignedTo": 12,
  "createdAt": "2026-06-20T10:00:00Z",
  "updatedAt": "2026-06-20T10:30:00Z",
  "lines": [
    {
      "id": 1,
      "sortOrder": 0,
      "serviceType": "SUBSCRIPTION",
      "serviceRefCode": "PLAN-PREMIUM",
      "label": "Abonnement Premium — 6 mois",
      "description": "Poste fixe + accès salle de réunion",
      "quantity": 6.00,
      "unitPrice": 75000.0000,
      "discountPercent": 0.00,
      "taxRate": 0.1900,
      "taxIncluded": false,
      "subtotalAmount": 450000.0000,
      "taxAmount": 85500.0000,
      "totalAmount": 535500.0000,
      "currency": "XAF"
    }
  ]
}
```

### 11.2 ProposalLineResponse

| Champ | Type | Description |
|-------|------|-------------|
| `id` | Long | ID de la ligne |
| `sortOrder` | Integer | Ordre d'affichage |
| `serviceType` | String | Type de service (`SUBSCRIPTION`, `PASS`, `BOOKING`, `CUSTOM`) |
| `serviceRefCode` | String | Code référence du service (optionnel) |
| `label` | String | Libellé de la ligne |
| `description` | String | Description détaillée |
| `quantity` | BigDecimal | Quantité |
| `unitPrice` | BigDecimal | Prix unitaire |
| `discountPercent` | BigDecimal | % de remise |
| `taxRate` | BigDecimal | Taux de TVA (ex. `0.1900` = 19%) |
| `taxIncluded` | Boolean | Le prix unitaire inclut-il la TVA ? |
| `subtotalAmount` | BigDecimal | Sous-total HT calculé |
| `taxAmount` | BigDecimal | Montant TVA calculé |
| `totalAmount` | BigDecimal | Total TTC calculé |

---

## 12. Cycle de vie d'une proposition

```
DRAFT ──send──> SENT ──view──> VIEWED
                  │                │
                  └──accept──> ACCEPTED ──convert──> CONVERTED
                  │                │
                  └──reject──> REJECTED
                  │
                  └──expire──> EXPIRED
```

| Statut | Description |
|--------|-------------|
| `DRAFT` | Brouillon, modifiable |
| `SENT` | Envoyée au client |
| `VIEWED` | Consultée par le client |
| `ACCEPTED` | Acceptée par le client |
| `REJECTED` | Refusée (avec raison) |
| `EXPIRED` | Validité dépassée |
| `CONVERTED` | Convertie en facture/souscription/pass |

---

## 13. Endpoints — Propositions

Base : `/crm/proposals`

| Méthode | Chemin | Autorité | Description |
|---------|--------|----------|-------------|
| POST | `/crm/proposals` | `CRM:WRITE` | Créer une proposition |
| GET | `/crm/proposals` | `CRM:READ` | Lister/rechercher |
| GET | `/crm/proposals/{id}` | `CRM:READ` | Détail par ID |
| GET | `/crm/proposals/number/{proposalNumber}` | `CRM:READ` | Détail par numéro |
| PUT | `/crm/proposals/{id}` | `CRM:WRITE` | Modifier |
| PATCH | `/crm/proposals/{id}/send` | `CRM:WRITE` | Envoyer |
| PATCH | `/crm/proposals/{id}/accept` | `CRM:WRITE` | Accepter |
| PATCH | `/crm/proposals/{id}/reject` | `CRM:WRITE` | Rejeter |

### 13.1 Création

**`POST /crm/proposals`**

```json
{
  "title": "Proposition aménagement espace équipe Tech",
  "opportunityId": 5,
  "leadId": null,
  "recipientType": "CUSTOMER",
  "recipientCode": "CLI-00012",
  "recipientName": "Société ABC",
  "recipientEmail": "contact@abc.cg",
  "validUntil": "2026-07-15",
  "currency": "XAF",
  "notes": "Remise fidélité appliquée",
  "internalNotes": "Marge de négociation : 10%",
  "assignedTo": 12
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `title` | String | **Oui** | Titre de la proposition |
| `opportunityId` | Long | Non | Lier à une opportunité CRM |
| `leadId` | Long | Non | Lier à un lead CRM |
| `recipientType` | String | Non | Type de destinataire |
| `recipientCode` | String | Non | Code du destinataire |
| `recipientName` | String | Non | Nom affiché |
| `recipientEmail` | String | Non | Email pour l'envoi |
| `validUntil` | Date | Non | Date d'expiration de la proposition |
| `currency` | String | Non | Devise |
| `notes` | String | Non | Notes visibles par le client |
| `internalNotes` | String | Non | Notes internes (non visibles) |
| `assignedTo` | Long | Non | ID de l'agent assigné |

### 13.2 Recherche

**`GET /crm/proposals`**

| Paramètre | Type | Description |
|-----------|------|-------------|
| `status` | ProposalStatus | Filtrer par statut |
| `opportunityId` | Long | Filtrer par opportunité |
| `searchText` | String | Recherche dans titre, numéro, nom destinataire |
| `page`, `size`, `sort` | Pagination | Défaut : `size=20, sort=createdAt,desc` |

### 13.3 Envoi

**`PATCH /crm/proposals/{id}/send`** — Aucun body requis.

Transition `DRAFT → SENT`. Enregistre `sentAt`.

### 13.4 Acceptation

**`PATCH /crm/proposals/{id}/accept`** — Aucun body requis.

Transition `SENT` ou `VIEWED` → `ACCEPTED`. Enregistre `acceptedAt`.

### 13.5 Rejet

**`PATCH /crm/proposals/{id}/reject`**

```json
{
  "reason": "Budget insuffisant pour ce trimestre"
}
```

Transition `SENT` ou `VIEWED` → `REJECTED`. Enregistre `rejectedAt` et `rejectionReason`.

---

## 14. Gestion des lignes

| Méthode | Chemin | Autorité | Description |
|---------|--------|----------|-------------|
| POST | `/crm/proposals/{id}/lines` | `CRM:WRITE` | Ajouter une ligne |
| PUT | `/crm/proposals/{id}/lines/{lineId}` | `CRM:WRITE` | Modifier une ligne |
| DELETE | `/crm/proposals/{id}/lines/{lineId}` | `CRM:WRITE` | Supprimer une ligne |

### 14.1 Ajouter une ligne

**`POST /crm/proposals/{id}/lines`**

```json
{
  "label": "Abonnement Premium — 6 mois",
  "description": "Poste fixe + accès salle de réunion",
  "serviceType": "SUBSCRIPTION",
  "serviceRefCode": "PLAN-PREMIUM",
  "quantity": 6,
  "unitPrice": 75000,
  "discountPercent": 0,
  "taxRate": 0.19,
  "taxIncluded": false,
  "sortOrder": 0,
  "currency": "XAF"
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `label` | String | **Oui** | Libellé de la prestation |
| `unitPrice` | BigDecimal | **Oui** | Prix unitaire |
| `quantity` | BigDecimal | Non | Quantité (défaut `1`) |
| `discountPercent` | BigDecimal | Non | Remise en % (défaut `0`) |
| `taxRate` | BigDecimal | Non | Taux TVA en décimal (défaut `0`) |
| `taxIncluded` | Boolean | Non | Prix TTC ? (défaut `false`) |
| `serviceType` | String | Non | Type de service lié |
| `serviceRefCode` | String | Non | Code de référence du service |
| `sortOrder` | Integer | Non | Ordre d'affichage |
| `currency` | String | Non | Devise de la ligne |

> Les montants `subtotalAmount`, `taxAmount`, `totalAmount` de la ligne sont **calculés automatiquement**
> par le backend. Les totaux de la proposition sont recalculés à chaque ajout/modification/suppression
> de ligne.

### 14.2 Modifier une ligne

**`PUT /crm/proposals/{id}/lines/{lineId}`**

```json
{
  "quantity": 12,
  "unitPrice": 70000,
  "discountPercent": 5
}
```

Tous les champs sont optionnels — seuls les champs fournis sont mis à jour.

### 14.3 Supprimer une ligne

**`DELETE /crm/proposals/{id}/lines/{lineId}`**

Retourne la `ProposalResponse` avec les totaux recalculés.

---

## 15. Calcul des montants

### 15.1 Par ligne

```
rawSubtotal    = quantity × unitPrice
discountAmount = rawSubtotal × discountPercent ÷ 100
afterDiscount  = rawSubtotal − discountAmount

Si taxIncluded = true :
  factor       = 1 + taxRate
  subtotal     = afterDiscount ÷ factor
  tax          = afterDiscount − subtotal
  total        = afterDiscount

Si taxIncluded = false :
  subtotal     = afterDiscount
  tax          = afterDiscount × taxRate
  total        = subtotal + tax
```

### 15.2 Par proposition

```
proposal.subtotalAmount = Σ ligne.subtotalAmount
proposal.taxAmount      = Σ ligne.taxAmount
proposal.totalAmount    = Σ ligne.totalAmount
```

---

## 16. Workflow complet

### Scénario type : proposition → acceptation → facturation

```
1. POST /crm/proposals                          → DRAFT
2. POST /crm/proposals/{id}/lines               → Ajouter des lignes
3. POST /crm/proposals/{id}/lines               → Ajouter d'autres lignes
4. PATCH /crm/proposals/{id}/send               → SENT (email au client)
5. [Client consulte]                            → VIEWED (optionnel)
6. PATCH /crm/proposals/{id}/accept             → ACCEPTED
7. [Conversion] :
   - Créer manuellement une facture depuis les lignes
   - OU créer une souscription / pass avec les références de la proposition
```

> **Note :** Les conversions automatiques (`convertToInvoice`, `convertToSubscription`, `convertToPass`)
> sont préparées côté backend mais les endpoints de conversion ne sont pas encore exposés dans cette
> version. Utilisez le champ `convertedInvoiceNumber` / `convertedPassNumber` pour tracer les
> conversions manuelles.

---

## 17. Autorités requises

### Pass Plan System

| Autorité | Usage |
|----------|-------|
| `PASS_PLAN:READ` / `PASS_PLAN_READ` | Lecture catalogue de plans, versions, prix, entitlements |
| `PASS_PLAN:WRITE` / `PASS_PLAN_WRITE` | Création/modification/publication/archivage de plans |
| `PASS:WRITE` / `PASS_WRITE` | Achat de pass (purchase) |
| `PASS:READ` / `PASS_READ` | Lecture des passes (existant) |

### Module Commercial CRM

| Autorité | Usage |
|----------|-------|
| `CRM:READ` / `CRM_READ` | Lecture propositions commerciales |
| `CRM:WRITE` / `CRM_WRITE` | Création/modification/envoi/acceptation/rejet de propositions |

---

## 18. Enums de référence

### PassType

| Valeur | Description |
|--------|-------------|
| `DAY_PASS` | Pass journalier |
| `TIME_PACK` | Pack horaire |
| `VISITOR_PASS` | Pass visiteur |
| `MEETING_ROOM_PACK` | Pack salle de réunion |
| `PROMOTIONAL_PASS` | Pass promotionnel |
| `SUBSCRIPTION_PASS` | Pass lié à une souscription |
| `COMPANY_SHARED_PASS` | Pass partagé entreprise |
| `CUSTOM` | Pass personnalisé |

### PassDurationUnit

| Valeur | Description |
|--------|-------------|
| `DAY` | Jours |
| `WEEK` | Semaines |
| `MONTH` | Mois |
| `YEAR` | Années |

### PassStatus

| Valeur | Description |
|--------|-------------|
| `DRAFT` | Brouillon |
| `PENDING_ACTIVATION` | En attente de paiement |
| `ACTIVE` | Actif |
| `PARTIALLY_USED` | Partiellement utilisé |
| `CONSUMED` | Entièrement consommé |
| `PAST_DUE` | Renouvellement échoué |
| `EXPIRED` | Expiré |
| `CANCELLED` | Annulé |
| `SUSPENDED` | Suspendu |

### PlanStatus

| Valeur | Description |
|--------|-------------|
| `DRAFT` | Brouillon, non publiable |
| `ACTIVE` | Publié, disponible à l'achat |
| `ARCHIVED` | Archivé, plus disponible |

### TargetAudience

| Valeur | Description |
|--------|-------------|
| `INDIVIDUAL` | Particuliers |
| `COMPANY` | Entreprises |
| `BOTH` | Les deux |

### SubscriberType

| Valeur | Description |
|--------|-------------|
| `MEMBER` | Membre |
| `CUSTOMER` | Client |
| `BUSINESS_ENTITY` | Entité commerciale |

### ProposalStatus

| Valeur | Description |
|--------|-------------|
| `DRAFT` | Brouillon |
| `SENT` | Envoyée |
| `VIEWED` | Consultée |
| `ACCEPTED` | Acceptée |
| `REJECTED` | Refusée |
| `EXPIRED` | Expirée |
| `CONVERTED` | Convertie |

### PassEventType

| Valeur | Description |
|--------|-------------|
| `PASS_CREATED` | Pass créé |
| `PASS_ACTIVATED` | Pass activé |
| `PASS_RENEWED` | Pass renouvelé |
| `PASS_SUSPENDED` | Pass suspendu |
| `PASS_CANCELLED` | Pass annulé |
| `PASS_EXPIRED` | Pass expiré |
| `PASS_PAST_DUE` | Renouvellement échoué |
| `ENTITLEMENTS_GRANTED` | Droits accordés |
| `BILLING_SCHEDULED` | Facturation planifiée |
| `RENEWAL_SCHEDULED` | Renouvellement planifié |
