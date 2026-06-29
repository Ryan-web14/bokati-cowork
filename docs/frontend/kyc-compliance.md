# KYC Compliance - API frontend

Base path: `/v1`

Cette version ajoute les ameliorations KYC 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14 et 18. Les traitements automatisables sont lances cote backend par workers planifies: expiration/renouvellement, rappels d'incompletude, auto-approbation immediate ou differee, timeline, OCR placeholder et synchronisation de statut.

## Statuts et enums

`KycCaseStatus`:
`NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `PENDING_CORRECTION`, `RENEWAL_REQUIRED`, `EXPIRED`

`KycRiskLevel`:
`LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`

`KycDocumentVerificationStatus`:
`PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED`

## Listing et dashboard

### `GET /v1/kyc/cases`

Query params optionnels:

| Param | Type | Description |
|---|---:|---|
| `status` | enum | Filtre par statut dossier |
| `ownerType` | enum | `MEMBER`, `CUSTOMER`, `BUSINESS` |
| `submittedAfter` | ISO datetime | Soumis apres cette date |
| `submittedBefore` | ISO datetime | Soumis avant cette date |
| `reviewedBy` | number | Agent reviewer |
| `pendingReviewOnly` | boolean | Limite a `SUBMITTED` et `UNDER_REVIEW` |
| `expiringWithinDays` | number | Dossiers ayant un document qui expire dans N jours |
| `riskLevel` | enum | Filtre risque |

La reponse est une liste de `KycCaseResponse`.

Champs ajoutes dans `KycCaseResponse`:

```json
{
  "assignedTo": 42,
  "assignedAt": "2026-04-29T09:10:00Z",
  "slaDeadline": "2026-04-30T09:10:00Z",
  "lastReminderSentAt": "2026-04-29T09:00:00Z",
  "reminderCount": 1,
  "riskLevel": "LOW",
  "kycLevel": 2
}
```

### `GET /v1/kyc/cases/dashboard`

Retourne les compteurs compliance: total par statut, SLA, expirations 7/30/60 jours, repartition par type de proprietaire et activite recente.

### `GET /v1/kyc/cases/expiring-soon?days=30`

Liste les dossiers ayant au moins un document expirant dans l'horizon demande.

## Assignation et file reviewer

### `PATCH /v1/kyc/cases/{code}/assign`

```json
{ "assignedTo": 42 }
```

Assigne le dossier, passe `SUBMITTED` en `UNDER_REVIEW`, calcule `slaDeadline`.

### `GET /v1/kyc/cases/my-queue?userId=42`

Retourne les dossiers assignes au reviewer. Si `userId` est absent, le backend tente d'utiliser l'utilisateur connecte.

## Notes et timeline

### `POST /v1/kyc/cases/{code}/notes`

```json
{
  "content": "Verifier avec le service commercial.",
  "authorId": 42,
  "internal": true
}
```

### `GET /v1/kyc/cases/{code}/notes`

Retourne les notes du dossier. Les notes internes ne doivent pas etre affichees sur un portail client.

### `GET /v1/kyc/cases/{code}/timeline`

Retourne une timeline unifiee: creation, soumission, upload documents, reviews, notes, events outbox.

## Expiration et renouvellement

### `GET /v1/kyc/cases/{code}/expiry-status`

Retourne les dates d'expiration document par document:

```json
[
  {
    "documentCode": "DOC-202604-000012",
    "documentType": "MEMBER_ID_CARD",
    "expiryDate": "2026-05-20",
    "daysUntilExpiry": 21,
    "expired": false,
    "status": "VERIFIED"
  }
]
```

Automation backend:

- Tous les lundis 08:00: rappels d'expiration a J-60/J-30/J-7 via outbox.
- Documents expires: `Document.status = EXPIRED`, `KycDocument.status = EXPIRED`.
- Dossier approuve avec document expire: `RENEWAL_REQUIRED`.
- Autres dossiers: `PENDING_CORRECTION`.

## Risque et niveaux KYC

### `PATCH /v1/kyc/cases/{code}/risk-level`

```json
{
  "riskLevel": "HIGH",
  "reviewedBy": 42,
  "comment": "Profil entreprise sensible"
}
```

Le backend calcule aussi un `kycLevel`:

- `1`: profil de base
- `2`: au moins un document verifie
- `3`: entreprise ou plusieurs documents verifies
- `4`: risque haut/tres haut ou document selfie/liveness/AML

Les plans d'abonnement exposent maintenant un champ backend `requiredKycLevel`. La creation d'abonnement est bloquee si le dossier KYC approuve du souscripteur n'atteint pas ce niveau.

## OCR Tesseract et validation croisee

### `GET /v1/kyc/documents/{documentCode}/ocr-result`

Retourne les donnees OCR stockees. Le backend utilise maintenant Tess4J/Tesseract en provider local. Si OCR est desactive, si le fichier n'existe pas, ou si `tessdata` n'est pas disponible, le backend persiste un resultat avec `confidenceScore = 0` et `rawOcrJson.status = SKIPPED` ou `FAILED`.

Champs actuellement extraits automatiquement:

- `extractedDocumentNumber` via regex sur numero/number/id.
- `extractedExpiryDate` via detection de dates `yyyy-MM-dd`, `dd/MM/yyyy`, `dd-MM-yyyy`.
- `rawOcrJson.rawText` contient le texte brut reconnu par Tesseract en cas de succes.

L'approbation d'un dossier applique les regles actives de `kyc_cross_validation_rule`. Une regle bloquante incoherente provoque un `400`.

## Export PDF

### `GET /v1/kyc/cases/{code}/export/pdf?includeInternalNotes=true`

Retourne `application/pdf`. Le rapport inclut resume dossier, documents, expiration, timeline et notes internes si demande.

## Bulk review

### `POST /v1/kyc/documents/bulk-approve`

```json
{
  "documentIds": ["DOC-001", "DOC-002"],
  "reviewedBy": 42
}
```

### `POST /v1/kyc/documents/bulk-reject`

```json
{
  "reviewedBy": 42,
  "items": [
    { "documentCode": "DOC-004", "reason": "Photo floue" },
    { "documentCode": "DOC-005", "reason": "Document expire" }
  ]
}
```

Reponse:

```json
{
  "processed": 2,
  "succeeded": 1,
  "failed": 1,
  "results": [
    { "documentCode": "DOC-004", "success": true, "error": null },
    { "documentCode": "DOC-005", "success": false, "error": "Document not found" }
  ]
}
```

Chaque document est traite independamment. Un echec n'annule pas les autres.

## Configuration utile

Les valeurs par defaut backend:

```yaml
app:
  kyc:
    expiry:
      reminder-days: [60, 30, 7]
      grace-period-days: 15
    reminders:
      enabled: true
      reminder-after-days: [3, 7, 14]
      max-reminders: 3
    ocr:
      enabled: true
      provider: TESSERACT
      language: fra+eng
      data-path: storage/tessdata
```

Le dossier local `storage/tessdata` contient les modeles officiels `fra.traineddata` et `eng.traineddata`. Pour Windows, installer le runtime Microsoft Visual C++ 2022 si Tess4J le demande.

## Points UI recommandes

- Afficher `riskLevel`, `kycLevel`, `assignedTo`, `slaDeadline` dans la vue dossier compliance.
- Utiliser `dashboard` pour les cartes de synthese compliance.
- Utiliser `my-queue` pour la vue "Mes dossiers".
- Afficher `expiry-status` sous forme d'alerte lorsque `daysUntilExpiry <= 30` ou `expired = true`.
- Afficher `timeline` comme source d'audit principale.
- Sur le portail client, ne jamais afficher les notes `internal = true`.
