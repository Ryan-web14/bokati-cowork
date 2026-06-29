# Guide Frontend/API — Améliorations Cash Register (mouvements, statistiques, anomalies, demandes)

Ce document complète `docs/payment-frontend-api.md` (section "Cash register") en couvrant les
nouvelles fonctionnalités ajoutées au module caisse :

1. Enrichissement des mouvements de caisse (statut, canal, solde courant, pièces jointes, signature)
2. Demandes de justificatif et de remise de fonds avec validation superviseur + notification mail
3. Statistiques de session, de caissier et de caisse
4. Détection semi-automatisée d'anomalies (fraude)
5. Génération de documents PDF (pièces justificatives, demandes, rapport de clôture)

Toutes les routes sont préfixées par `/api/v1/cash-registers` (voir `ApiPath.V1`). La devise de
caisse reste `XAF`.

---

## 1. Mouvements de caisse enrichis

### 1.1 Nouveaux champs de `CreateCashMovementRequest`

En plus des champs existants (`movementType`, `amount`, `documentType`, `flowCategory`,
`counterpartyCode`, `metadataJson`, etc.), la création de mouvement accepte désormais :

```json
{
  "movementType": "CASH_OUT",
  "amount": 25000,
  "relatedMovementNumber": "CSM-202604-000010",
  "batchId": "BATCH-2026-04-07-01",
  "exchangeRate": 1.0,
  "channel": "MANUAL",
  "deviceCode": "POS-RECEPTION-01",
  "deviceIp": "192.168.1.42",
  "subCategory": "SUPPLIER_PAYMENT_URGENT",
  "tags": "urgent,fournisseur,validation-requise"
}
```

- `relatedMovementNumber` — relie ce mouvement à un autre (ex. contrepassation, remboursement lié à un paiement)
- `batchId` — regroupe des mouvements générés en lot (ex. import, rapprochement)
- `exchangeRate` — taux de change appliqué si le mouvement provient d'une devise étrangère
- `channel` — `MANUAL`, `AUTO_PAYMENT`, `IMPORT`, `API`, `POS`
- `deviceCode` / `deviceIp` — traçabilité du poste/dispositif à l'origine du mouvement
- `subCategory` / `tags` — classification libre pour le reporting et les filtres

### 1.2 Nouveaux champs de `CashMovementResponse`

```json
{
  "movementNumber": "CSM-202604-000011",
  "status": "CONFIRMED",
  "relatedMovementNumber": "CSM-202604-000010",
  "batchId": "BATCH-2026-04-07-01",
  "runningBalance": 187500.0000,
  "exchangeRate": 1.0,
  "channel": "MANUAL",
  "movementDeviceCode": "POS-RECEPTION-01",
  "deviceIp": "192.168.1.42",
  "subCategory": "SUPPLIER_PAYMENT_URGENT",
  "tags": "urgent,fournisseur,validation-requise",
  "riskScore": null,
  "requiresSignature": false,
  "signedBy": null,
  "signedAt": null,
  "printedAt": null,
  "attachments": []
}
```

- `status` — `PENDING`, `CONFIRMED`, `REVERSED`, `CANCELLED`
- `runningBalance` — solde de caisse théorique après ce mouvement, recalculé en rejouant
  chronologiquement tous les mouvements de la session depuis `openingAmount`
