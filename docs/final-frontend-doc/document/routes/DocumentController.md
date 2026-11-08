# DocumentController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 25 route(s).

## Routes

<a id="r-get-sni-api-v1-documents"></a>
### GET /sni/api/v1/documents

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=uploadedAt,DESC |

<a id="r-get-sni-api-v1-documents-access-logs-by-user"></a>
### GET /sni/api/v1/documents/access-logs/by-user

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `PaginatedResponse<DocumentAccessLogResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentAccessLogResponse](#m-DocumentAccessLogResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `accessLogsByUser`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userId` | requête | `Long` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=accessedAt,DESC |

<a id="r-get-sni-api-v1-documents-analytics"></a>
### GET /sni/api/v1/documents/analytics

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `DocumentAnalyticsResponse`
- Modèles : [DocumentAnalyticsResponse](#m-DocumentAnalyticsResponse), [DocumentSpace](#m-DocumentSpace)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `analytics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | requête | `DocumentSpace` |  |  |
| `months` | requête | `int` |  | 12 |

<a id="r-get-sni-api-v1-documents-dashboard"></a>
### GET /sni/api/v1/documents/dashboard

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `DocumentDashboardResponse`
- Modèles : [DocumentDashboardResponse](#m-DocumentDashboardResponse), [DocumentSpace](#m-DocumentSpace)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | requête | `DocumentSpace` |  |  |

<a id="r-get-sni-api-v1-documents-export-csv"></a>
### GET /sni/api/v1/documents/export/csv

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `fichier (octets)`
- Modèles : [DocumentSpace](#m-DocumentSpace), [DocumentCategory](#m-DocumentCategory), [DocumentOwnerType](#m-DocumentOwnerType), [DocumentStatus](#m-DocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `exportCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | requête | `DocumentSpace` |  |  |
| `category` | requête | `DocumentCategory` |  |  |
| `tags` | requête | `List<String>` |  |  |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `statuses` | requête | `List<DocumentStatus>` |  |  |
| `uploadedAfter` | requête | `LocalDate` |  |  |
| `uploadedBefore` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-documents-folder"></a>
### GET /sni/api/v1/documents/folder

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `DocumentFolderResponse`
- Modèles : [DocumentFolderResponse](#m-DocumentFolderResponse), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `folder`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` | oui |  |
| `ownerCode` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-import-zip"></a>
### POST /sni/api/v1/documents/import-zip

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Envoi : `multipart/form-data`
- Réponse : `ZipImportResponse`
- Modèles : [ZipImportResponse](#m-ZipImportResponse), [BatchUploadMetadataRequest](#m-BatchUploadMetadataRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `importZip`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `metadata` | requête (champs de l'objet) | `BatchUploadMetadataRequest` |  |  |

<a id="r-get-sni-api-v1-documents-search"></a>
### GET /sni/api/v1/documents/search

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse), [DocumentSpace](#m-DocumentSpace), [DocumentCategory](#m-DocumentCategory), [DocumentOwnerType](#m-DocumentOwnerType), [DocumentStatus](#m-DocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `space` | requête | `DocumentSpace` |  |  |
| `category` | requête | `DocumentCategory` |  |  |
| `tags` | requête | `List<String>` |  |  |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `statuses` | requête | `List<DocumentStatus>` |  |  |
| `expiresInDays` | requête | `Integer` |  |  |
| `isExpired` | requête | `Boolean` |  |  |
| `uploadedAfter` | requête | `LocalDate` |  |  |
| `uploadedBefore` | requête | `LocalDate` |  |  |
| `metaKey` | requête | `String` |  |  |
| `metaValue` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=uploadedAt,DESC |

<a id="r-post-sni-api-v1-documents-upload"></a>
### POST /sni/api/v1/documents/upload

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Envoi : `multipart/form-data`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_UPLOAD`, voir le README racine)
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [DocumentUploadMetadataRequest](#m-DocumentUploadMetadataRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `upload`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `metadata` | requête (champs de l'objet) | `DocumentUploadMetadataRequest` |  |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-post-sni-api-v1-documents-upload-batch"></a>
### POST /sni/api/v1/documents/upload-batch

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Envoi : `multipart/form-data`
- Réponse : `DocumentBulkActionResponse`
- Modèles : [DocumentBulkActionResponse](#m-DocumentBulkActionResponse), [BatchUploadMetadataRequest](#m-BatchUploadMetadataRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `uploadBatch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `files` | partie multipart | `List<MultipartFile>` | oui |  |
| `metadata` | requête (champs de l'objet) | `BatchUploadMetadataRequest` |  |  |

<a id="r-get-sni-api-v1-documents-code"></a>
### GET /sni/api/v1/documents/{code}

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-code-access-logs"></a>
### GET /sni/api/v1/documents/{code}/access-logs

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `PaginatedResponse<DocumentAccessLogResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentAccessLogResponse](#m-DocumentAccessLogResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `accessLogs`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=accessedAt,DESC |

<a id="r-post-sni-api-v1-documents-code-approve"></a>
### POST /sni/api/v1/documents/{code}/approve

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_APPROVE`, voir le README racine)
- Corps JSON (obligatoire) : `DocumentReviewDecisionRequest`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [DocumentReviewDecisionRequest](#m-DocumentReviewDecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-code-archive"></a>
### POST /sni/api/v1/documents/{code}/archive

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `archive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-documents-code-download"></a>
### GET /sni/api/v1/documents/{code}/download

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `download`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-code-lock"></a>
### POST /sni/api/v1/documents/{code}/lock

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `lock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-code-preview"></a>
### GET /sni/api/v1/documents/{code}/preview

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-code-preview-token"></a>
### GET /sni/api/v1/documents/{code}/preview-token

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `previewToken`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-code-reject"></a>
### POST /sni/api/v1/documents/{code}/reject

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_REJECT`, voir le README racine)
- Corps JSON (obligatoire) : `DocumentReviewDecisionRequest`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [DocumentReviewDecisionRequest](#m-DocumentReviewDecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-code-replace"></a>
### POST /sni/api/v1/documents/{code}/replace

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Envoi : `multipart/form-data`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_REPLACE`, voir le README racine)
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `replace`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-post-sni-api-v1-documents-code-request-correction"></a>
### POST /sni/api/v1/documents/{code}/request-correction

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Corps JSON (obligatoire) : `DocumentCorrectionRequest`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [DocumentCorrectionRequest](#m-DocumentCorrectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `requestCorrection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-code-signed-preview"></a>
### GET /sni/api/v1/documents/{code}/signed-preview

- Accès : public, sans jeton
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `signedPreview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `token` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-documents-code-unlock"></a>
### POST /sni/api/v1/documents/{code}/unlock

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `unlock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-code-versions"></a>
### GET /sni/api/v1/documents/{code}/versions

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `List<DocumentVersionResponse>`
- Modèles : [DocumentVersionResponse](#m-DocumentVersionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `listVersions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-code-versions-versionnumber-restore"></a>
### POST /sni/api/v1/documents/{code}/versions/{versionNumber}/restore

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentController.java`, méthode `restoreVersion`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `versionNumber` | chemin | `Integer` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DocumentResponse"></a>
#### DocumentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `ownerId` | entier int64 ⚠ |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` |  |  |
| `category` | enum : `KYC`, `LEGAL`, `SYSTEM`, `FINANCIAL`, `ASSET`, `OTHER` |  |  |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `spaceReferenceCode` | texte |  |  |
| `folderCode` | texte |  |  |
| `folderName` | texte |  |  |
| `documentTypeCode` | texte |  |  |
| `documentTypeName` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `fileName` | texte |  |  |
| `fileUrl` | texte |  |  |
| `previewUrl` | texte |  |  |
| `downloadUrl` | texte |  |  |
| `fileSize` | entier int64 ⚠ |  |  |
| `mimeType` | texte |  |  |
| `checksumSha256` | texte |  |  |
| `currentVersionNumber` | entier |  |  |
| `status` | enum : `DRAFT`, `UPLOADED`, `PENDING_REVIEW`, `NEEDS_CORRECTION`, `APPROVED`, `REJECTED`, `SIGNED`, `EXPIRED`, `ARCHIVED`, `SUPERSEDED` |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `uploadedBy` | entier int64 ⚠ |  |  |
| `uploadedAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `versions` | [DocumentVersionResponse](#m-DocumentVersionResponse)[] |  |  |
| `tags` | [DocumentTagResponse](#m-DocumentTagResponse)[] |  |  |
| `metadata` | objet { clé : texte } |  |  |

<a id="m-DocumentOwnerType"></a>
#### DocumentOwnerType

Énumération : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL`

<a id="m-DocumentAccessLogResponse"></a>
#### DocumentAccessLogResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `documentCode` | texte |  |  |
| `userId` | entier int64 ⚠ |  |  |
| `action` | texte |  |  |
| `accessedAt` | date-heure ISO 8601 UTC |  |  |
| `ipAddress` | texte |  |  |
| `userAgent` | texte |  |  |

<a id="m-DocumentAnalyticsResponse"></a>
#### DocumentAnalyticsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `periodMonths` | entier |  |  |
| `totalDocuments` | entier int64 ⚠ |  |  |
| `pendingReview` | entier int64 ⚠ |  |  |
| `needsCorrection` | entier int64 ⚠ |  |  |
| `expiredDocuments` | entier int64 ⚠ |  |  |
| `byStatus` | objet { clé : entier int64 ⚠ } |  |  |
| `bySpace` | objet { clé : entier int64 ⚠ } |  |  |
| `byCategory` | objet { clé : entier int64 ⚠ } |  |  |
| `correctionRate30d` | nombre décimal |  |  |
| `rejectionRate30d` | nombre décimal |  |  |
| `avgReviewTimeHours` | nombre décimal |  |  |
| `uploadsByMonth` | [DocumentAnalyticsResponse.PeriodCount](#m-DocumentAnalyticsResponse-PeriodCount)[] |  |  |
| `approvalsByMonth` | [DocumentAnalyticsResponse.PeriodCount](#m-DocumentAnalyticsResponse-PeriodCount)[] |  |  |
| `rejectionsByMonth` | [DocumentAnalyticsResponse.PeriodCount](#m-DocumentAnalyticsResponse-PeriodCount)[] |  |  |
| `correctionsByMonth` | [DocumentAnalyticsResponse.PeriodCount](#m-DocumentAnalyticsResponse-PeriodCount)[] |  |  |

<a id="m-DocumentSpace"></a>
#### DocumentSpace

Énumération : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC`

<a id="m-DocumentDashboardResponse"></a>
#### DocumentDashboardResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `total` | entier int64 ⚠ |  |  |
| `byStatus` | objet { clé : entier int64 ⚠ } |  |  |
| `byCategory` | objet { clé : entier int64 ⚠ } |  |  |
| `pendingReviewOlderThan48h` | entier int64 ⚠ |  |  |
| `needsCorrectionCount` | entier int64 ⚠ |  |  |
| `expiringIn30Days` | entier int64 ⚠ |  |  |
| `rejectionRate30d` | nombre décimal |  |  |
| `avgReviewTimeHours` | nombre décimal |  |  |
| `topTags` | [DocumentDashboardResponse.TagCount](#m-DocumentDashboardResponse-TagCount)[] |  |  |

<a id="m-DocumentCategory"></a>
#### DocumentCategory

Énumération : `KYC`, `LEGAL`, `SYSTEM`, `FINANCIAL`, `ASSET`, `OTHER`

<a id="m-DocumentStatus"></a>
#### DocumentStatus

Énumération : `DRAFT`, `UPLOADED`, `PENDING_REVIEW`, `NEEDS_CORRECTION`, `APPROVED`, `REJECTED`, `SIGNED`, `EXPIRED`, `ARCHIVED`, `SUPERSEDED`

<a id="m-DocumentFolderResponse"></a>
#### DocumentFolderResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `owner` | [DocumentFolderResponse.OwnerInfo](#m-DocumentFolderResponse-OwnerInfo) |  |  |
| `spaces` | [DocumentFolderResponse.SpaceFolder](#m-DocumentFolderResponse-SpaceFolder)[] |  |  |
| `missingRequirements` | [KycRequirementStatus](#m-KycRequirementStatus)[] |  |  |
| `expiringSoon` | [DocumentResponse](#m-DocumentResponse)[] |  |  |

<a id="m-ZipImportResponse"></a>
#### ZipImportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `totalFiles` | entier |  |  |
| `succeeded` | entier |  |  |
| `failed` | entier |  |  |
| `foldersCreated` | entier |  |  |
| `items` | [ZipImportResponse.ZipImportItem](#m-ZipImportResponse-ZipImportItem)[] |  |  |

<a id="m-BatchUploadMetadataRequest"></a>
#### BatchUploadMetadataRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `documentTypeCode` | texte | oui | longueur min `1` |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `folderCode` | texte |  |  |
| `referenceCode` | texte |  |  |
| `tagCodes` | texte[] |  |  |

<a id="m-DocumentUploadMetadataRequest"></a>
#### DocumentUploadMetadataRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` | oui |  |
| `ownerCode` | texte |  |  |
| `documentTypeCode` | texte | oui | longueur min `1` |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `documentNumber` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `folderCode` | texte |  |  |

<a id="m-DocumentBulkActionResponse"></a>
#### DocumentBulkActionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `total` | entier |  |  |
| `succeeded` | entier |  |  |
| `failed` | entier |  |  |
| `results` | [DocumentBulkItemResult](#m-DocumentBulkItemResult)[] |  |  |

<a id="m-DocumentReviewDecisionRequest"></a>
#### DocumentReviewDecisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reviewedBy` | entier int64 ⚠ | oui |  |
| `comment` | texte |  |  |
| `rejectionReasonCode` | texte |  |  |
| `rejectionReasonDetail` | texte |  |  |

<a id="m-DocumentCorrectionRequest"></a>
#### DocumentCorrectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reviewedBy` | entier int64 ⚠ | oui |  |
| `correctionNote` | texte | oui | longueur min `1` |
| `comment` | texte |  |  |
| `deadlineDays` | entier |  | min `1` |

<a id="m-DocumentVersionResponse"></a>
#### DocumentVersionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `versionNumber` | entier |  |  |
| `storageProvider` | texte |  |  |
| `storagePath` | texte |  |  |
| `previewUrl` | texte |  |  |
| `downloadUrl` | texte |  |  |
| `originalFileName` | texte |  |  |
| `storedFileName` | texte |  |  |
| `mimeTypeDeclared` | texte |  |  |
| `mimeTypeDetected` | texte |  |  |
| `fileExtension` | texte |  |  |
| `checksumSha256` | texte |  |  |
| `fileSizeBytes` | entier int64 ⚠ |  |  |
| `uploadStatus` | enum : `PENDING_SCAN`, `READY`, `QUARANTINED`, `REJECTED`, `DELETED` |  |  |
| `antivirusStatus` | enum : `PENDING`, `CLEAN`, `INFECTED`, `ERROR` |  |  |
| `uploadedBy` | entier int64 ⚠ |  |  |
| `uploadedAt` | date-heure ISO 8601 UTC |  |  |
| `current` | booléen |  |  |

<a id="m-DocumentTagResponse"></a>
#### DocumentTagResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `color` | texte |  |  |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `createdBy` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DocumentAnalyticsResponse-PeriodCount"></a>
#### DocumentAnalyticsResponse.PeriodCount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `period` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |

<a id="m-DocumentDashboardResponse-TagCount"></a>
#### DocumentDashboardResponse.TagCount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `tagCode` | texte |  |  |
| `tagLabel` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |

<a id="m-DocumentFolderResponse-OwnerInfo"></a>
#### DocumentFolderResponse.OwnerInfo

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |

<a id="m-DocumentFolderResponse-SpaceFolder"></a>
#### DocumentFolderResponse.SpaceFolder

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `totalDocuments` | entier int64 ⚠ |  |  |
| `approvedDocuments` | entier int64 ⚠ |  |  |
| `completionRate` | nombre décimal |  |  |
| `documents` | [DocumentResponse](#m-DocumentResponse)[] |  |  |

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

<a id="m-ZipImportResponse-ZipImportItem"></a>
#### ZipImportResponse.ZipImportItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `originalPath` | texte |  |  |
| `documentCode` | texte |  |  |
| `folderCode` | texte |  |  |
| `success` | booléen |  |  |
| `error` | texte |  |  |

<a id="m-DocumentBulkItemResult"></a>
#### DocumentBulkItemResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `status` | texte |  |  |
| `reason` | texte |  |  |
