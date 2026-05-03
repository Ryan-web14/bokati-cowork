# Modules Existants — Guide d'intégration Frontend

Améliorations livrées sur les modules existants (roadmap 2.1 → 2.9 et 3.4).

Base URL : `/sni/api/v1`

---

## Sommaire

- [2.1 Booking](#21-booking)
- [2.2 Billing](#22-billing)
- [2.3 Payment](#23-payment)
- [2.4 Subscription](#24-subscription)
- [2.6 Document & Signature & KYC](#26-document--signature--kyc)
- [2.7 Notification](#27-notification)
- [2.8 Inventory](#28-inventory)
- [2.9 Analytics & Reporting](#29-analytics--reporting)

> **Non implémenté dans cette itération :** module Access Control (2.1), période d'essai TRIALING (2.4).

---

## 2.1 Booking

### Récurrence avancée

`POST /bookings/recurring`

Nouvelles options par rapport à la version précédente :

```json
{
  "booking": {
    "resourceCode": "SALLE-01",
    "identityLookup": { "memberId": "MBR-001" },
    "startedAt": "2026-05-05T09:00:00",
    "endedAt":   "2026-05-05T11:00:00",
    "quantity": 1,
    "paymentMode": "SUBSCRIPTION"
  },
  "frequency": "WEEKLY",
  "intervalValue": 1,
  "occurrences": 10,
  "endDate": "2026-07-31",
  "excludedDates": ["2026-05-19", "2026-06-02"]
}
```

| Champ | Description |
|-------|-------------|
| `frequency` | `DAILY`, `WEEKLY`, `MONTHLY` |
| `intervalValue` | Intervalle (ex : 2 = toutes les 2 semaines). Min : 1 |
| `occurrences` | Nombre max d'occurrences. Min : 2, Max : 60 |
| `endDate` | Coupe la récurrence à cette date (prioritaire sur `occurrences`) |
| `excludedDates` | Dates à sauter (jours fériés, congés) |

Retourne `List<BookingResponse>`.

---

### Réservation depuis le portail client

`POST /client/bookings`

Même payload que `POST /bookings`. Destiné au portail client autonome.

---

### Check-in QR

Chaque `BookingResponse` contient désormais :

```json
{
  "checkInToken": "a3f8b2c1d4e5f6a7b8c9d0e1f2a3b4c5",
  "checkInQrValue": "BOOKING_CHECK_IN:a3f8b2c1d4e5f6a7b8c9d0e1f2a3b4c5",
  "virtualMeetingUrl": "https://meet.jit.si/bokati-bkg-a3f8b2c1"
}
```

- `checkInQrValue` : valeur à encoder dans le QR Code affiché au client.
- `virtualMeetingUrl` : uniquement renseigné pour les ressources de type `VIRTUAL_ROOM`.

Scan QR côté staff :

`PATCH /bookings/check-in/qr/{checkInToken}`

```json
{
  "actor": "STAFF",
  "note": "Enregistrement portail",
  "sendEmail": false
}
```

---

### Liste d'attente (Waitlist)

`POST /bookings/waitlist`

```json
{
  "resourceCode": "BUREAU-02",
  "identityLookup": { "memberId": "MBR-042" },
  "startedAt": "2026-05-10T14:00:00",
  "endedAt":   "2026-05-10T17:00:00",
  "quantity": 1,
  "paymentMode": "DIRECT"
}
```

La réponse inclut un champ `alternatives` : la liste des créneaux disponibles proches suggérés par le système.

**UX recommandée :**
1. Afficher les `alternatives` si présentes.
2. Proposer "Réserver ce créneau" pour chaque alternative.
3. Proposer "Me mettre en liste d'attente" pour le créneau initial.

Autres endpoints :

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/bookings/waitlist` | Lister les entrées. Query : `status` (`WAITING`, `OFFERED`, `BOOKED`, `CANCELLED`, `EXPIRED`) |
| `PATCH` | `/bookings/waitlist/{id}/cancel` | Annuler une entrée |
| `POST` | `/bookings/waitlist/promote-available` | (admin) Promouvoir les offres disponibles. Query : `limit` (défaut : 100) |
| `POST` | `/bookings/waitlist/expire-offers` | (admin) Expirer les offres échues |

**`BookingWaitlistEntryResponse`**

```json
{
  "id": 1,
  "resourceCode": "BUREAU-02",
  "resourceName": "Bureau Privatif 02",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-042",
  "contactName": "Jean Client",
  "contactEmail": "jean@email.com",
  "contactPhone": "+242060000000",
  "startedAt": "2026-05-10T14:00:00",
  "endedAt":   "2026-05-10T17:00:00",
  "quantity": 1,
  "paymentMode": "DIRECT",
  "status": "WAITING",
  "offeredAt": null,
  "expiresAt": null,
  "createdAt": "2026-04-29T08:00:00Z",
  "alternatives": []
}
```

Le worker backend traite automatiquement la promotion des offres et leur expiration.

---

## 2.2 Billing

### Relances automatiques de paiement

Un worker backend identifie les factures `OVERDUE` et envoie des rappels email à J+1, J+7, J+15. Aucune action frontend requise.

Template email utilisé : `billing-reminder.html`.

---

### Relevé client CSV

`GET /billing/customers/{customerType}/{customerCode}/statement.csv`

Paramètres de pagination supportés (ex : `?size=500`).

Retourne un fichier `text/csv` téléchargeable. En-têtes :

```
documentNumber,type,status,issueDate,dueDate,totalAmount,paidAmount,balanceDue,currency
```

Exemple d'usage frontend :

```js
const url = `/sni/api/v1/billing/customers/MEMBER/MBR-001/statement.csv`;
const link = document.createElement('a');
link.href = url;
link.download = `releve-MBR-001.csv`;
link.click();
```

---

## 2.3 Payment

### Lien de paiement

**Créer un lien :**

`POST /payments/intents/{intentNumber}/link`

```json
{
  "expiresInMinutes": 1440
}
```

`expiresInMinutes` min : 5.

La réponse `PaymentIntentResponse` contient :

```json
{
  "paymentLinkToken": "eyJhbGci...",
  "paymentLinkExpiresAt": "2026-04-30T10:00:00Z"
}
```

**Résoudre un lien (sans authentification) :**

`GET /payment-links/{token}`

Retourne `PaymentIntentResponse`. Le front peut ensuite initier le paiement via les endpoints standards :

- `PATCH /payments/intents/{intentNumber}/cash`
- `PATCH /payments/intents/{intentNumber}/wallet`
- `PATCH /payments/intents/{intentNumber}/mobile-money`

---

### Paiement Mobile Money (PawaPay)

`PATCH /payments/intents/{intentNumber}/mobile-money`

```json
{
  "phoneNumber": "+242060000000",
  "correspondent": "MTN_MOMO_COG",
  "createdBy": "USR-001",
  "metadataJson": "{}"
}
```

**Correspondants disponibles (`CongoCorrespondent`) :**

| Valeur | Opérateur | Pays |
|--------|-----------|------|
| `MTN_MOMO_COG` | MTN Mobile Money | Congo-Brazzaville |
| `AIRTEL_OAPI_COG` | Airtel Money | Congo-Brazzaville |
| `ORANGE_COG` | Orange Money | Congo-Brazzaville |
| `AIRTEL_OAPI_COD` | Airtel Money | RDC |
| `ORANGE_COD` | Orange Money | RDC |
| `MPESA_COD` | M-Pesa | RDC |

Le paiement mobile money est asynchrone. Le backend reçoit les callbacks PawaPay (`/payments/mobile-money/pawaypay/callback`) et met à jour le statut de la transaction. Le front peut polling `GET /payments/intents/{intentNumber}` ou `GET /payments/intents/{intentNumber}/transactions` pour suivre l'état.

**Statuts `PaymentIntentStatus` :**
`PENDING`, `PROCESSING`, `AUTHORIZED`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `EXPIRED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `REVERSED`

---

### Reçus de paiement

Accès au reçu via numéro de transaction ou numéro de reçu :

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/payments/transactions/{transactionNumber}/receipt` | Reçu JSON |
| `GET` | `/payments/transactions/{transactionNumber}/receipt/pdf` | Reçu PDF |
| `GET` | `/payments/receipts/{receiptNumber}` | Reçu JSON par numéro de reçu |
| `GET` | `/payments/receipts/{receiptNumber}/pdf` | Reçu PDF |

**`PaymentReceiptResponse`**

```json
{
  "receiptNumber": "RCP-202604-000001",
  "receiptIssuedAt": "2026-04-29T10:00:00Z",
  "transactionNumber": "TXN-202604-000042",
  "intentNumber": "INT-202604-000010",
  "paymentMethod": "MOBILE_MONEY",
  "transactionStatus": "SUCCEEDED",
  "provider": "PawaPay",
  "providerReference": "abc123",
  "purpose": "Règlement facture INV-001",
  "sourceType": "INVOICE",
  "sourceCode": "INV-202604-000001",
  "customerType": "MEMBER",
  "customerCode": "MBR-001",
  "customerName": "Jean Client",
  "customerEmail": "jean@email.com",
  "customerPhone": "+242060000000",
  "billingAddressJson": null,
  "paidAmount": 50000,
  "allocatedAmount": 50000,
  "advanceAmount": 0,
  "currency": "XAF",
  "paidAt": "2026-04-29T10:05:00Z",
  "receivedBy": "USR-001",
  "allocations": [
    {
      "documentNumber": "INV-202604-000001",
      "documentType": "INVOICE",
      "title": "Abonnement Avril 2026",
      "documentTotalAmount": 50000,
      "allocatedAmount": 50000,
      "remainingBalanceDue": 0,
      "currency": "XAF"
    }
  ]
}
```

---

### Métriques de caisse

Ces endpoints sont disponibles dans `PaymentController` :

`GET /payments/cash-registers/metrics` — Vue d'ensemble

```json
{
  "currency": "XAF",
  "registerCount": 2,
  "activeRegisterCount": 2,
  "openSessionCount": 1,
  "closingReviewSessionCount": 0,
  "closedSessionCount": 5,
  "movementCount": 48,
  "openingFloatAmount": 100000,
  "totalPayments": 850000,
  "totalRefunds": 15000,
  "totalCashIn": 20000,
  "totalCashOut": 5000,
  "totalAdjustments": 0,
  "netCashPosition": 950000,
  "pendingVarianceAmount": 0
}
```

---

## 2.4 Subscription

### Pause d'abonnement

`PATCH /subscriptions/{subscriptionNumber}/pause`

```json
{
  "days": 15,
  "resumeDate": "2026-05-15",
  "reason": "Déplacement professionnel",
  "changedBy": "USR-001"
}
```

`resumeDate` est prioritaire sur `days`. L'un ou l'autre est suffisant.

### Reprise d'abonnement

`PATCH /subscriptions/{subscriptionNumber}/resume`

Body optionnel :
```json
{
  "reason": "Retour du client",
  "changedBy": "USR-001"
}
```

### Champs ajoutés dans `SubscriptionResponse`

```json
{
  "status": "PAUSED",
  "pausedAt": "2026-04-29T10:00:00Z",
  "pauseUntil": "2026-05-15"
}
```

### Enum `SubscriptionStatus` (complet)

`DRAFT`, `PENDING_ACTIVATION`, `TRIALING`, `ACTIVE`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `CANCELLED`, `EXPIRED`

> `TRIALING` est présent dans l'enum mais non activé dans cette itération.

---

## 2.6 Document & Signature & KYC

### Signature électronique in-app

**Créer une demande de signature :**

`POST /documents/{documentCode}/signatures`

```json
{
  "signerType": "MEMBER",
  "signerId": 123,
  "signerName": "Jean Client",
  "signerEmail": "jean@email.com"
}
```

**Lister les signatures :**

`GET /documents/{documentCode}/signatures`

**Signer le document :**

`PATCH /documents/{documentCode}/signatures/{signatureId}/sign`

```json
{
  "accepted": true,
  "signatureData": "Jean Client"
}
```

`signatureData` : chaîne libre (nom tapé, données SVG d'une signature dessinée, etc.). `accepted` doit être `true`.

La réponse `DocumentSignatureResponse` inclut `signedAt`, `ipAddress`, et `userAgent` pour la traçabilité légale.

---

### KYC — Nouveaux endpoints

#### Dashboard

`GET /kyc/cases/dashboard`

```json
{
  "generatedAt": "2026-04-29T10:00:00Z",
  "summary": {
    "SUBMITTED": 12,
    "UNDER_REVIEW": 5,
    "APPROVED": 80,
    "REJECTED": 3
  },
  "sla": {
    "casesReviewedWithin24h": 15,
    "casesExceeding48h": 2,
    "avgReviewTimeHours": 6.4
  },
  "expiringSoon": {
    "within7Days": 1,
    "within30Days": 4,
    "within60Days": 9
  },
  "byOwnerType": [
    { "ownerType": "MEMBER", "count": 60, "pendingReview": 4 },
    { "ownerType": "CUSTOMER", "count": 35, "pendingReview": 2 }
  ],
  "recentActivity": [
    { "action": "DOCUMENT_APPROVED", "count": 8, "period": "LAST_7_DAYS" }
  ]
}
```

#### Cas expirant bientôt

`GET /kyc/cases/expiring-soon?days=30`

Retourne `List<KycCaseResponse>`.

#### Ma file d'attente (reviewers)

`GET /kyc/cases/my-queue?userId=42`

#### Assigner un cas

`PATCH /kyc/cases/{code}/assign`

```json
{
  "assignedTo": 42
}
```

#### Notes internes

`POST /kyc/cases/{code}/notes`

```json
{
  "content": "Client à appeler pour clarification",
  "authorId": 42,
  "internal": true
}
```

`GET /kyc/cases/{code}/notes`

#### Timeline

`GET /kyc/cases/{code}/timeline`

```json
[
  {
    "timestamp": "2026-04-28T09:00:00Z",
    "action": "DOCUMENT_UPLOADED",
    "actor": "MBR-001",
    "description": "Carte nationale d'identité uploadée"
  }
]
```

#### Statut des documents proches de l'expiration

`GET /kyc/cases/{code}/expiry-status`

```json
[
  {
    "documentCode": "DOC-202604-000010",
    "documentType": "NATIONAL_ID",
    "expiryDate": "2026-05-15",
    "daysUntilExpiry": 16,
    "expired": false,
    "status": "VERIFIED"
  }
]
```

#### Niveau de risque

`PATCH /kyc/cases/{code}/risk-level`

```json
{
  "riskLevel": "MEDIUM",
  "reviewedBy": 42,
  "comment": "Activités commerciales à surveiller"
}
```

**Valeurs `KycRiskLevel` :** `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`

#### Résultat OCR

`GET /kyc/documents/{documentCode}/ocr-result`

```json
{
  "documentCode": "DOC-202604-000010",
  "extractedFirstName": "Jean",
  "extractedLastName": "Client",
  "extractedDateOfBirth": "1990-03-15",
  "extractedExpiryDate": "2028-03-14",
  "extractedDocumentNumber": "CNI-123456",
  "extractedNationality": "CG",
  "confidenceScore": 0.94,
  "rawOcrJson": "{ ... }",
  "processedAt": "2026-04-29T08:12:00Z"
}
```

#### Approbations et rejets en masse

`POST /kyc/documents/bulk-approve`

```json
{
  "documentCodes": ["DOC-001", "DOC-002", "DOC-003"],
  "reviewedBy": 42
}
```

`POST /kyc/documents/bulk-reject`

```json
{
  "items": [
    {
      "documentCode": "DOC-004",
      "rejectionReasonCode": "BLURRY",
      "rejectionReasonDetail": "Document illisible"
    }
  ],
  "reviewedBy": 42
}
```

Réponse `KycBulkActionResponse` :

```json
{
  "processed": 3,
  "succeeded": 3,
  "failed": 0,
  "results": [
    { "documentCode": "DOC-001", "success": true, "errorMessage": null }
  ]
}
```

#### Export PDF d'un dossier KYC

`GET /kyc/cases/{code}/export/pdf?includeInternalNotes=false`

Retourne `application/pdf`.

---

### Champs ajoutés dans `KycCaseResponse`

```json
{
  "assignedTo": 42,
  "assignedAt": "2026-04-28T09:00:00Z",
  "slaDeadline": "2026-04-30T09:00:00Z",
  "lastReminderSentAt": "2026-04-29T08:00:00Z",
  "reminderCount": 1,
  "riskLevel": "LOW",
  "kycLevel": 1,
  "requirements": [
    {
      "documentTypeCode": "NATIONAL_ID",
      "documentTypeName": "Carte Nationale d'Identité",
      "required": true,
      "status": "VERIFIED",
      "documentCode": "DOC-202604-000010"
    }
  ]
}
```

### Enum `KycCaseStatus` (complet)

`NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `PENDING_CORRECTION`, `RENEWAL_REQUIRED`, `EXPIRED`

---

## 2.7 Notification

Dans cette itération, seuls les **templates email** ont été ajoutés. Aucun endpoint nouveau.

Templates disponibles (`src/main/resources/templates/email/`) :

| Template | Déclencheur |
|----------|-------------|
| `booking-reminder.html` | Rappel automatique avant une réservation |
| `billing-reminder.html` | Relance facture OVERDUE |
| `subscription-expiry-reminder.html` | Rappel avant expiration d'abonnement |
| `kyc-expiry-reminder.html` | Rappel avant expiration d'un document KYC |

Ces emails sont envoyés automatiquement par des workers backend. Le front n'a rien à faire.

---

## 2.8 Inventory

### Génération d'étiquettes en masse

`POST /inventory/admin/labels`

```json
{
  "items": [
    { "type": "ITEM",     "code": "ITEM-001" },
    { "type": "ASSET",    "code": "AST-001"  },
    { "type": "LOCATION", "code": "LOC-003"  }
  ]
}
```

Retourne `List<InventoryLabelResponse>` contenant `barcodeValue` et `qrValue` pour chaque entrée.

Endpoint unitaire inchangé : `GET /inventory/admin/labels/{type}/{code}`

---

## 2.9 Analytics & Reporting

### Dashboard temps réel

`GET /analytics/live`

```json
{
  "generatedAt": "2026-04-29T11:32:00Z",
  "activeBookings": 4,
  "checkedInBookings": 2,
  "occupiedResources": 3
}
```

**Polling recommandé : toutes les 30 secondes.**

---

### Comparaison de périodes

`GET /analytics/comparison?currentFrom=2026-04-01&currentTo=2026-04-30&previousFrom=2026-03-01&previousTo=2026-03-31`

```json
{
  "current":  { "...": "AnalyticsOverviewResponse période courante" },
  "previous": { "...": "AnalyticsOverviewResponse période précédente" },
  "revenue":              { "current": 1500000, "previous": 1200000, "percentChange": 25.00 },
  "paid":                 { "current": 1350000, "previous": 1100000, "percentChange": 22.73 },
  "bookings":             { "current": 87,      "previous": 72,      "percentChange": 20.83 },
  "activeSubscriptions":  { "current": 45,      "previous": 40,      "percentChange": 12.50 }
}
```

`percentChange` est positif si la période courante est supérieure, négatif sinon.

---

### Module Reporting (nouveau)

#### Dashboard financier

`GET /reports/finance/dashboard?from=2026-04-01&to=2026-04-30`

```json
{
  "generatedAt": "2026-04-29T11:00:00Z",
  "fromDate": "2026-04-01",
  "toDate": "2026-04-30",
  "invoiceCount": 95,
  "totalInvoiced": 4750000,
  "totalPaid": 4200000,
  "totalOutstanding": 550000,
  "overdueCount": 4,
  "overdueAmount": 180000,
  "collectionRate": 0.884,
  "totalVat": 475000,
  "totalAdditionalCent": 0,
  "totalDiscounts": 50000,
  "cancelledInvoiceCount": 2,
  "revenueBySource": [
    {
      "sourceType": "SUBSCRIPTION",
      "invoiceCount": 45,
      "totalInvoiced": 2250000,
      "totalPaid": 2100000,
      "outstandingAmount": 150000
    }
  ],
  "payments": {
    "transactionCount": 88,
    "totalReceived": 4200000,
    "cashAmount": 800000,
    "walletAmount": 600000,
    "mobileMoneyAmount": 2800000,
    "bankTransferAmount": 0,
    "cardAmount": 0,
    "chequeAmount": 0,
    "failedCount": 3,
    "pendingCount": 1
  }
}
```

#### Répartition des moyens de paiement

`GET /reports/finance/payments?from=2026-04-01&to=2026-04-30`

#### Balance âgée (impayés)

`GET /reports/finance/aging`

Retourne les créances en retard par tranche : courante, 1–30 j, 31–60 j, 61–90 j, +90 j.

#### Rapport par caisse

`GET /reports/finance/cash-registers?from=2026-04-01&to=2026-04-30`

Résumé par caisse enregistreuse : entrées, sorties, sessions, variance.

#### Flux de trésorerie

`GET /reports/finance/cash-flow?from=2026-04-01&to=2026-04-30`

Flux journalier : entrées vs sorties par type de mouvement.

---

#### Taux d'occupation des ressources

`GET /reports/occupancy?from=2026-04-01&to=2026-04-30`

```json
{
  "generatedAt": "2026-04-29T11:00:00Z",
  "fromDate": "2026-04-01",
  "toDate": "2026-04-30",
  "periodDays": 30,
  "totalBookings": 214,
  "totalBookedMinutes": 38520,
  "overallOccupancyRate": 0.538,
  "byResourceType": [
    {
      "typeCode": "PRIVATE_OFFICE",
      "typeName": "Bureau Privatif",
      "resourceCount": 5,
      "totalBookings": 120,
      "totalBookedMinutes": 21600,
      "totalRevenue": 3240000,
      "occupancyRate": 0.60
    }
  ],
  "byResource": [
    {
      "resourceCode": "BUREAU-01",
      "resourceName": "Bureau Lembissi",
      "typeCode": "PRIVATE_OFFICE",
      "typeName": "Bureau Privatif",
      "bookingCount": 28,
      "bookedMinutes": 5040,
      "availableMinutes": 7200,
      "occupancyRate": 0.70,
      "revenue": 756000
    }
  ]
}
```

`occupancyRate` : valeur entre 0 et 1 (ex : `0.70` = 70 %). Calculée sur la base de 8 h/jour.

---

## Récapitulatif des enums utiles

### `CongoCorrespondent`
`MTN_MOMO_COG`, `AIRTEL_OAPI_COG`, `ORANGE_COG`, `AIRTEL_OAPI_COD`, `ORANGE_COD`, `MPESA_COD`

### `PaymentMethod`
`CASH`, `WALLET`, `MOBILE_MONEY`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE`

### `PaymentIntentStatus`
`PENDING`, `PROCESSING`, `AUTHORIZED`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `EXPIRED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `REVERSED`

### `BookingWaitlistStatus`
`WAITING`, `OFFERED`, `BOOKED`, `CANCELLED`, `EXPIRED`

### `SubscriptionStatus`
`DRAFT`, `PENDING_ACTIVATION`, `TRIALING`, `ACTIVE`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `CANCELLED`, `EXPIRED`

### `KycCaseStatus`
`NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `PENDING_CORRECTION`, `RENEWAL_REQUIRED`, `EXPIRED`

### `KycRiskLevel`
`LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`