- `riskScore` — score de risque optionnel (alimenté par les règles de détection d'anomalies)
- `attachments` — liste des pièces jointes (voir 1.3)

### 1.3 Pièces jointes d'un mouvement

```http
POST /cash-registers/movements/{movementNumber}/attachments
```

```json
{
  "fileName": "facture-fournisseur-0456.pdf",
  "contentType": "application/pdf",
  "storagePath": "cash/movements/CSM-202604-000011/facture-fournisseur-0456.pdf",
  "label": "Facture fournisseur",
  "uploadedBy": "admin-001"
}
```

Retourne le mouvement avec sa liste `attachments` mise à jour (`CashMovementAttachmentResponse`
contient `id`, `fileName`, `contentType`, `storagePath`, `label`, `uploadedBy`, `uploadedAt`).

### 1.4 Signature et impression

```http
PATCH /cash-registers/movements/{movementNumber}/sign?signedBy=supervisor-001
PATCH /cash-registers/movements/{movementNumber}/print
```

- `sign` renseigne `signedBy` et `signedAt` (utile pour les mouvements à `requiresSignature = true`)
- `print` renseigne `printedAt`, à appeler après l'impression d'une pièce justificative

---

## 2. Demandes de caisse (justificatif / remise de fonds)

Permet à un caissier de soumettre une demande qui doit être validée par un superviseur avant
d'être exécutée automatiquement sous forme de mouvement de caisse.

### 2.1 Types et statuts

- `requestType` : `JUSTIFICATIF`, `REMISE_DE_FONDS`, `CASH_ADVANCE`, `EXPENSE_REIMBURSEMENT`
- `status` : `PENDING` → `APPROVED`/`REJECTED` → (si approuvée) `EXECUTED` ; ou `CANCELLED`

À l'approbation, la demande est automatiquement convertie en mouvement de caisse :

| `requestType`           | `movementType` généré | `documentType`   |
|-------------------------|------------------------|------------------|
| `REMISE_DE_FONDS`       | `SAFE_DEPOSIT`         | `BANK_SLIP`      |
| `JUSTIFICATIF`          | `ADJUSTMENT`           | `CASH_VOUCHER`   |
| `CASH_ADVANCE`          | `CASH_OUT`             | `EXIT_VOUCHER`   |
| `EXPENSE_REIMBURSEMENT` | `CASH_OUT`             | `EXPENSE_NOTE`   |

### 2.2 Soumettre une demande

```http
POST /cash-registers/sessions/{sessionNumber}/requests
```

```json
{
  "requestType": "REMISE_DE_FONDS",
  "amount": 150000,
  "currency": "XAF",
  "reason": "Dépôt en banque de fin de journée",
  "requestedBy": "cashier-001",
  "attachments": [
    { "fileName": "comptage.pdf", "contentType": "application/pdf", "storagePath": "cash/requests/comptage.pdf", "label": "Feuille de comptage" }
  ]
}
```

Envoie une notification au superviseur (email générique avec lien vers la demande).

### 2.3 Lister / consulter

```http
GET /cash-registers/requests
GET /cash-registers/requests/{requestNumber}
```

Filtres de liste : `registerCode`, `sessionNumber`, `requestType`, `status`, `requestedBy`, `searchText`, `page`, `size`.

### 2.4 Approuver / rejeter (superviseur)

```http
PATCH /cash-registers/requests/{requestNumber}/approve
PATCH /cash-registers/requests/{requestNumber}/reject
```

```json
{
  "reviewedBy": "supervisor-001",
  "note": "Validé après vérification du comptage"
}
```

- À l'approbation : exécution automatique (création du mouvement lié), notification email au
  demandeur ("Demande de caisse approuvée — ...") avec mention du mouvement généré
- Au rejet : notification email au demandeur ("Demande de caisse rejetée — ...") avec le motif

---

## 3. Statistiques de caisse

Routes en lecture seule, préfixées par `/cash-registers/statistics`.

### 3.1 Statistiques de session

```http
GET /cash-registers/statistics/sessions/{sessionNumber}
```

```json
{
  "sessionNumber": "CSS-202604-000001",
  "registerCode": "CSR-00001",
  "openedBy": "cashier-001",
  "durationMinutes": 480,
  "movementCount": 42,
  "transactionsPerHour": 5.25,
  "totalInflow": 620000.0000,
  "totalOutflow": 95000.0000,
  "inOutRatio": 6.5263,
  "idleMinutes": 35,
  "averageMovementAmount": 18500.0000,
  "varianceAmount": -500.0000,
  "variancePercentage": -0.27
}
```

- `transactionsPerHour` = nombre de mouvements ÷ durée de session en heures
- `inOutRatio` = total des entrées ÷ total des sorties (`null` si aucune sortie)
- `idleMinutes` = somme des intervalles sans mouvement (ouverture → 1er mouvement → ... → clôture/maintenant)
- `variancePercentage` = écart de caisse ÷ montant attendu, en %

### 3.2 Statistiques de caissier

```http
GET /cash-registers/statistics/cashiers/{cashierCode}
```

```json
{
  "cashierCode": "cashier-001",
  "sessionCount": 64,
  "closedSessionCount": 60,
  "reviewSessionCount": 7,
  "reviewFrequencyPercentage": 10.94,
  "averageVarianceAmount": -120.5,
  "varianceStandardDeviation": 980.3,
  "varianceTrend": "STABLE",
  "averageSessionDurationMinutes": 452.8,
  "riskScore": 14.32
}
```

- `reviewFrequencyPercentage` = part des sessions ayant généré un écart non nul
- `averageVarianceAmount` / `varianceStandardDeviation` proviennent de l'historique des
  sessions clôturées du caissier (agrégat SQL `AVG`/`STDDEV_SAMP`)
- `varianceTrend` — `IMPROVING`, `STABLE` ou `DEGRADING`, calculé en comparant la moyenne des
  écarts absolus de la première moitié des sessions clôturées vis-à-vis de la seconde moitié
- `riskScore` — score composite dérivé de la fréquence de revue, de l'écart-type des écarts et
  de la tendance (0 à 100)

### 3.3 Statistiques de caisse (registre)

```http
GET /cash-registers/statistics/registers/{registerCode}?fromDate=2026-04-01T00:00:00Z&toDate=2026-04-30T23:59:59Z
```

```json
{
  "registerCode": "CSR-00001",
  "registerName": "Caisse réception",
  "sessionCount": 30,
  "movementCount": 540,
  "averageSessionDurationMinutes": 460.2,
  "averageVarianceAmount": -85.4,
  "peakHours": [
    { "hourOfDay": 11, "movementCount": 64, "totalAmount": 1180000.0000 },
    { "hourOfDay": 15, "movementCount": 58, "totalAmount": 990000.0000 }
  ],
  "periodOverPeriodChangePercentage": 12.5
}
```

- `peakHours` — top 5 des heures (UTC) avec le plus de mouvements sur la période
- `periodOverPeriodChangePercentage` — variation du nombre de mouvements vs. une période
  précédente de même durée immédiatement avant `fromDate` (`null` si dates non fournies ou
  absence de données comparatives)

---

## 4. Détection semi-automatisée d'anomalies (fraude)

Routes préfixées par `/cash-registers/anomalies`. Les règles sont exécutées automatiquement
**à chaque clôture de session** (après validation transactionnelle, via un événement
`CashSessionClosedEvent` traité en `AFTER_COMMIT`), et peuvent être relancées manuellement.

### 4.1 Règles de détection

| Type                        | Déclencheur |
|-----------------------------|-------------|
| `VARIANCE_OUTLIER`          | Écart de caisse significativement éloigné (z-score > 2 ou > 3) de la moyenne/écart-type historiques du caissier (ou écart brut important si historique insuffisant) |
| `ROUND_NUMBER_PATTERN`      | Plus de 50 % des mouvements de la session portent des montants ronds (multiples de 5 000 XAF) |
| `EXCESSIVE_REFUNDS`         | Ratio remboursements / paiements > 30 % (avec au moins 3 remboursements) |
| `EXCESSIVE_ADJUSTMENTS`     | 5 ajustements manuels ou plus sur la session |
| `OFF_HOURS_SESSION`         | Session ouverte entre 22h et 6h (UTC) |
| `THRESHOLD_STRUCTURING`     | 3 mouvements ou plus avec des montants compris entre 80 % et 100 % du plafond de caisse (`maxCashAmount`) — fractionnement suspect |
| `NEAR_MAX_CASH_RECURRENCE`  | Le solde courant atteint au moins 90 % du plafond de caisse à 2 reprises ou plus sans dépôt |

Chaque règle ne génère **qu'un seul** signalement par session (idempotence via
`existsByCashSession_IdAndAnomalyType`). Les signalements de sévérité `MEDIUM` ou `HIGH`
déclenchent une notification email au superviseur.

