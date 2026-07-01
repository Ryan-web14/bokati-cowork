# KYC Document Upload — Admin Guide

Base URL: `/sni/api/v1`

All admin endpoints require a valid JWT with appropriate admin privileges.

---

## 1. Get Upload Configuration for a Document Type

Before building any upload form, fetch the config for the target document type so you know which fields to display and which MIME types to accept.

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

### Field Reference
| Field | Type | Meaning |
|-------|------|---------|
| `requiresDocumentNumber` | boolean | Show/require document number field |
| `requiresIssueDate` | boolean | Show/require date d'émission |
| `requiresExpiryDate` | boolean | Show/require date d'expiration |
| `requiresBackSide` | boolean | A second upload (BACK) is needed |
| `allowedMimeTypes` | string[] | Accepted MIME types for `<input accept>` |
| `maxFileSizeBytes` | number | Max upload size in bytes |

> **Admin rule:** fields flagged `requires*` are recommended but NOT enforced server-side. Missing values produce a warning in the server log only.

---

## 2. List Document Types

```
GET /document-types?ownerType=MEMBER&active=true
```

Query params:
- `ownerType` (optional): `MEMBER` | `CUSTOMER` | `BUSINESS`
- `active` (optional): `true` | `false`

---

## 3. Upload a KYC Document on Behalf of an Owner

```
POST /kyc/documents/admin/upload
Content-Type: multipart/form-data
```

### Form Fields

| Field | Required | Description |
|-------|----------|-------------|
| `ownerType` | ✅ | `MEMBER` \| `CUSTOMER` \| `BUSINESS` |
| `ownerCode` | ✅ | The owner's unique code (e.g. `MBR-0001`) |
| `documentTypeCode` | ✅ | e.g. `CNI`, `PASSEPORT`, `NIU` |
| `side` | — | `FRONT` (default) or `BACK` |
| `documentNumber` | — | Document number (recommended when `requiresDocumentNumber=true`) |
| `issueDate` | — | `yyyy-MM-dd` (recommended when `requiresIssueDate=true`) |
| `expiryDate` | — | `yyyy-MM-dd` (recommended when `requiresExpiryDate=true`) |
| `file` | ✅ | The document file |

### Upload Flow for Two-Sided Documents (e.g. CNI)

1. Upload FRONT → `side=FRONT` — creates the KYC document entry
2. Upload BACK → `side=BACK` — attaches the back file to the existing entry

The BACK upload will fail with `400` if no FRONT has been uploaded yet for the same owner + type.

### Response `201 Created`
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
  "uploadedBy": "admin@cowork.com",
  "uploadedAt": "2026-07-01T10:30:00Z"
}
```

### Errors
| Status | Cause |
|--------|-------|
| `400` | Missing file, unknown document type, BACK before FRONT, no KYC case exists for owner |
| `403` | Insufficient admin privileges |
| `404` | Owner not found |

---

## 4. Enrich Document Metadata After Upload

Use this when you upload a document first and fill in details after examining the physical document.

```
PATCH /kyc/documents/admin/{kycDocumentId}/details
Content-Type: application/json
```

### Path Parameter
| Param | Description |
|-------|-------------|
| `kycDocumentId` | The `id` returned by the upload endpoint |

### Request Body
```json
{
  "documentNumber": "123456789",
  "issueDate": "2020-01-15",
  "expiryDate": "2030-01-14"
}
```
All fields are optional — only provided fields are updated.

### Response `200 OK`
Same structure as upload response.

---

## 5. Review Queue & Bulk Actions

```
GET  /kyc/documents/review-queue      — paginated PENDING_REVIEW documents
GET  /kyc/documents/approved          — approved documents
GET  /kyc/documents/rejected          — rejected documents
POST /kyc/documents/bulk-approve      — approve multiple
POST /kyc/documents/bulk-reject       — reject multiple
GET  /kyc/documents/{code}/ocr-result — OCR extraction result
```

See the existing KYC documentation for full review workflow details.

---

## 6. Document Type Management

```
POST   /document-types              — create
PATCH  /document-types/{code}       — update
GET    /document-types/{code}       — get one
GET    /document-types              — list
PATCH  /document-types/{code}/activate
PATCH  /document-types/{code}/deactivate
DELETE /document-types/{code}
```

### Key Fields in Create/Update Request

```json
{
  "code": "CNI",
  "name": "Carte Nationale d'Identité",
  "category": "KYC",
  "ownerType": "MEMBER",
  "requiresDocumentNumber": true,
  "requiresIssueDate": true,
  "requiresExpiryDate": true,
  "requiresBackSide": true,
  "autoApprove": false,
  "autoApproveAfterDays": null,
  "allowedMimeTypes": "application/pdf,image/jpeg,image/png",
  "maxFileSizeBytes": 10485760
}
```

---

## 7. Auto-Approve Behavior

Document types with `autoApprove=true` and `autoApproveAfterDays > 0` are automatically moved from `PENDING_REVIEW` → `APPROVED` by the hourly background worker once the configured number of days has elapsed since upload.

Current auto-approve types (from seed):
| Type | Days |
|------|------|
| `JUSTIFICATIF_DOMICILE` | 3 |
| `PHOTO_IDENTITE` | 2 |
