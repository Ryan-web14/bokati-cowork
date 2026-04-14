# Contract, Document And KYC Module Specification

## Objective

This document defines the target functional and technical scope for:

- contract drafting and lifecycle
- legal document management
- KYC document intake and review
- member, customer and business compliance validation
- physical file storage and security controls

The goal is to make the module complete enough to support:

- member onboarding
- customer onboarding
- business onboarding
- contract preparation and signature flow
- regulatory and internal compliance review
- document correction and re-submission
- long-term archival and auditability

## Scope

The module must cover these domains:

- `document-master`
  - document types
  - document requirements
  - review and signature metadata
- `kyc`
  - member KYC
  - customer KYC
  - business KYC
- `legal-document`
  - generated contracts
  - manually uploaded legal files
  - signed copies
- `contract`
  - contract draft
  - review
  - approval
  - signature
  - activation
  - renewal
  - termination

## Main Business Rules

### 1. Owner Types

All documents must be attached to one logical owner:

- `MEMBER`
- `CUSTOMER`
- `BUSINESS`
- `CONTRACT`

Notes:

- `MEMBER` is the primary natural person record.
- `CUSTOMER` can be a person or a company account.
- `BUSINESS` represents the legal business entity.
- `CONTRACT` stores contract documents and signed versions.

### 2. KYC Principles

KYC must support:

- required documents by owner type
- optional documents
- document expiry
- verification status
- correction workflow
- reviewer comments and rejection reasons

For a newly created member:

- initial member status remains `PENDING`
- status becomes `ACTIVE` only after required KYC is validated
- if KYC is rejected, status becomes `REJECTED` or remains `PENDING_CORRECTION` depending on your policy
- reviewer must provide at least one rejection reason

Recommended policy:

- `PENDING` after creation
- `PENDING_KYC` once account exists but documents are incomplete
- `UNDER_REVIEW` once all required documents are submitted
- `ACTIVE` after approval
- `REJECTED` if onboarding is rejected definitively
- `PENDING_CORRECTION` if resubmission is allowed

### 3. Business Compliance

For a business, compliance must support at least:

- `RCCM`
- `NIU`
- company statutes if required
- tax certificate if required
- proof of address
- representative identity document
- representative authorization or power of attorney if required

A business KYC file is compliant only if:

- all mandatory document types are present
- all mandatory documents are approved
- no mandatory approved document is expired
- required identifiers like `NIU` and `RCCM` pass validation rules

### 4. Correction Workflow

When a reviewer rejects a document or a full KYC case:

- a clear reason must be stored
- rejection can target:
  - one specific document
  - one group of documents
  - the full KYC case
- the user must be able to upload a replacement version
- old versions must remain archived and auditable

### 5. Versioning

Each uploaded document must support versioning.

Rules:

- a document logical record has many file versions
- only one version is the current active version
- old versions remain readable for audit
- replacing a file must never overwrite the binary irreversibly
- reviews must reference the exact reviewed version

## Target Domain Model

### 1. DocumentType

Purpose:

- defines what kind of document is expected

Core fields:

- `code`
- `name`
- `category`
- `ownerType`
- `description`
- `required`
- `requiresExpiryDate`
- `requiresReview`
- `requiresSignature`
- `multipleAllowed`
- `allowedMimeTypes`
- `maxFileSizeBytes`
- `validityDurationDays`
- `active`

Examples:

- `MEMBER_ID_CARD`
- `MEMBER_PASSPORT`
- `MEMBER_PROOF_OF_ADDRESS`
- `BUSINESS_RCCM`
- `BUSINESS_NIU`
- `BUSINESS_STATUTES`
- `BUSINESS_TAX_CERTIFICATE`
- `CONTRACT_MEMBERSHIP`
- `CONTRACT_ADDENDUM`

### 2. DocumentRequirement

Purpose:

- defines required documents by owner type and context

Recommended extra fields:

- `ownerType`
- `documentTypeCode`
- `required`
- `active`
- `customerType`
- `businessLegalForm`
- `countryCode`
- `appliesFrom`
- `appliesTo`

This allows future rules such as:

- some documents required only for companies
- some documents required only in a given country
- some documents required only for a given business legal form

### 3. Document

Purpose:

- logical document record

Core fields:

- `code`
- `ownerId`
- `ownerType`
- `category`
- `documentTypeId`
- `title`
- `description`
- `status`
- `currentVersion`
- `issueDate`
- `expiryDate`
- `uploadedBy`
- `uploadedAt`
- `updatedAt`
- `deleted`

