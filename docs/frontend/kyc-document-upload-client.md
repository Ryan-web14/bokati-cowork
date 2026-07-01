# KYC Document Upload — Client (Espace Client) Guide

Base URL: `/sni/api/v1`

All client endpoints require a valid Member JWT. The member identity is derived from the token — no owner params needed.

---

## 1. Get Upload Configuration for a Document Type

Fetch this first to know which form fields to display and which files to accept.

```
GET /document-types/{code}/upload-config
```

### Path Parameter
| Param | Description |
|-------|-------------|
| `code` | Document type code (e.g. `CNI`, `PASSEPORT`, `NIU`) |

### Response `200 OK`
```json
{
  "documentTypeCode": "CNI",
  "documentTypeName": "Carte Nationale d'Identité",
  "helpText": "Scan recto et verso de la CNI en cours de validité",
  "requiresDocumentNumber": true,
  "requiresIssueDate": true,
  "requiresExpiryDate": true,
  "requiresBackSide": true,
  "allowedMimeTypes": ["application/pdf", "image/jpeg", "image/png"],
  "maxFileSizeBytes": 10485760,
  "category": "KYC"
}
```

### Client-side form rendering rules

| Field config | What to render |
|---|---|
| `requiresDocumentNumber=true` | Text input "Numéro de document" — **required** |
| `requiresIssueDate=true` | Date input "Date d'émission" — **required** |
| `requiresExpiryDate=true` | Date input "Date d'expiration" — **required** |
| `requiresDocumentNumber=false` | Do NOT render that input |
| `requiresIssueDate=false` | Do NOT render that input |
| `requiresExpiryDate=false` | Do NOT render that input |
| `requiresBackSide=true` | Show a two-step upload flow (FRONT then BACK) |
| `allowedMimeTypes` | Use as `<input type="file" accept="...">` value |
| `maxFileSizeBytes` | Validate client-side before upload |

> **Client rule:** `requires*` flags are enforced server-side. Missing required fields will return a `400` with a descriptive French error message.

---

## 2. View KYC Case Status

```
GET /portal/kyc/my-case
```

Response includes case status, completion percent, and missing document types.

---

## 3. List Requirements

```
GET /portal/kyc/requirements
```

Returns all required document types with their current status for the authenticated member.

```json
[
  {
    "documentTypeCode": "CNI",
    "documentTypeName": "Carte Nationale d'Identité",
    "required": true,
    "requiresBackSide": true,
    "status": "PENDING",
    "documentCode": "DOC-abc123",
    "backDocumentCode": null
  }
]
```

Status values: `null` (not submitted), `PENDING`, `APPROVED`, `REJECTED`, `EXPIRED`

---

## 4. Upload a KYC Document

```
POST /portal/kyc/documents/upload
Content-Type: multipart/form-data
```

### Form Fields

| Field | Required | Enforcement |
|-------|----------|-------------|
| `documentType` | ✅ | Must be a valid active code |
| `file` | ✅ | Size and MIME validated against DocumentType config |
| `side` | — | `FRONT` (default) or `BACK` |
| `documentNumber` | conditional | **Required** if `requiresDocumentNumber=true` for this type |
| `issueDate` | conditional | **Required** (`yyyy-MM-dd`) if `requiresIssueDate=true` for this type |
| `expiryDate` | conditional | **Required** (`yyyy-MM-dd`) if `requiresExpiryDate=true` for this type |

### Upload Flow for Two-Sided Documents

For types where `requiresBackSide=true` (e.g. CNI):

1. `side=FRONT` — upload the front face → creates the KYC entry
2. `side=BACK` — upload the back face → attaches to the existing entry

BACK upload returns `400` if FRONT has not been uploaded yet.

### Response `200 OK`
```json
{
  "id": 42,
  "documentCode": "DOC-abc123",
  "documentType": "CNI",
  "documentNumber": "123456789",
  "fileName": "cni-recto.jpg",
  "fileSize": 204800,
  "mimeType": "image/jpeg",
  "requiresBackSide": true,
  "backDocumentCode": null,
  "backFileName": null,
  "backFileSize": null,
  "backMimeType": null,
  "issueDate": "2020-01-15",
  "expiryDate": "2030-01-14",
  "status": "PENDING",
  "uploadedAt": "2026-07-01T10:30:00Z"
}
```

### Error Examples

| Status | Message |
|--------|---------|
| `400` | "Le numéro de document est obligatoire pour le type : Carte Nationale d'Identité" |
| `400` | "La date d'émission est obligatoire pour le type : Carte Nationale d'Identité" |
| `400` | "La date d'expiration est obligatoire pour le type : NIU" |
| `400` | "Taille du fichier (12288 Ko) dépasse la limite autorisée de 10 Mo" |
| `400` | "Type de fichier non autorisé. Acceptés : application/pdf, image/jpeg, image/png" |
| `400` | "Le recto doit être uploadé avant le verso pour le type : CNI" |
| `400` | "KYC case is not open for document uploads. Current status: UNDER_REVIEW" |

---

## 5. List My Documents

```
GET /portal/kyc/documents
```

Returns all KYC documents submitted by the authenticated member.

---

## 6. Get a Single Document

```
GET /portal/kyc/documents/{id}
```

---

## 7. Download Document File

```
GET /portal/kyc/documents/{id}/download
```

Returns the raw file with appropriate `Content-Type` and `Content-Disposition` headers.

---

## 8. Resubmit a Document

Allowed only when current status is `PENDING` or `REJECTED`.

```
POST /portal/kyc/documents/{id}/resubmit
Content-Type: multipart/form-data
```

Form field: `file` (the new file, replaces the existing one)

---

## 9. Delete a Document

Allowed only when current status is `PENDING`.

```
DELETE /portal/kyc/documents/{id}
```

Response: `204 No Content`

---

## 10. Submit KYC Case for Review

Once all required documents are uploaded, submit the case.

```
POST /portal/kyc/submit
```

The case must be in `IN_PROGRESS` or `PENDING_CORRECTION` status.

Response includes updated case status (`SUBMITTED` or `UNDER_REVIEW`).

---

## 11. Completion Summary

```
GET /portal/kyc/completion
```

Useful for progress bars.

```json
{
  "caseCode": "KYC-001",
  "caseStatus": "IN_PROGRESS",
  "completionPercent": 66,
  "totalRequired": 3,
  "totalSubmitted": 2,
  "totalVerified": 0,
  "missingDocumentTypeCodes": ["JUSTIFICATIF_DOMICILE"],
  "requirements": [...]
}
```

---

## 12. Document Type Rules Reference

| Type | Number | Issue Date | Expiry Date | Back Side |
|------|--------|-----------|------------|-----------|
| `CNI` | ✅ required | ✅ required | ✅ required | ✅ required |
| `PASSEPORT` | ✅ required | ✅ required | ✅ required | — |
| `NIU` | ✅ required | — | ✅ required | — |
| `JUSTIFICATIF_DOMICILE` | — | — | — | — |
| `REGISTRE_COMMERCE` | ✅ required | ✅ required | — | — |
| `PHOTO_IDENTITE` | — | — | — | — |