### 4.2 Lancer une analyse manuelle

```http
POST /cash-registers/anomalies/sessions/{sessionNumber}/analyze
```

Retourne la liste des **nouveaux** signalements créés lors de cette analyse (tableau vide si
aucune nouvelle anomalie, y compris si elles avaient déjà été détectées précédemment).

### 4.3 Lister les signalements

```http
GET /cash-registers/anomalies
```

Filtres : `registerCode`, `sessionNumber`, `severity` (`LOW`/`MEDIUM`/`HIGH`), `status`
(`OPEN`/`ACKNOWLEDGED`/`DISMISSED`/`ESCALATED`), `anomalyType`, `page`, `size`.

```json
{
  "flagNumber": "CAF-202604-000003",
  "sessionNumber": "CSS-202604-000001",
  "movementNumber": null,
  "registerCode": "CSR-00001",
  "anomalyType": "VARIANCE_OUTLIER",
  "severity": "HIGH",
  "score": 3.42,
  "description": "Écart de caisse de -8500 s'écartant fortement de la moyenne habituelle (-120.50, écart-type 980.30) du caissier cashier-001 (z-score 3.42).",
  "detectedAt": "2026-04-07T18:32:00Z",
  "status": "OPEN",
  "reviewedBy": null,
  "reviewedAt": null,
  "reviewNote": null
}
```