Recommended statuses:

- `DRAFT`
- `UPLOADED`
- `UNDER_REVIEW`
- `APPROVED`
- `REJECTED`
- `EXPIRED`
- `ARCHIVED`
- `SUPERSEDED`

### 4. DocumentVersion

Purpose:

- stores each uploaded binary version

Core fields:

- `documentId`
- `versionNumber`
- `storageProvider`
- `storageBucket`
- `storageKey`
- `originalFileName`
- `storedFileName`
- `mimeTypeDeclared`
- `mimeTypeDetected`
- `fileExtension`
- `checksumSha256`
- `fileSizeBytes`
- `uploadStatus`
- `antivirusStatus`
- `uploadedBy`
- `uploadedAt`
- `isCurrent`

Recommended upload statuses:

- `PENDING_SCAN`
- `READY`
- `QUARANTINED`
- `REJECTED`
- `DELETED`

Recommended antivirus statuses:

- `PENDING`
- `CLEAN`
- `INFECTED`
- `ERROR`

### 5. DocumentReview

Purpose:

- stores reviewer action on a document version or KYC case

Core fields:

- `documentId`
- `documentVersionId`
- `reviewStatus`
- `reviewedBy`
- `reviewedAt`
- `comment`
- `rejectionReasonCode`
- `rejectionReasonDetail`

Recommended review statuses:

- `PENDING`
- `APPROVED`
- `REJECTED`
- `NEEDS_CORRECTION`

### 6. KycCase

Purpose:

- groups all KYC documents and review decisions for one owner

Core fields:

- `code`
- `ownerType`
- `ownerId`
- `status`
- `startedAt`
- `submittedAt`
- `completedAt`
- `reviewedBy`
- `reviewedAt`
- `decisionComment`

Recommended statuses:

- `NOT_STARTED`
- `IN_PROGRESS`
- `SUBMITTED`
- `UNDER_REVIEW`
- `APPROVED`
- `REJECTED`
- `PENDING_CORRECTION`
- `EXPIRED`

### 7. KycCaseItem

Purpose:

- tracks each required document in one KYC case

Core fields:

- `kycCaseId`
- `documentTypeCode`
- `required`
- `documentId`
- `status`
- `lastReviewStatus`
- `isSatisfied`

### 8. Contract

Purpose:

- lifecycle of a commercial/legal contract

Core fields:

- `contractCode`
- `ownerType`
- `ownerId`
- `businessId`
- `templateCode`
- `status`
- `effectiveDate`
- `startDate`
- `endDate`
- `renewalType`
- `signedAt`
- `activatedAt`
- `terminatedAt`
- `terminationReason`

Recommended contract statuses:

- `DRAFT`
- `GENERATED`
- `UNDER_REVIEW`
- `AWAITING_SIGNATURE`
- `SIGNED`
- `ACTIVE`
- `SUSPENDED`
- `EXPIRED`
- `TERMINATED`
- `CANCELLED`

### 9. ContractParty

Purpose:

- formalizes who signs and in which role

Core fields:

- `contractId`
- `partyType`
- `partyId`
- `role`
- `signOrder`
- `mustSign`

### 10. ContractTemplate

Purpose:

- reusable contract body with variables

Core fields:

- `code`
- `name`
- `version`
- `language`
- `bodyTemplate`
- `active`

## Required Document Sets

### Member KYC

Recommended mandatory documents:

- identity document
  - national ID card or passport
- selfie or live verification if required
- proof of address if required

Optional:

- residence permit
- secondary contact proof

### Customer KYC

If customer is a person:

- same set as member when needed

If customer is a company:

- representative identity document
- proof of authorization
- company proof of address
- `RCCM`
- `NIU`

### Business KYC

Recommended mandatory documents:

- `RCCM`
- `NIU`
- statutes
- representative identity document
- representative authorization document
- tax document if required
- proof of business address

Optional:

- bank account certificate
- insurance certificate
- sector license

### Contract Documents

Recommended document types:

- draft contract
- reviewed contract
- signed contract
- amendment
- annex
- termination letter

## Key Workflows

### 1. Member Onboarding

1. Member is created with status `PENDING`.
2. System creates `KycCase` for the member.
3. Frontend fetches required document list.
4. User uploads required documents.
5. Each upload creates:
   - `Document`
   - `DocumentVersion`
   - `KycCaseItem` link if needed
