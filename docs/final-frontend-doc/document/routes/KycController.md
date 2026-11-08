# KycController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 24 route(s).

## Routes

<a id="r-get-sni-api-v1-kyc-cases"></a>
### GET /sni/api/v1/kyc/cases

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `PaginatedResponse<KycCaseResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [KycCaseResponse](#m-KycCaseResponse), [DocumentOwnerType](#m-DocumentOwnerType), [KycRiskLevel](#m-KycRiskLevel)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `String` |  |  |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `submittedAfter` | requête | `Instant` |  |  |
| `submittedBefore` | requête | `Instant` |  |  |
| `reviewedBy` | requête | `Long` |  |  |
| `pendingReviewOnly` | requête | `Boolean` |  |  |
| `expiringWithinDays` | requête | `Integer` |  |  |
| `riskLevel` | requête | `KycRiskLevel` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=startedAt,ASC |

<a id="r-post-sni-api-v1-kyc-cases"></a>
### POST /sni/api/v1/kyc/cases

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_CASE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateKycCaseRequest`
- Réponse : `KycCaseResponse`
- Modèles : [KycCaseResponse](#m-KycCaseResponse), [CreateKycCaseRequest](#m-CreateKycCaseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `create`

<a id="r-get-sni-api-v1-kyc-cases-dashboard"></a>
### GET /sni/api/v1/kyc/cases/dashboard

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `KycDashboardResponse`
- Modèles : [KycDashboardResponse](#m-KycDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `dashboard`

<a id="r-get-sni-api-v1-kyc-cases-expiring-soon"></a>
### GET /sni/api/v1/kyc/cases/expiring-soon

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `PaginatedResponse<KycCaseResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [KycCaseResponse](#m-KycCaseResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `expiringSoon`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `days` | requête | `Integer` |  | 30 |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-kyc-cases-my-queue"></a>
### GET /sni/api/v1/kyc/cases/my-queue

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `PaginatedResponse<KycCaseResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [KycCaseResponse](#m-KycCaseResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `myQueue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userId` | requête | `Long` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-kyc-cases-code"></a>
### GET /sni/api/v1/kyc/cases/{code}

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `KycCaseResponse`
- Modèles : [KycCaseResponse](#m-KycCaseResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-kyc-cases-code-approve"></a>
### POST /sni/api/v1/kyc/cases/{code}/approve

- Accès : back-office : rôle d'administration et droit `KYC:APPROVE`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_CASE_APPROVE`, voir le README racine)
- Corps JSON (obligatoire) : `KycDecisionRequest`
- Réponse : `KycCaseResponse`
- Modèles : [KycCaseResponse](#m-KycCaseResponse), [KycDecisionRequest](#m-KycDecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-kyc-cases-code-assign"></a>
### PATCH /sni/api/v1/kyc/cases/{code}/assign

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Corps JSON (obligatoire) : `KycAssignRequest`
- Réponse : `KycCaseResponse`
- Modèles : [KycCaseResponse](#m-KycCaseResponse), [KycAssignRequest](#m-KycAssignRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `assign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-kyc-cases-code-documents-bulk-approve"></a>
### POST /sni/api/v1/kyc/cases/{code}/documents/bulk-approve

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_CASE_BULK_APPROVE`, voir le README racine)
- Corps JSON (obligatoire) : `KycCaseBulkApproveRequest`
- Réponse : `KycBulkActionResponse`
- Modèles : [KycBulkActionResponse](#m-KycBulkActionResponse), [KycCaseBulkApproveRequest](#m-KycCaseBulkApproveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `caseBulkApprove`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-kyc-cases-code-documents-bulk-reject"></a>
### POST /sni/api/v1/kyc/cases/{code}/documents/bulk-reject

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_CASE_BULK_REJECT`, voir le README racine)
- Corps JSON (obligatoire) : `KycCaseBulkRejectRequest`
- Réponse : `KycBulkActionResponse`
- Modèles : [KycBulkActionResponse](#m-KycBulkActionResponse), [KycCaseBulkRejectRequest](#m-KycCaseBulkRejectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `caseBulkReject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-kyc-cases-code-documents-request-correction"></a>
### POST /sni/api/v1/kyc/cases/{code}/documents/request-correction

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Corps JSON (obligatoire) : `KycCaseCorrectionRequest`
- Réponse : `KycDocumentResponse`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse), [KycCaseCorrectionRequest](#m-KycCaseCorrectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `caseCorrectionRequest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-kyc-cases-code-expiry-status"></a>
### GET /sni/api/v1/kyc/cases/{code}/expiry-status

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycExpiryDocumentStatus>`
- Modèles : [KycExpiryDocumentStatus](#m-KycExpiryDocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `expiryStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-kyc-cases-code-export-pdf"></a>
### GET /sni/api/v1/kyc/cases/{code}/export/pdf

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `exportPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `includeInternalNotes` | requête | `boolean` |  | true |

<a id="r-get-sni-api-v1-kyc-cases-code-missing-requirements"></a>
### GET /sni/api/v1/kyc/cases/{code}/missing-requirements

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycRequirementStatus>`
- Modèles : [KycRequirementStatus](#m-KycRequirementStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `missingRequirements`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-kyc-cases-code-notes"></a>
### GET /sni/api/v1/kyc/cases/{code}/notes

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycCaseNoteResponse>`
- Modèles : [KycCaseNoteResponse](#m-KycCaseNoteResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `notes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-kyc-cases-code-notes"></a>
### POST /sni/api/v1/kyc/cases/{code}/notes

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Corps JSON (obligatoire) : `KycCaseNoteRequest`
- Réponse : `KycCaseNoteResponse`
- Modèles : [KycCaseNoteResponse](#m-KycCaseNoteResponse), [KycCaseNoteRequest](#m-KycCaseNoteRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `addNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-kyc-cases-code-reject"></a>
### POST /sni/api/v1/kyc/cases/{code}/reject

- Accès : back-office : rôle d'administration et droit `KYC:REJECT`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_CASE_REJECT`, voir le README racine)
- Corps JSON (obligatoire) : `KycDecisionRequest`
- Réponse : `KycCaseResponse`
- Modèles : [KycCaseResponse](#m-KycCaseResponse), [KycDecisionRequest](#m-KycDecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-kyc-cases-code-requirements"></a>
### GET /sni/api/v1/kyc/cases/{code}/requirements

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycCaseRequirementResponse>`
- Modèles : [KycCaseRequirementResponse](#m-KycCaseRequirementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `listCaseRequirements`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-kyc-cases-code-requirements"></a>
### POST /sni/api/v1/kyc/cases/{code}/requirements

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Corps JSON (obligatoire) : `KycCaseRequirementRequest`
- Réponse : `KycCaseRequirementResponse`
- Modèles : [KycCaseRequirementResponse](#m-KycCaseRequirementResponse), [KycCaseRequirementRequest](#m-KycCaseRequirementRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `addCaseRequirement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-kyc-cases-code-requirements-documenttypecode"></a>
### DELETE /sni/api/v1/kyc/cases/{code}/requirements/{documentTypeCode}

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `removeCaseRequirement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `documentTypeCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-kyc-cases-code-review-queue"></a>
### GET /sni/api/v1/kyc/cases/{code}/review-queue

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycDocumentResponse>`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `caseReviewQueue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-kyc-cases-code-risk-level"></a>
### PATCH /sni/api/v1/kyc/cases/{code}/risk-level

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Corps JSON (obligatoire) : `KycRiskLevelRequest`
- Réponse : `KycCaseResponse`
- Modèles : [KycCaseResponse](#m-KycCaseResponse), [KycRiskLevelRequest](#m-KycRiskLevelRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `riskLevel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-kyc-cases-code-submit"></a>
### POST /sni/api/v1/kyc/cases/{code}/submit

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_CASE_SUBMIT`, voir le README racine)
- Réponse : `KycCaseResponse`
- Modèles : [KycCaseResponse](#m-KycCaseResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `submit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-kyc-cases-code-timeline"></a>
### GET /sni/api/v1/kyc/cases/{code}/timeline

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycTimelineEntryResponse>`
- Modèles : [KycTimelineEntryResponse](#m-KycTimelineEntryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycController.java`, méthode `timeline`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-KycCaseResponse"></a>
#### KycCaseResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `ownerName` | texte |  |  |
| `ownerCode` | texte |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `ownerId` | entier int64 ⚠ |  |  |
| `status` | enum : `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `PENDING_CORRECTION`, `RENEWAL_REQUIRED`, `EXPIRED` |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `submittedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `reviewedBy` | entier int64 ⚠ |  |  |
| `reviewedByEmail` | texte |  |  |
| `reviewedAt` | date-heure ISO 8601 UTC |  |  |
| `decisionComment` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `assignedToEmail` | texte |  |  |
| `assignedAt` | date-heure ISO 8601 UTC |  |  |
| `slaDeadline` | date-heure ISO 8601 UTC |  |  |
| `lastReminderSentAt` | date-heure ISO 8601 UTC |  |  |
| `reminderCount` | entier |  |  |
| `riskLevel` | enum : `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH` |  |  |
| `kycLevel` | entier |  |  |
| `complete` | booléen |  |  |
| `approved` | booléen |  |  |
| `missingDocumentTypeCodes` | texte[] |  |  |
| `requirements` | [KycRequirementStatus](#m-KycRequirementStatus)[] |  |  |
| `documents` | [KycDocumentResponse](#m-KycDocumentResponse)[] |  |  |

<a id="m-DocumentOwnerType"></a>
#### DocumentOwnerType

Énumération : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`

<a id="m-KycRiskLevel"></a>
#### KycRiskLevel

Énumération : `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`

<a id="m-CreateKycCaseRequest"></a>
#### CreateKycCaseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `documentTypeCodes` | texte[] |  |  |

<a id="m-KycDashboardResponse"></a>
#### KycDashboardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `summary` | objet { clé : entier int64 ⚠ } |  |  |
| `sla` | [KycDashboardResponse.SlaMetrics](#m-KycDashboardResponse-SlaMetrics) |  |  |
| `expiringSoon` | [KycDashboardResponse.ExpiringSoonMetrics](#m-KycDashboardResponse-ExpiringSoonMetrics) |  |  |
| `byOwnerType` | [KycDashboardResponse.OwnerTypeMetrics](#m-KycDashboardResponse-OwnerTypeMetrics)[] |  |  |
| `recentActivity` | [KycDashboardResponse.ActivityMetrics](#m-KycDashboardResponse-ActivityMetrics)[] |  |  |

<a id="m-KycDecisionRequest"></a>
#### KycDecisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reviewedBy` | entier int64 ⚠ | oui |  |
| `comment` | texte |  |  |

<a id="m-KycAssignRequest"></a>
#### KycAssignRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assignedTo` | entier int64 ⚠ |  |  |
| `email` | texte |  |  |

<a id="m-KycBulkActionResponse"></a>
#### KycBulkActionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `processed` | entier |  |  |
| `succeeded` | entier |  |  |
| `failed` | entier |  |  |
| `results` | [KycBulkItemResult](#m-KycBulkItemResult)[] |  |  |

<a id="m-KycCaseBulkApproveRequest"></a>
#### KycCaseBulkApproveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCodes` | texte[] | oui | éléments min `1` |
| `reviewedBy` | entier int64 ⚠ | oui |  |

<a id="m-KycCaseBulkRejectRequest"></a>
#### KycCaseBulkRejectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `items` | [KycBulkRejectItemRequest](#m-KycBulkRejectItemRequest)[] | oui | éléments min `1` |
| `reviewedBy` | entier int64 ⚠ | oui |  |

<a id="m-KycDocumentResponse"></a>
#### KycDocumentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `documentCode` | texte |  |  |
| `documentType` | texte |  |  |
| `ownerName` | texte |  |  |
| `documentNumber` | texte |  |  |
| `fileName` | texte |  |  |
| `fileSize` | entier int64 ⚠ |  |  |
| `mimeType` | texte |  |  |
| `previewUrl` | texte |  |  |
| `downloadUrl` | texte |  |  |
| `requiresBackSide` | booléen |  |  |
| `backDocumentCode` | texte |  |  |
| `backFileName` | texte |  |  |
| `backFileSize` | entier int64 ⚠ |  |  |
| `backMimeType` | texte |  |  |
| `backPreviewUrl` | texte |  |  |
| `backDownloadUrl` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `status` | enum : `PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED` |  |  |
| `uploadedBy` | entier int64 ⚠ |  |  |
| `uploadedByEmail` | texte |  |  |
| `uploadedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-KycCaseCorrectionRequest"></a>
#### KycCaseCorrectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte | oui | longueur min `1` |
| `reviewedBy` | entier int64 ⚠ | oui |  |
| `correctionNote` | texte | oui | longueur min `1` |
| `deadlineDays` | entier |  | min `1` |

<a id="m-KycExpiryDocumentStatus"></a>
#### KycExpiryDocumentStatus

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte |  |  |
| `documentType` | texte |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `daysUntilExpiry` | entier int64 ⚠ |  |  |
| `expired` | booléen |  |  |
| `status` | enum : `PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED` |  |  |

<a id="m-KycRequirementStatus"></a>
#### KycRequirementStatus

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentTypeCode` | texte |  |  |
| `documentTypeName` | texte |  |  |
| `required` | booléen |  |  |
| `requiresBackSide` | booléen |  |  |
| `status` | enum : `PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED` |  |  |
| `documentCode` | texte |  |  |
| `backDocumentCode` | texte |  |  |

<a id="m-KycCaseNoteResponse"></a>
#### KycCaseNoteResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `kycCaseCode` | texte |  |  |
| `content` | texte |  |  |
| `authorId` | entier int64 ⚠ |  |  |
| `authorEmail` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `internal` | booléen |  |  |

<a id="m-KycCaseNoteRequest"></a>
#### KycCaseNoteRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `content` | texte | oui | longueur min `1` |
| `authorId` | entier int64 ⚠ | oui |  |
| `internal` | booléen |  |  |

<a id="m-KycCaseRequirementResponse"></a>
#### KycCaseRequirementResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `kycCaseCode` | texte |  |  |
| `documentTypeCode` | texte |  |  |
| `documentTypeName` | texte |  |  |
| `required` | booléen |  |  |
| `requiresBackSide` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-KycCaseRequirementRequest"></a>
#### KycCaseRequirementRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentTypeCode` | texte | oui | longueur min `1` |
| `documentTypeName` | texte |  |  |
| `required` | booléen |  |  |

<a id="m-KycRiskLevelRequest"></a>
#### KycRiskLevelRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `riskLevel` | enum : `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH` | oui |  |
| `reviewedBy` | entier int64 ⚠ |  |  |
| `comment` | texte |  |  |

<a id="m-KycTimelineEntryResponse"></a>
#### KycTimelineEntryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `timestamp` | date-heure ISO 8601 UTC |  |  |
| `action` | texte |  |  |
| `actor` | texte |  |  |
| `description` | texte |  |  |

<a id="m-KycDashboardResponse-SlaMetrics"></a>
#### KycDashboardResponse.SlaMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `casesReviewedWithin24h` | entier int64 ⚠ |  |  |
| `casesExceeding48h` | entier int64 ⚠ |  |  |
| `avgReviewTimeHours` | nombre décimal |  |  |

<a id="m-KycDashboardResponse-ExpiringSoonMetrics"></a>
#### KycDashboardResponse.ExpiringSoonMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `within7Days` | entier int64 ⚠ |  |  |
| `within30Days` | entier int64 ⚠ |  |  |
| `within60Days` | entier int64 ⚠ |  |  |

<a id="m-KycDashboardResponse-OwnerTypeMetrics"></a>
#### KycDashboardResponse.OwnerTypeMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `count` | entier int64 ⚠ |  |  |
| `pendingReview` | entier int64 ⚠ |  |  |

<a id="m-KycDashboardResponse-ActivityMetrics"></a>
#### KycDashboardResponse.ActivityMetrics

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `action` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |
| `period` | texte |  |  |

<a id="m-KycBulkItemResult"></a>
#### KycBulkItemResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte |  |  |
| `success` | booléen |  |  |
| `error` | texte |  |  |

<a id="m-KycBulkRejectItemRequest"></a>
#### KycBulkRejectItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte | oui | longueur min `1` |
| `reason` | texte | oui | longueur min `1` |
