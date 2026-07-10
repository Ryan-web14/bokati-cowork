# KYC — Revue de documents (globale) & upload admin

> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> **Back-office** (JWT admin requis).

Deux contrôleurs :

- **Revue globale** (`/kyc/documents`) — traiter les documents KYC de **tous les dossiers** confondus.
- **Upload admin** (`/kyc/documents/admin`) — un agent saisit un document reçu hors ligne (guichet, email) pour le compte d'un client.

Le modèle `KycDocumentResponse` est décrit dans [kyc-cases.md](./kyc-cases.md) §5.

---

## Partie A — Revue globale · `/kyc/documents`

### A.1 Approuver en masse

```http
POST /kyc/documents/bulk-approve
Content-Type: application/json
Idempotency-Key: <uuid>
```
```json
{ "documentIds": ["KYD-...01", "KYD-...02"], "reviewedBy": 42 }
```
⚠️ Ici le champ s'appelle **`documentIds`** (liste de **codes** de documents), et non `documentCodes` comme dans la version « dossier » ([kyc-cases.md](./kyc-cases.md) §5). `reviewedBy` obligatoire. Route idempotente.

### A.2 Rejeter en masse — **motif par document**

```http
POST /kyc/documents/bulk-reject
   (alias : /bulk-disapprove, /bulk-desapprove)
Content-Type: application/json
```
```json
{
  "items": [
    { "documentCode": "KYD-...03", "reason": "Document illisible" },
    { "documentCode": "KYD-...04", "reason": "Date d'expiration dépassée" }
  ],
  "reviewedBy": 42
}
```
Chaque item porte son propre `reason` (obligatoire). Route idempotente.

**Réponse** (approve/reject) → `KycBulkActionResponse` :
```json
{
  "processed": 2,
  "succeeded": 2,
  "failed": 0,
  "results": [
    { "documentCode": "KYD-...03", "success": true, "error": null },
    { "documentCode": "KYD-...04", "success": true, "error": null }
  ]
}
```

### A.3 Lister les documents à revoir / déjà traités

```http
GET /kyc/documents/review-queue    → List<KycDocumentResponse>   (tous les documents en attente, global)
GET /kyc/documents/reviewed?status=APPROVED   → List<KycDocumentResponse>
GET /kyc/documents/approved        → List<KycDocumentResponse>   (raccourci : uniquement VERIFIED)
GET /kyc/documents/rejected        → List<KycDocumentResponse>   (alias : /disapproved, /desapproved)
```

Le paramètre `status` de `/reviewed` :
- accepte `APPROVED`/`VERIFIED` **ou** `REJECTED`/`DISAPPROVED`/`DESAPPROVED` (insensible à la casse) ;
- absent → renvoie **les deux** (vérifiés + rejetés) ;
- toute autre valeur → **400 Bad Request** (`"Unsupported KYC document review status: ..."`).

### A.4 Résultat OCR d'un document

```http
GET /kyc/documents/{documentCode}/ocr-result   → KycDocumentOcrResultResponse
```
```json
{
  "documentCode": "KYD-...01",
  "extractedFirstName": "Jean",
  "extractedLastName": "Kabila",
  "extractedDateOfBirth": "1990-05-20",
  "extractedExpiryDate": "2030-01-15",
  "extractedDocumentNumber": "1234567890",
  "extractedNationality": "COG",
  "confidenceScore": 0.94,
  "rawOcrJson": "{ ...payload brut du moteur OCR... }",
  "processedAt": "2026-07-05T14:05:00Z"
}
```
- `confidenceScore` : score global de confiance (0–1).
- `rawOcrJson` : payload brut du moteur OCR (JSON sérialisé en chaîne) — utile pour du debug/affichage avancé, mais préférer les champs `extracted*` structurés pour l'UI standard.

---

## Partie B — Upload admin · `/kyc/documents/admin`

Utile quand un document est reçu hors ligne et doit être saisi par le staff pour le compte d'un client.

### B.1 Uploader un document (multipart)

```http
POST /kyc/documents/admin/upload
Content-Type: multipart/form-data
```
Champs de formulaire (query/form params) + partie `file` :

| Champ | Obligatoire | Description |
|---|---|---|
| `ownerType` | ✅ | `MEMBER`, `CUSTOMER` ou `BUSINESS`. |
| `ownerCode` | ✅ | **Code public** du propriétaire (jamais l'id interne). |
| `documentTypeCode` | ✅ | Ex : `CNI`, `PASSEPORT`, `NIU`. |
| `side` | — | `FRONT` (défaut) ou `BACK`. |
| `documentNumber` | — | Numéro sur la pièce (exigé selon le type). |
| `issueDate` | — | `yyyy-MM-dd`. |
| `expiryDate` | — | `yyyy-MM-dd`. |
| `file` | ✅ | La partie binaire. |

**Réponse** `201 Created` → `KycDocumentResponse` (voir [kyc-cases.md](./kyc-cases.md) §5).

### B.2 Compléter les métadonnées d'un document existant

```http
PATCH /kyc/documents/admin/{kycDocumentId}/details
Content-Type: application/json
```
```json
{ "documentNumber": "1234567890", "issueDate": "2020-01-15", "expiryDate": "2030-01-15" }
```
Tous les champs sont optionnels — utile lorsque le document a été uploadé d'abord et les détails ajoutés après. **Réponse** `200 OK` → `KycDocumentResponse`.

⚠️ Le path prend l'**`kycDocumentId`** (id numérique, champ `id` du `KycDocumentResponse`), pas le `documentCode`.

---

## Enum

- **KycDocumentVerificationStatus** : `PENDING, VERIFIED, REJECTED, EXPIRED`

> **Note d'implémentation** : `KycDocumentReviewController` (`/kyc/documents/**`) et `KycDocumentAdminController` (`/kyc/documents/admin/**`) relèvent du module KYC (préfixe `/kyc` résolu avant `/documents`), pas de la GED générique `/documents`.