### 4.4 Examiner un signalement (superviseur)

```http
PATCH /cash-registers/anomalies/{flagNumber}/review
```

```json
{
  "reviewedBy": "supervisor-001",
  "status": "ACKNOWLEDGED",
  "note": "Vérifié avec le caissier — erreur de comptage corrigée"
}
```

`status` cible doit être l'une de `ACKNOWLEDGED`, `DISMISSED`, `ESCALATED` (ou `OPEN` pour
rouvrir). Un signalement déjà `DISMISSED`/`ESCALATED` ne peut plus être réexaminé.

---

## 5. Génération de documents PDF

Routes retournant `application/pdf` (en-tête `Content-Disposition: inline`).

```http
GET /cash-registers/movements/{movementNumber}/document
GET /cash-registers/requests/{requestNumber}/document
GET /cash-registers/sessions/{sessionNumber}/closing-report
```

- **Document de mouvement** — pièce justificative individuelle (bon de sortie, bordereau de
  remise de fonds, pièce de caisse, etc. selon `documentType`/`movementType`) avec montant,
  motif, statut de signature et emplacements de signature
- **Document de demande** — récapitulatif de la demande de justificatif/remise de fonds avec
  son statut de validation, le mouvement généré et la note du superviseur
- **Rapport de clôture de session** — synthèse complète d'une session clôturée : informations
  d'ouverture/fermeture, totaux par catégorie de flux, écart constaté, statistiques
  (durée, nombre de mouvements, ratio entrées/sorties, temps d'inactivité) et détail
  chronologique des mouvements avec solde courant — prêt à être signé par le caissier et le
  superviseur

---

## 6. Récapitulatif des nouvelles routes

```text
POST   /cash-registers/movements/{movementNumber}/attachments
PATCH  /cash-registers/movements/{movementNumber}/sign
PATCH  /cash-registers/movements/{movementNumber}/print
GET    /cash-registers/movements/{movementNumber}/document

POST   /cash-registers/sessions/{sessionNumber}/requests
GET    /cash-registers/requests
GET    /cash-registers/requests/{requestNumber}
PATCH  /cash-registers/requests/{requestNumber}/approve
PATCH  /cash-registers/requests/{requestNumber}/reject
GET    /cash-registers/requests/{requestNumber}/document

GET    /cash-registers/statistics/sessions/{sessionNumber}
GET    /cash-registers/statistics/cashiers/{cashierCode}
GET    /cash-registers/statistics/registers/{registerCode}

POST   /cash-registers/anomalies/sessions/{sessionNumber}/analyze
GET    /cash-registers/anomalies
PATCH  /cash-registers/anomalies/{flagNumber}/review

GET    /cash-registers/sessions/{sessionNumber}/closing-report
```
