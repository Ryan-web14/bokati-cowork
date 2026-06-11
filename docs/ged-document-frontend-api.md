# GED — Document Management Frontend API

> Base URL: `/sni/api/v1`  
> Auth: Bearer JWT required on all endpoints unless noted otherwise.  
> Content-Type: `application/json` unless endpoint uses `multipart/form-data`.

---

## Table of Contents

1. [Common Patterns](#common-patterns)
2. [Enums Reference](#enums-reference)
3. [Document Types](#document-types)
4. [Document Requirements](#document-requirements)
5. [Documents (Core)](#documents-core)
6. [Document Tags & Metadata](#document-tags--metadata)
7. [Document Signatures](#document-signatures)
8. [Document Review Queue](#document-review-queue)
9. [Document Analytics & Export](#document-analytics--export)
10. [KYC Cases](#kyc-cases)
11. [KYC Document Review](#kyc-document-review)
12. [Document Retention Policies](#document-retention-policies)
13. [RBAC — Required Permissions](#rbac--required-permissions)
14. [Error Codes](#error-codes)

---

## Common Patterns

### Paginated Response

All list endpoints that support pagination return:

```json
{
  "content": [ ...items... ],
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false,
  "empty": false
}
```

Pagination query params (Spring Data Pageable):

| Param | Default | Description |
|-------|---------|-------------|
| `page` | `0` | Zero-based page index |
| `size` | `20` | Items per page |
| `sort` | endpoint-specific | Field + direction, e.g. `uploadedAt,desc` |

### Idempotency Header

Mutating endpoints decorated with `@Idempotent` accept an optional:

```
Idempotency-Key: <uuid>
```

Duplicate requests with the same key return the cached response (HTTP 200) without re-executing.

---

## Enums Reference

### `DocumentStatus`
| Value | Meaning |
|-------|---------|
| `DRAFT` | Not yet submitted |
| `UPLOADED` | File received, not yet sent for review |
| `PENDING_REVIEW` | Awaiting reviewer action |
| `NEEDS_CORRECTION` | Reviewer requested corrections |
| `APPROVED` | Document validated |
| `REJECTED` | Document rejected |
| `SIGNED` | Document has been signed |
| `EXPIRED` | Passed expiry date |
| `ARCHIVED` | Archived (manual or retention policy) |
| `SUPERSEDED` | Replaced by a newer version |

### `DocumentSpace`
| Value | Description |
|-------|-------------|
| `KYC_SPACE` | KYC identity documents |
| `CONTRACT_SPACE` | Contract-related documents |
| `FINANCIAL_SPACE` | Financial documents (invoices, payments) |
| `ASSET_SPACE` | Asset-linked documents |
| `ADMINISTRATIVE` | Administrative documents |
| `GENERIC` | Uncategorised documents |

> The space is **auto-derived** from the document type at upload time. It does not need to be sent explicitly.

### `DocumentCategory`
`KYC` | `LEGAL` | `SYSTEM` | `FINANCIAL` | `ASSET` | `OTHER`

### `DocumentOwnerType`
`CUSTOMER` | `MEMBER` | `BUSINESS` | `CONTRACT` | `INVOICE` | `PAYMENT` | `PROPOSAL` | `ASSET`

### `KycCaseStatus`
`DRAFT` | `PENDING_REVIEW` | `IN_REVIEW` | `APPROVED` | `REJECTED` | `EXPIRED` | `CANCELLED`

### `KycRiskLevel`
`LOW` | `MEDIUM` | `HIGH` | `CRITICAL`

### `KycDocumentVerificationStatus`
`PENDING` | `VERIFIED` | `REJECTED` | `EXPIRED`

### `DocumentRetentionReference`
| Value | Cutoff calculated from |
|-------|----------------------|
| `UPLOAD_DATE` | `uploadedAt + retentionDays` |
| `EXPIRY_DATE` | `expiryDate + retentionDays` |

### `DocumentRetentionAction`
`ARCHIVE` | `PURGE_FLAG`

---

## Document Types

Base path: `/sni/api/v1/document-types`

Document types are the catalog of document kinds that the GED can store. Each type defines constraints (file size, MIME types, whether expiry date is required, etc.).

### `POST /sni/api/v1/document-types`
Create a new document type.

**Request body:**
```json
{
  "code": "PASSPORT",
  "name": "Passport",
  "category": "KYC",
  "ownerType": "CUSTOMER",
  "description": "International travel document",
  "helpText": "Must be valid for at least 6 months",
  "documentDetails": "Pages 1-2 required",
  "required": true,
  "requiresExpiryDate": true,
  "requiresReview": true,
  "requiresSignature": false,
  "multipleAllowed": false,
  "allowedMimeTypes": "application/pdf,image/jpeg,image/png",
  "maxFileSizeBytes": 10485760,
  "active": true
}
```

**Response `201`:** `DocumentTypeResponse`

---

### `PATCH /sni/api/v1/document-types/{code}`
Update an existing document type.

**Path param:** `code` — document type code  
**Request body:** same as `POST`  
**Response `200`:** `DocumentTypeResponse`

---

### `GET /sni/api/v1/document-types/{code}`
Fetch a single document type.

**Response `200`:** `DocumentTypeResponse`

---

### `GET /sni/api/v1/document-types`
List all document types.

| Query param | Type | Description |
|-------------|------|-------------|
| `ownerType` | `DocumentOwnerType` | Filter by applicable owner type |
| `active` | `boolean` | Filter by active status |

**Response `200`:** `List<DocumentTypeResponse>`

---

### `PATCH /sni/api/v1/document-types/{code}/activate`
Activate a deactivated document type.  
**Response `200`:** `DocumentTypeResponse`

### `PATCH /sni/api/v1/document-types/{code}/deactivate`
Deactivate a document type (soft disable — no documents deleted).  
**Response `200`:** `DocumentTypeResponse`

### `DELETE /sni/api/v1/document-types/{code}`
Permanently delete a document type (only if no documents reference it).  
**Response `204`:** No content

---

### `DocumentTypeResponse` model

```json
{
  "code": "PASSPORT",
  "name": "Passport",
  "category": "KYC",
  "ownerType": "CUSTOMER",
  "description": "International travel document",
  "helpText": "Must be valid for at least 6 months",
  "documentDetails": "Pages 1-2 required",
  "required": true,
  "requiresExpiryDate": true,
  "requiresReview": true,
  "requiresSignature": false,
  "multipleAllowed": false,
  "allowedMimeTypes": "application/pdf,image/jpeg,image/png",
  "maxFileSizeBytes": 10485760,
  "active": true
}
```

---

## Document Requirements

Base path: `/sni/api/v1/document-requirements`

Requirements define which document types are mandatory for a given owner type (used by KYC completeness checks).

### `POST /sni/api/v1/document-requirements`
Create a requirement rule.

**Request body:** `DocumentRequirementRequest` — see `DocumentTypeRequest` shape (same fields including `ownerType`, `required`, etc.)  
**Response `201`:** `DocumentRequirementResponse`

### `PATCH /sni/api/v1/document-requirements/{id}`
Update a requirement.  
**Response `200`:** `DocumentRequirementResponse`

### `GET /sni/api/v1/document-requirements/{id}`
Get a requirement by numeric ID.  
**Response `200`:** `DocumentRequirementResponse`

### `GET /sni/api/v1/document-requirements`
List all requirements.

| Query param | Type | Description |
|-------------|------|-------------|
| `ownerType` | `DocumentOwnerType` | Filter by owner type |
| `active` | `boolean` | Filter by active status |

**Response `200`:** `List<DocumentRequirementResponse>`

### `PATCH /sni/api/v1/document-requirements/{id}/activate`
**Response `200`:** `DocumentRequirementResponse`

### `PATCH /sni/api/v1/document-requirements/{id}/deactivate`
**Response `200`:** `DocumentRequirementResponse`

### `DELETE /sni/api/v1/document-requirements/{id}`
**Response `204`:** No content

---

## Documents (Core)

Base path: `/sni/api/v1/documents`

### `POST /sni/api/v1/documents/upload`
Upload a new document.

**Content-Type:** `multipart/form-data`

| Form part | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | binary | yes | Document file |
| `ownerType` | `DocumentOwnerType` | yes | Entity type owning the document |
| `ownerCode` | `string` | yes | Code/reference of the owner entity |
| `documentTypeCode` | `string` | yes | Must exist in document type catalog |
| `title` | `string` | yes | Human-readable title |
| `description` | `string` | no | Optional description |
| `documentNumber` | `string` | no | Document reference number (passport no., etc.) |
| `issueDate` | `date (YYYY-MM-DD)` | no | Issue date |
| `expiryDate` | `date (YYYY-MM-DD)` | no | Required when `documentType.requiresExpiryDate=true` |

**Response `201`:** `DocumentResponse`

> **Note:** The `space` field on the returned document is automatically derived from the document type.

---

### `POST /sni/api/v1/documents/{code}/replace`
Replace the file on an existing document (creates a new version).

**Content-Type:** `multipart/form-data`

| Form part | Type | Required |
|-----------|------|----------|
| `file` | binary | yes |

**Response `200`:** `DocumentResponse`

---

### `GET /sni/api/v1/documents/{code}`
Fetch document details.  
**Response `200`:** `DocumentResponse`

---

### `GET /sni/api/v1/documents`
List documents by owner.

| Query param | Type | Description |
|-------------|------|-------------|
| `ownerType` | `DocumentOwnerType` | Filter by owner type |
| `ownerCode` | `string` | Filter by owner code |
| + pagination | | Default: `size=20, sort=uploadedAt,desc` |

**Response `200`:** `PaginatedResponse<DocumentResponse>`

---

### `GET /sni/api/v1/documents/{code}/versions`
List all versions of a document.  
**Response `200`:** `List<DocumentVersionResponse>`

#### `DocumentVersionResponse`
```json
{
  "versionNumber": 3,
  "fileName": "passport_v3.pdf",
  "fileUrl": "...",
  "fileSize": 245760,
  "mimeType": "application/pdf",
  "checksumSha256": "abc123...",
  "uploadedBy": 42,
  "uploadedAt": "2025-11-01T10:00:00Z",
  "current": true
}
```

---

### `GET /sni/api/v1/documents/{code}/download`
Download the file. Returns raw binary with `Content-Disposition: attachment`.  
Triggers an access log entry (`DOWNLOAD`).  
**Response `200`:** `application/octet-stream` (or actual MIME type)

---

### `GET /sni/api/v1/documents/{code}/preview`
Preview the file inline. Returns raw binary with `Content-Disposition: inline`.  
Triggers an access log entry (`PREVIEW`).  
**Response `200`:** `application/pdf` or `image/*` etc.

---

### `POST /sni/api/v1/documents/{code}/approve`
Approve a document. Requires `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` permission.

**Request body:**
```json
{
  "reviewedBy": 42,
  "comment": "All good"
}
```

**Response `200`:** `DocumentResponse`

> If the document type has `requiresSignature=true`, an outbox event `DOCUMENT_SIGNATURE_REQUIRED` is published automatically after approval.

---

### `POST /sni/api/v1/documents/{code}/reject`
Reject a document. Requires review permission.

**Request body:**
```json
{
  "reviewedBy": 42,
  "comment": "Document is blurry"
}
```

**Response `200`:** `DocumentResponse`

---

### `POST /sni/api/v1/documents/{code}/request-correction`
Ask the owner to correct and resubmit the document.

**Request body:**
```json
{
  "reviewedBy": 42,
  "correctionInstructions": "Please provide full page scan including document edges"
}
```

**Response `200`:** `DocumentResponse`

---

### `POST /sni/api/v1/documents/{code}/archive`
Manually archive a document.

| Query param | Type | Required |
|-------------|------|----------|
| `reason` | `string` | no |

**Response `200`:** `DocumentResponse`

---

### `POST /sni/api/v1/documents/{code}/versions/{versionNumber}/restore`
Restore a previous version as the current version. A new version entry is created (version number = current + 1) with the same file as the target version. Requires review permission.

**Response `200`:** `DocumentResponse`

---

### `GET /sni/api/v1/documents/search`
Full-text / filtered document search.

| Query param | Type | Description |
|-------------|------|-------------|
| `q` | `string` | Free-text search on title, description |
| `space` | `DocumentSpace` | Filter by document space |
| `category` | `DocumentCategory` | Filter by category |
| `tags` | `string[]` | Filter by tag codes (multi-value) |
| `ownerType` | `DocumentOwnerType` | Filter by owner type |
| `ownerCode` | `string` | Filter by owner code |
| `statuses` | `DocumentStatus[]` | Filter by one or more statuses (multi-value) |
| `expiresInDays` | `integer` | Documents expiring within N days |
| `isExpired` | `boolean` | Only expired documents |
| `uploadedAfter` | `date (YYYY-MM-DD)` | Uploaded on or after date |
| `uploadedBefore` | `date (YYYY-MM-DD)` | Uploaded on or before date |
| `metaKey` | `string` | Metadata key to filter on |
| `metaValue` | `string` | Metadata value for `metaKey` |
| + pagination | | Default: `size=20, sort=uploadedAt,desc` |

**Response `200`:** `PaginatedResponse<DocumentResponse>`

---

### `GET /sni/api/v1/documents/dashboard`
Aggregated counts summary.

| Query param | Type | Description |
|-------------|------|-------------|
| `space` | `DocumentSpace` | Optional — scope to one space |

**Response `200`:** `DocumentDashboardResponse`

```json
{
  "totalDocuments": 1250,
  "pendingReview": 45,
  "expiredDocuments": 12,
  "needsCorrection": 8,
  "byStatus": {
    "APPROVED": 980,
    "PENDING_REVIEW": 45,
    "EXPIRED": 12
  },
  "bySpace": {
    "KYC_SPACE": 400,
    "CONTRACT_SPACE": 250
  }
}
```

---

### `GET /sni/api/v1/documents/folder`
Get all documents for a single owner, grouped by document type/space.

| Query param | Type | Required |
|-------------|------|----------|
| `ownerType` | `DocumentOwnerType` | yes |
| `ownerCode` | `string` | yes |

**Response `200`:** `DocumentFolderResponse`

```json
{
  "ownerType": "CUSTOMER",
  "ownerCode": "CUST-001",
  "totalDocuments": 8,
  "completeness": 0.75,
  "bySpace": {
    "KYC_SPACE": [ ...DocumentResponse... ],
    "CONTRACT_SPACE": [ ...DocumentResponse... ]
  },
  "missingRequiredTypes": ["PROOF_OF_ADDRESS"]
}
```

---

### `DocumentResponse` model

```json
{
  "code": "DOC-0001",
  "ownerId": 12,
  "ownerType": "CUSTOMER",
  "category": "KYC",
  "space": "KYC_SPACE",
  "spaceReferenceCode": "CUST-001",
  "documentTypeCode": "PASSPORT",
  "documentTypeName": "Passport",
  "title": "John Doe — Passport",
  "description": null,
  "fileName": "passport.pdf",
  "fileUrl": "/files/...",
  "previewUrl": "/sni/api/v1/documents/DOC-0001/preview",
  "downloadUrl": "/sni/api/v1/documents/DOC-0001/download",
  "fileSize": 245760,
  "mimeType": "application/pdf",
  "checksumSha256": "abc123...",
  "currentVersionNumber": 2,
  "status": "APPROVED",
  "issueDate": "2020-01-15",
  "expiryDate": "2030-01-15",
  "uploadedBy": 42,
  "uploadedAt": "2025-06-01T08:30:00Z",
  "updatedAt": "2025-06-02T14:00:00Z",
  "versions": [ ...DocumentVersionResponse... ],
  "tags": [ ...DocumentTagResponse... ],
  "metadata": {
    "documentNumber": "AB123456",
    "issuingCountry": "CG"
  }
}
```

---

## Document Tags & Metadata

### Tag Catalog

#### `POST /sni/api/v1/document-tags`
Create a new tag.

```json
{
  "code": "URGENT",
  "name": "Urgent",
  "color": "#FF0000",
  "space": "KYC_SPACE"
}
```
**Response `201`:** `DocumentTagResponse`

#### `PUT /sni/api/v1/document-tags/{code}`
Update a tag.  
**Response `200`:** `DocumentTagResponse`

#### `GET /sni/api/v1/document-tags/{code}`
Get a tag by code.  
**Response `200`:** `DocumentTagResponse`

#### `GET /sni/api/v1/document-tags`
List all tags.

| Query param | Type | Description |
|-------------|------|-------------|
| `space` | `DocumentSpace` | Filter by space |

**Response `200`:** `List<DocumentTagResponse>`

#### `DELETE /sni/api/v1/document-tags/{code}`
Delete a tag.  
**Response `204`**

#### `DocumentTagResponse`
```json
{
  "code": "URGENT",
  "name": "Urgent",
  "color": "#FF0000",
  "space": "KYC_SPACE"
}
```

---

### Tag Assignments

#### `POST /sni/api/v1/documents/{code}/tags`
Assign tags to a document.

```json
{
  "tagCodes": ["URGENT", "VERIFIED"]
}
```
**Response `200`:** `List<DocumentTagResponse>` (all tags on the document after operation)

#### `DELETE /sni/api/v1/documents/{code}/tags/{tagCode}`
Remove a tag from a document.  
**Response `204`**

#### `GET /sni/api/v1/documents/{code}/tags`
Get all tags on a document.  
**Response `200`:** `List<DocumentTagResponse>`

---

### Metadata

Key-value metadata attached to any document.

#### `PATCH /sni/api/v1/documents/{code}/metadata`
Set / overwrite metadata entries.

```json
{
  "issuingCountry": "CG",
  "documentNumber": "AB123456"
}
```
**Response `200`:** `Map<String, String>` (full metadata map after update)

#### `GET /sni/api/v1/documents/{code}/metadata`
Get all metadata entries.  
**Response `200`:** `Map<String, String>`

#### `DELETE /sni/api/v1/documents/{code}/metadata/{key}`
Delete a single metadata key.  
**Response `204`**

---

## Document Signatures

Base path: `/sni/api/v1/documents/{documentCode}/signatures`

Signature workflows are triggered automatically when a document type has `requiresSignature=true` and the document is approved. They can also be created manually.

### `POST /sni/api/v1/documents/{documentCode}/signatures`
Request a signature on a document.

```json
{
  "signerUserId": 88,
  "signerName": "Jane Doe",
  "signerEmail": "jane@example.com",
  "role": "APPROVER",
  "expiresAt": "2025-12-31T23:59:59Z"
}
```
**Response `201`:** `DocumentSignatureResponse`

---

### `GET /sni/api/v1/documents/{documentCode}/signatures`
List all signature requests for a document.  
**Response `200`:** `List<DocumentSignatureResponse>`

---

### `PATCH /sni/api/v1/documents/{documentCode}/signatures/{signatureId}/sign`
Record that a signer has signed.

```json
{
  "signedBy": 88,
  "signatureData": "base64encodedSignatureImage",
  "comment": "Signed after review"
}
```
**Response `200`:** `DocumentSignatureResponse`

---

### `DocumentSignatureResponse`
```json
{
  "id": 1,
  "documentCode": "DOC-0001",
  "signerUserId": 88,
  "signerName": "Jane Doe",
  "signerEmail": "jane@example.com",
  "role": "APPROVER",
  "status": "SIGNED",
  "requestedAt": "2025-06-01T09:00:00Z",
  "signedAt": "2025-06-02T11:30:00Z",
  "expiresAt": "2025-12-31T23:59:59Z",
  "ipAddress": "196.10.1.1",
  "userAgent": "Mozilla/5.0..."
}
```

---

## Document Review Queue

Base path: `/sni/api/v1/review`

Requires document review permissions (see [RBAC](#rbac--required-permissions)).

### `GET /sni/api/v1/review/queue`
Global review queue — all documents with status `PENDING_REVIEW` across all spaces.  
Requires `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW`.

| Pagination | Default: `size=20` |
|------------|--------------------|

**Response `200`:** `PaginatedResponse<DocumentResponse>`

---

### `GET /sni/api/v1/review/{space}`
Queue for a specific document space.  
Requires space-specific review permission (`DOCUMENT_REVIEW_{SPACE}` or global `DOCUMENT_REVIEW`).

**Path param:** `space` — one of the `DocumentSpace` values.

**Response `200`:** `PaginatedResponse<DocumentResponse>`

---

### `POST /sni/api/v1/review/{space}/bulk-approve`
Bulk approve multiple documents in a space.  
Requires space review permission.

```json
{
  "codes": ["DOC-0001", "DOC-0002", "DOC-0003"],
  "reviewedBy": 42,
  "comment": "Batch approval after audit"
}
```
**Response `200`:** `DocumentBulkActionResponse`

---

### `POST /sni/api/v1/review/{space}/bulk-reject`
Bulk reject multiple documents.

```json
{
  "codes": ["DOC-0004", "DOC-0005"],
  "reviewedBy": 42,
  "comment": "Insufficient quality",
  "reason": "POOR_QUALITY"
}
```
**Response `200`:** `DocumentBulkActionResponse`

---

### `POST /sni/api/v1/review/{space}/bulk-request-correction`
Bulk request correction on multiple documents.

```json
{
  "codes": ["DOC-0006"],
  "reviewedBy": 42,
  "correctionInstructions": "Rescan at higher resolution"
}
```
**Response `200`:** `DocumentBulkActionResponse`

---

### `DocumentBulkActionResponse`
```json
{
  "processedCount": 3,
  "successCount": 3,
  "failedCount": 0,
  "failedCodes": []
}
```

---

## Document Analytics & Export

### `GET /sni/api/v1/documents/analytics`
GED analytics for a given period. Requires review permission.

| Query param | Type | Default | Description |
|-------------|------|---------|-------------|
| `space` | `DocumentSpace` | null (all spaces) | Scope to one space |
| `months` | `integer` | `12` | Analysis period, clamped 1–24 |

**Response `200`:** `DocumentAnalyticsResponse`

```json
{
  "space": null,
  "periodMonths": 12,
  "totalDocuments": 1500,
  "pendingReview": 45,
  "needsCorrection": 12,
  "expiredDocuments": 30,
  "byStatus": {
    "APPROVED": 1100,
    "PENDING_REVIEW": 45,
    "EXPIRED": 30
  },
  "bySpace": {
    "KYC_SPACE": 600,
    "CONTRACT_SPACE": 300
  },
  "byCategory": {
    "KYC": 600,
    "LEGAL": 300
  },
  "correctionRate30d": 0.08,
  "rejectionRate30d": 0.05,
  "avgReviewTimeHours": 4.2,
  "uploadsByMonth": [
    { "period": "2024-07", "count": 120 },
    { "period": "2024-08", "count": 135 }
  ],
  "approvalsByMonth": [
    { "period": "2024-07", "count": 110 }
  ],
  "rejectionsByMonth": [
    { "period": "2024-07", "count": 6 }
  ],
  "correctionsByMonth": [
    { "period": "2024-07", "count": 10 }
  ]
}
```

**Field definitions:**
- `correctionRate30d` — corrections / total reviews in last 30 days (0.0–1.0)
- `rejectionRate30d` — rejections / total reviews in last 30 days (0.0–1.0)
- `avgReviewTimeHours` — average time from `UPLOADED` → `APPROVED/REJECTED` in last 30 days
- `uploadsByMonth`, `approvalsByMonth`, etc. — time-series with `period` as `"yyyy-MM"` string

---

### `GET /sni/api/v1/documents/export/csv`
Export filtered documents as CSV (UTF-8). Requires review permission.  
Max 10,000 rows.

| Query param | Type | Description |
|-------------|------|-------------|
| `space` | `DocumentSpace` | Filter by space |
| `category` | `DocumentCategory` | Filter by category |
| `tags` | `string[]` | Filter by tag codes |
| `ownerType` | `DocumentOwnerType` | Filter by owner type |
| `ownerCode` | `string` | Filter by owner code |
| `statuses` | `DocumentStatus[]` | Filter by statuses |
| `uploadedAfter` | `date (YYYY-MM-DD)` | |
| `uploadedBefore` | `date (YYYY-MM-DD)` | |

**Response `200`:** `text/csv; charset=UTF-8`  
`Content-Disposition: attachment; filename="documents-export.csv"`

**CSV columns:** `code`, `title`, `ownerType`, `ownerCode`, `documentTypeCode`, `space`, `category`, `status`, `issueDate`, `expiryDate`, `uploadedAt`, `uploadedBy`, `fileName`, `fileSize`, `mimeType`, `currentVersionNumber`

---

### `GET /sni/api/v1/documents/{code}/access-logs`
Access log history for a specific document. Requires review permission.

Pagination: `size=20, sort=accessedAt,desc`

**Response `200`:** `PaginatedResponse<DocumentAccessLogResponse>`

---

### `GET /sni/api/v1/documents/access-logs/by-user`
Access logs for all documents accessed by a specific user. Requires review permission.

| Query param | Type | Required |
|-------------|------|----------|
| `userId` | `long` | yes |

Pagination: `size=20, sort=accessedAt,desc`

**Response `200`:** `PaginatedResponse<DocumentAccessLogResponse>`

---

### `DocumentAccessLogResponse`
```json
{
  "id": 1,
  "documentCode": "DOC-0001",
  "userId": 42,
  "action": "DOWNLOAD",
  "accessedAt": "2025-06-01T14:30:00Z",
  "ipAddress": "196.10.1.1",
  "userAgent": "Mozilla/5.0..."
}
```

**`action` values:** `DOWNLOAD`, `PREVIEW`

---

## KYC Cases

Base path: `/sni/api/v1/kyc/cases`

A KYC case groups all identity documents for an entity (customer, member, business) and tracks the verification lifecycle.

### `POST /sni/api/v1/kyc/cases`
Open a new KYC case.

```json
{
  "ownerType": "CUSTOMER",
  "ownerCode": "CUST-001",
  "ownerId": 12,
  "kycLevel": 2,
  "riskLevel": "MEDIUM"
}
```
**Response `201`:** `KycCaseResponse`

---

### `GET /sni/api/v1/kyc/cases/{code}`
Fetch a KYC case with its documents and requirements status.  
**Response `200`:** `KycCaseResponse`

---

### `GET /sni/api/v1/kyc/cases`
Search / list KYC cases.

| Query param | Type | Description |
|-------------|------|-------------|
| `status` | `KycCaseStatus` | Filter by case status |
| `ownerType` | `DocumentOwnerType` | Filter by owner type |
| `submittedAfter` | `datetime (ISO-8601)` | Submitted on or after |
| `submittedBefore` | `datetime (ISO-8601)` | Submitted on or before |
| `reviewedBy` | `long` | Reviewer user ID |
| `pendingReviewOnly` | `boolean` | Only cases awaiting review |
| `expiringWithinDays` | `integer` | Cases expiring within N days |
| `riskLevel` | `KycRiskLevel` | Filter by risk level |
| + pagination | | Default: `size=20, sort=startedAt` |

**Response `200`:** `PaginatedResponse<KycCaseResponse>`

---

### `GET /sni/api/v1/kyc/cases/dashboard`
KYC operational dashboard metrics.  
**Response `200`:** `KycDashboardResponse`

```json
{
  "generatedAt": "2025-06-10T07:00:00Z",
  "summary": {
    "DRAFT": 20,
    "PENDING_REVIEW": 35,
    "IN_REVIEW": 10,
    "APPROVED": 800,
    "REJECTED": 50,
    "EXPIRED": 15
  },
  "sla": {
    "casesReviewedWithin24h": 420,
    "casesExceeding48h": 8,
    "avgReviewTimeHours": 6.4
  },
  "expiringSoon": {
    "within7Days": 5,
    "within30Days": 22,
    "within60Days": 48
  },
  "byOwnerType": [
    { "ownerType": "CUSTOMER", "count": 700, "pendingReview": 25 }
  ],
  "recentActivity": [
    { "action": "APPROVED", "count": 15, "period": "2025-06-09" }
  ]
}
```

---

### `GET /sni/api/v1/kyc/cases/expiring-soon`
Cases expiring within N days.

| Query param | Default | Description |
|-------------|---------|-------------|
| `days` | `30` | Lookahead in days |
| + pagination | `size=20` | |

**Response `200`:** `PaginatedResponse<KycCaseResponse>`

---

### `GET /sni/api/v1/kyc/cases/my-queue`
Cases assigned to a specific reviewer.

| Query param | Type | Description |
|-------------|------|-------------|
| `userId` | `long` | Reviewer ID (optional — defaults to authenticated user) |
| + pagination | | `size=20` |

**Response `200`:** `PaginatedResponse<KycCaseResponse>`

---

### `POST /sni/api/v1/kyc/cases/{code}/submit`
Submit a case for review (moves from `DRAFT` → `PENDING_REVIEW`).  
**Response `200`:** `KycCaseResponse`

---

### `POST /sni/api/v1/kyc/cases/{code}/approve`
Approve a KYC case.

```json
{
  "reviewedBy": 42,
  "comment": "All documents verified",
  "validUntil": "2027-06-10"
}
```
**Response `200`:** `KycCaseResponse`

---

### `POST /sni/api/v1/kyc/cases/{code}/reject`
Reject a KYC case.

```json
{
  "reviewedBy": 42,
  "comment": "Suspicious document",
  "rejectionReason": "DOCUMENT_MISMATCH"
}
```
**Response `200`:** `KycCaseResponse`

---

### `GET /sni/api/v1/kyc/cases/{code}/missing-requirements`
List document types that are still missing for the case to be complete.

**Response `200`:** `List<KycRequirementStatus>`

```json
[
  {
    "documentTypeCode": "PROOF_OF_ADDRESS",
    "documentTypeName": "Proof of Address",
    "required": true,
    "satisfied": false,
    "documentCode": null
  }
]
```

---

### `PATCH /sni/api/v1/kyc/cases/{code}/assign`
Assign a case to a reviewer.

```json
{
  "assignedTo": 55,
  "note": "Assigned for level-2 review"
}
```
**Response `200`:** `KycCaseResponse`

---

### `POST /sni/api/v1/kyc/cases/{code}/notes`
Add an internal note to a case.

```json
{
  "content": "Contacted customer for additional documents",
  "isInternal": true
}
```
**Response `201`:** `KycCaseNoteResponse`

```json
{
  "id": 1,
  "caseCode": "KYC-0001",
  "content": "Contacted customer...",
  "isInternal": true,
  "authorId": 42,
  "createdAt": "2025-06-10T09:00:00Z"
}
```

---

### `GET /sni/api/v1/kyc/cases/{code}/notes`
List all notes on a case (internal + public).  
**Response `200`:** `List<KycCaseNoteResponse>`

---

### `GET /sni/api/v1/kyc/cases/{code}/timeline`
Full audit timeline for a case.

**Response `200`:** `List<KycTimelineEntryResponse>`

```json
[
  {
    "event": "CASE_SUBMITTED",
    "description": "Case submitted for review",
    "actorId": 12,
    "occurredAt": "2025-06-01T10:00:00Z",
    "metadata": {}
  }
]
```

---

### `GET /sni/api/v1/kyc/cases/{code}/expiry-status`
Document-level expiry status for each document in the case.

**Response `200`:** `List<KycExpiryDocumentStatus>`

```json
[
  {
    "documentTypeCode": "PASSPORT",
    "documentCode": "DOC-0001",
    "expiryDate": "2030-01-15",
    "daysUntilExpiry": 1680,
    "status": "VALID"
  }
]
```

**`status` values:** `VALID`, `EXPIRING_SOON`, `EXPIRED`

---

### `PATCH /sni/api/v1/kyc/cases/{code}/risk-level`
Update the risk level of a KYC case.

```json
{
  "riskLevel": "HIGH",
  "justification": "Multiple document anomalies detected"
}
```
**Response `200`:** `KycCaseResponse`

---

### `GET /sni/api/v1/kyc/cases/{code}/export/pdf`
Export the full KYC dossier as a PDF.

| Query param | Default | Description |
|-------------|---------|-------------|
| `includeInternalNotes` | `true` | Include internal reviewer notes in PDF |

**Response `200`:** `application/pdf`  
`Content-Disposition: attachment; filename="kyc-{code}.pdf"`

---

### `KycCaseResponse` model

```json
{
  "code": "KYC-0001",
  "ownerName": "John Doe",
  "ownerCode": "CUST-001",
  "ownerType": "CUSTOMER",
  "ownerId": 12,
  "status": "PENDING_REVIEW",
  "startedAt": "2025-05-01T08:00:00Z",
  "submittedAt": "2025-05-15T10:00:00Z",
  "completedAt": null,
  "reviewedBy": null,
  "reviewedAt": null,
  "decisionComment": null,
  "assignedTo": 55,
  "assignedAt": "2025-05-15T11:00:00Z",
  "slaDeadline": "2025-05-17T10:00:00Z",
  "lastReminderSentAt": null,
  "reminderCount": 0,
  "riskLevel": "LOW",
  "kycLevel": 1,
  "complete": false,
  "approved": false,
  "missingDocumentTypeCodes": ["PROOF_OF_ADDRESS"],
  "requirements": [ ...KycRequirementStatus... ],
  "documents": [ ...KycDocumentResponse... ]
}
```

---

## KYC Document Review

Base path: `/sni/api/v1/kyc/documents`

Endpoints focused on reviewing individual KYC documents (not the parent case).

### `POST /sni/api/v1/kyc/documents/bulk-approve`
Bulk approve KYC documents.

```json
{
  "documentIds": [10, 11, 12],
  "reviewedBy": 42,
  "comment": "All verified"
}
```
**Response `200`:** `KycBulkActionResponse`

---

### `POST /sni/api/v1/kyc/documents/bulk-reject`
Bulk reject KYC documents. Also accessible as `/bulk-disapprove` or `/bulk-desapprove`.

```json
{
  "documentIds": [13, 14],
  "reviewedBy": 42,
  "comment": "Image quality too low",
  "rejectionReason": "POOR_QUALITY"
}
```
**Response `200`:** `KycBulkActionResponse`

---

### `GET /sni/api/v1/kyc/documents/review-queue`
All KYC documents currently pending review.  
**Response `200`:** `List<KycDocumentResponse>`

---

### `GET /sni/api/v1/kyc/documents/reviewed`
All reviewed KYC documents. Filter with `?status=APPROVED` or `?status=REJECTED`.

| Query param | Description |
|-------------|-------------|
| `status` | `APPROVED`/`VERIFIED` or `REJECTED`/`DISAPPROVED`/`DESAPPROVED` (case-insensitive). Default: both. |

**Response `200`:** `List<KycDocumentResponse>`

---

### `GET /sni/api/v1/kyc/documents/approved`
Shortcut for `?status=APPROVED`.  
**Response `200`:** `List<KycDocumentResponse>`

---

### `GET /sni/api/v1/kyc/documents/rejected`
Shortcut for `?status=REJECTED`. Also accessible as `/disapproved` or `/desapproved`.  
**Response `200`:** `List<KycDocumentResponse>`

---

### `GET /sni/api/v1/kyc/documents/{documentCode}/ocr-result`
OCR extraction results for a KYC document.

**Response `200`:** `KycDocumentOcrResultResponse`

```json
{
  "documentCode": "DOC-0001",
  "extractedFields": {
    "surname": "DOE",
    "givenNames": "JOHN",
    "nationality": "COG",
    "dateOfBirth": "1985-03-22",
    "documentNumber": "AB123456",
    "expiryDate": "2030-01-15"
  },
  "confidence": 0.94,
  "processedAt": "2025-06-01T09:05:00Z",
  "rawText": "..."
}
```

---

### `KycBulkActionResponse`
```json
{
  "processedCount": 3,
  "successCount": 2,
  "failedCount": 1,
  "failedIds": [14]
}
```

---

## Document Retention Policies

Base path: `/sni/api/v1/document-retention-policies`

All endpoints require `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` permission.

Retention policies define automated lifecycle rules: documents matching a policy's criteria are archived (or flagged for purge) after a configurable number of days relative to upload or expiry date.

The retention worker runs daily at 02:30 UTC. It can be enabled/disabled via `app.document.retention.enabled` and the schedule overridden via `app.document.retention.cron`.

### `GET /sni/api/v1/document-retention-policies`
List all active retention policies.  
**Response `200`:** `List<DocumentRetentionPolicyResponse>`

---

### `GET /sni/api/v1/document-retention-policies/{code}`
Get a specific retention policy.  
**Response `200`:** `DocumentRetentionPolicyResponse`

---

### `POST /sni/api/v1/document-retention-policies`
Create a new retention policy.

```json
{
  "code": "KYC-3YR",
  "name": "KYC Documents — 3 Year Retention",
  "space": "KYC_SPACE",
  "documentTypeCode": null,
  "retentionDays": 1095,
  "retentionReference": "UPLOAD_DATE",
  "action": "ARCHIVE"
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `code` | yes | Unique identifier |
| `name` | yes | Human-readable name |
| `space` | no | Scope to one document space. Omit for global policy. |
| `documentTypeCode` | no | Scope to a specific document type. |
| `retentionDays` | yes | Days (≥ 1) before action is triggered |
| `retentionReference` | no | `UPLOAD_DATE` (default) or `EXPIRY_DATE` |
| `action` | no | `ARCHIVE` (default) or `PURGE_FLAG` |

**Response `201`:** `DocumentRetentionPolicyResponse`

---

### `PUT /sni/api/v1/document-retention-policies/{code}`
Replace a retention policy.  
**Request body:** same as `POST`  
**Response `200`:** `DocumentRetentionPolicyResponse`

---

### `DELETE /sni/api/v1/document-retention-policies/{code}`
Deactivate a retention policy (soft delete — existing archived docs are not affected).  
**Response `204`**

---

### `DocumentRetentionPolicyResponse`
```json
{
  "id": 1,
  "code": "KYC-3YR",
  "name": "KYC Documents — 3 Year Retention",
  "space": "KYC_SPACE",
  "documentTypeCode": null,
  "retentionDays": 1095,
  "retentionReference": "UPLOAD_DATE",
  "action": "ARCHIVE",
  "active": true,
  "createdAt": "2025-06-01T00:00:00Z",
  "createdBy": 1
}
```

---

## RBAC — Required Permissions

### Permission names

| Permission | Stored as (DB `name`) | Full authority string |
|------------|-----------------------|----------------------|
| Global document review | `DOCUMENT_REVIEW` | `DOCUMENT:REVIEW` |
| KYC space review | `DOCUMENT_REVIEW_KYC_SPACE` | `DOCUMENT:REVIEW_KYC_SPACE` |
| Contract space review | `DOCUMENT_REVIEW_CONTRACT_SPACE` | `DOCUMENT:REVIEW_CONTRACT_SPACE` |
| Financial space review | `DOCUMENT_REVIEW_FINANCIAL_SPACE` | `DOCUMENT:REVIEW_FINANCIAL_SPACE` |
| Asset space review | `DOCUMENT_REVIEW_ASSET_SPACE` | `DOCUMENT:REVIEW_ASSET_SPACE` |
| Administrative space review | `DOCUMENT_REVIEW_ADMINISTRATIVE` | `DOCUMENT:REVIEW_ADMINISTRATIVE` |
| Generic space review | `DOCUMENT_REVIEW_GENERIC` | `DOCUMENT:REVIEW_GENERIC` |

> A user with `DOCUMENT_REVIEW` (global) can act on **all** spaces.  
> A user with `DOCUMENT_REVIEW_KYC_SPACE` can only act on `KYC_SPACE` documents.

### Endpoint permission map

| Endpoint | Required Permission |
|----------|-------------------|
| `POST /documents/{code}/approve` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `POST /documents/{code}/reject` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `POST /documents/{code}/request-correction` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `POST /documents/{code}/versions/{n}/restore` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `GET /documents/analytics` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `GET /documents/export/csv` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `GET /documents/{code}/access-logs` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `GET /documents/access-logs/by-user` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `GET /review/queue` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |
| `GET /review/{space}` | Global or space-specific review permission |
| `POST /review/{space}/bulk-*` | Global or space-specific review permission |
| All `/document-retention-policies` | `DOCUMENT:REVIEW` or `DOCUMENT_REVIEW` |

> All other document endpoints (upload, list, search, download, preview, tags, metadata, signatures, folder, dashboard, KYC) are accessible to authenticated users without specific document review permissions.

---

## Error Codes

All errors follow the standard API error envelope:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2025-06-10T07:00:00Z",
  "path": "/sni/api/v1/documents/upload"
}
```

| HTTP Status | Typical cause |
|-------------|--------------|
| `400` | Validation failure (missing required field, invalid enum value) |
| `401` | Missing or expired JWT |
| `403` | Authenticated but lacking required permission |
| `404` | Resource not found (invalid code, no such document/case/type) |
| `409` | Idempotency conflict or duplicate code on creation |
| `413` | Uploaded file exceeds `maxFileSizeBytes` for the document type |
| `422` | Business logic violation (e.g., approving an already-approved document) |
| `500` | Unexpected server error |

---

*Last updated: 2026-06-10 — covers V142–V143 migrations (document review permissions + retention policy table).*