6. Files pass MIME and antivirus checks.
7. When all required documents are present, KYC case becomes `SUBMITTED`.
8. Reviewer validates or rejects.
9. If approved:
   - `KycCase.status = APPROVED`
   - `Member.status = ACTIVE`
10. If rejected:
   - `KycCase.status = PENDING_CORRECTION` or `REJECTED`
   - rejection reasons become visible to frontend

### 2. Business Onboarding

1. Business entity is created.
2. System creates business KYC case.
3. Required document set is computed by business legal form and country.
4. User uploads `RCCM`, `NIU`, statutes and others.
5. Compliance team reviews.
6. Business KYC is approved only if all required items are approved and non-expired.

### 3. Contract Drafting

1. Staff selects template and owner.
2. System resolves variables:
   - member/customer/business info
   - pricing and subscription data
   - effective dates
3. Draft contract is rendered.
4. Draft PDF is generated and stored as a `Document`.
5. Review step optionally validates legal wording.
6. Contract moves to signature.
7. Signed version is uploaded or generated from e-signature provider.
8. Contract becomes `ACTIVE`.

### 4. Correction And Re-Submission

1. Reviewer rejects document with reason code and comment.
2. Current version stays archived, not deleted.
3. User uploads replacement.
4. New `DocumentVersion` becomes current.
5. Review restarts on the new version only.

## API Surface Recommendation

### Document Master

- `GET /document-types`
- `POST /document-types`
- `PATCH /document-types/{code}`
- `GET /document-requirements`
- `POST /document-requirements`
- `PATCH /document-requirements/{id}`

### Upload And Storage

- `POST /documents/upload/init`
  - returns upload policy or presigned URL
- `POST /documents/upload/complete`
  - validates uploaded object metadata and creates DB records
- `GET /documents/{code}`
- `GET /documents/{code}/versions`
- `GET /documents/{code}/download`
- `POST /documents/{code}/replace`

### Review

- `POST /documents/{code}/submit-review`
- `POST /documents/{code}/approve`
- `POST /documents/{code}/reject`
- `GET /documents/review-queue`

### KYC

- `POST /kyc/cases`
- `GET /kyc/cases/{code}`
- `GET /kyc/cases/{code}/requirements`
- `GET /kyc/cases/{code}/documents`
- `POST /kyc/cases/{code}/submit`
- `POST /kyc/cases/{code}/approve`
- `POST /kyc/cases/{code}/reject`
- `GET /kyc/cases/review-queue`

### Contract

- `POST /contracts`
- `POST /contracts/{code}/generate-draft`
- `POST /contracts/{code}/submit-review`
- `POST /contracts/{code}/approve`
- `POST /contracts/{code}/send-for-signature`
- `POST /contracts/{code}/mark-signed`
- `POST /contracts/{code}/activate`
- `POST /contracts/{code}/terminate`
- `GET /contracts/{code}`
- `GET /contracts`

## Frontend Requirements

The frontend should support:

- dynamic required document checklist
- drag-and-drop upload
- upload progress
- preview for PDF and images
- document rejection reason display
- re-upload of corrected versions
- KYC case progress bar
- contract template selection
- contract preview before generation
- signature status tracking
- expiry alerts for expiring documents

Recommended screens:

- member KYC dashboard
- customer KYC dashboard
- business KYC dashboard
- compliance review queue
- contract list
- contract detail
- contract draft preview
- document detail with version history

## Physical File Storage Options

### Recommended Option A: Object Storage

Best default choice:

- AWS S3
- Cloudflare R2
- MinIO
- Azure Blob Storage
- Google Cloud Storage

Why:

- scalable
- cheaper than DB binary storage
- supports presigned upload/download
- easy versioning and lifecycle rules
- works well with antivirus scanning pipeline

Recommended storage layout:

- bucket: `bokati-documents`
- key format:
  - `env/{ownerType}/{ownerCode}/{documentCode}/v{version}/{uuid}.{ext}`

Example:

- `prod/MEMBER/MEM-00012/DOC-00045/v3/0f8c...pdf`

### Option B: Local Or Mounted Disk

Useful only for early environments:

- local dev
- single-node staging

Not recommended for production because:

- hard to scale
- weak redundancy
- harder backup and restore
- more complex deployment and failover

### Option C: Database BLOB

Not recommended as the primary store for this project.

Why:

- larger DB size
- slower backups
- worse horizontal scalability
- harder CDN integration

Possible use:

- tiny documents only
- highly restricted systems

## Upload Security

### MIME Verification

Do not trust the MIME type sent by the browser.

Store both:

- declared MIME type from client
- detected MIME type from server-side file inspection

Recommended validation:

- allow only whitelisted formats
- detect actual MIME by file signature
- reject mismatched extension and MIME

Recommended allowed types:

- `application/pdf`
- `image/jpeg`
- `image/png`
- optionally `image/webp`

Avoid by default:

- `application/msword`
- macro-enabled Office files
- archives like `.zip` for KYC unless strictly required

### Antivirus Scanning

Recommended approach:

1. upload file to temporary quarantine location
2. trigger antivirus scan
3. only mark document version `READY` if scan is clean
4. if infected:
   - mark version `QUARANTINED` or `REJECTED`
   - block download and review

Recommended engines:

- ClamAV for first implementation
- external malware scanning service later if volume grows

Recommended architecture:

- upload service stores file in `quarantine/`
- async scanner job reads object
- scanner updates `DocumentVersion.antivirusStatus`
- clean files are moved or promoted to `safe/`

### File Integrity

Compute and store:

- `sha256`
- file size

Use checksum for:

- duplicate detection
- tamper detection
- auditability

## Versioning Strategy

Versioning must exist at two levels.

### 1. Business Versioning

Every document replacement creates:

- same logical `Document`
- new `DocumentVersion`

This preserves history while keeping one stable document reference for the frontend.

### 2. Storage-Level Versioning

If object storage supports native versioning, enable it too.

Why:

- protection against accidental overwrite
- recovery support

But the application must still keep explicit business versions in DB.

Do not rely only on bucket-native versioning.

## Contract Generation Strategy

Recommended generation pipeline:

1. Contract template stored in DB or versioned template files.
2. Variables resolved from:
   - member
   - customer
   - business
   - commercial offer
3. HTML is rendered.
4. HTML is converted to PDF.
5. PDF is stored as a `DocumentVersion`.
6. Contract record links to that generated document.

Recommended tools:

- server-side HTML templates
- OpenHTMLtoPDF, Flying Saucer, or headless Chromium based renderer

For advanced legal formatting:

- keep template versioned
- freeze rendered content at signing time

## Compliance Decision Engine

The backend should expose a computed compliance summary.

For any owner:

- `isComplete`
- `isUnderReview`
- `isApproved`
- `missingDocuments`
- `rejectedDocuments`
- `expiredDocuments`
- `nextExpiryDate`

This summary should drive frontend badges and eligibility rules.

## Status Synchronization Rules

### Member

- create => `PENDING`
- KYC submitted => `UNDER_REVIEW` or stay `PENDING`
- KYC approved => `ACTIVE`
- KYC rejected for correction => `PENDING_CORRECTION`
- KYC definitively rejected => `REJECTED`

### Customer

Recommended:

- keep customer account active independently
- expose separate `kycStatus`
- if needed, block sensitive operations until KYC is approved

### Business

Recommended:

- `businessStatus`
- `complianceStatus`

Do not mix operational business status and compliance status in one field.

## Audit Requirements

Every important action must be auditable:

- upload
- replace version
- approve
- reject
- correction request
- submit KYC case
- generate contract
- sign contract
- activate contract

Audit metadata:

- actor
- timestamp
- IP if user-facing
- previous status
- new status
- reason/comment

## Recommended Implementation Order

### Phase 1

- complete `document-master`
- add `DocumentVersion`
- add secure upload flow
- add MIME and checksum validation
- add ClamAV scanning

### Phase 2

- complete `KycCase` and `KycCaseItem`
- member KYC activation flow
- customer KYC flow
- business KYC flow

### Phase 3

- contract templates
- draft generation
- review and approval flow
- signed document management

### Phase 4

- e-signature integration
- advanced compliance rules
- document expiry reminders
- archival and retention policies

## Recommended Technical Decisions

### Final Recommendations

- use object storage as the primary binary store
- keep metadata and business versions in PostgreSQL
- use quarantine plus async antivirus scan
- verify real MIME type server-side
- implement explicit `DocumentVersion`
- implement `KycCase` and `KycCaseItem`
- separate compliance status from business status where possible
- never overwrite old files
- keep reviewer reason codes and free-text comments

### First Storage Choice For This Project

Recommended production target:

- `MinIO` if self-hosted
- `S3` or `R2` if managed cloud is acceptable

Recommended local/dev target:

- local MinIO container

This gives:

- same programming model in dev and prod
- easy presigned upload
- easy bucket policy management
- simpler migration than local disk

