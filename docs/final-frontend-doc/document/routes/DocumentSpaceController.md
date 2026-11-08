# DocumentSpaceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 15 route(s).

## Routes

<a id="r-get-sni-api-v1-spaces-administrative-dashboard"></a>
### GET /sni/api/v1/spaces/administrative/dashboard

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `DocumentDashboardResponse`
- Modèles : [DocumentDashboardResponse](#m-DocumentDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `adminDashboard`

<a id="r-get-sni-api-v1-spaces-administrative-documents"></a>
### GET /sni/api/v1/spaces/administrative/documents

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse), [DocumentOwnerType](#m-DocumentOwnerType), [DocumentStatus](#m-DocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `listAdministrative`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `statuses` | requête | `List<DocumentStatus>` |  |  |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=uploadedAt,DESC |

<a id="r-post-sni-api-v1-spaces-administrative-documents-upload"></a>
### POST /sni/api/v1/spaces/administrative/documents/upload

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Envoi : `multipart/form-data`
- En-tête `Idempotency-Key` : conseillé (opération `SPACE_ADMIN_UPLOAD`, voir le README racine)
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [SpaceDocumentUploadRequest](#m-SpaceDocumentUploadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `uploadAdministrative`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `request` | requête (champs de l'objet) | `SpaceDocumentUploadRequest` |  |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-get-sni-api-v1-spaces-assets-dashboard"></a>
### GET /sni/api/v1/spaces/assets/dashboard

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `DocumentDashboardResponse`
- Modèles : [DocumentDashboardResponse](#m-DocumentDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `assetDashboard`

<a id="r-get-sni-api-v1-spaces-assets-documents"></a>
### GET /sni/api/v1/spaces/assets/documents

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse), [DocumentOwnerType](#m-DocumentOwnerType), [DocumentStatus](#m-DocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `listAssets`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `statuses` | requête | `List<DocumentStatus>` |  |  |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=uploadedAt,DESC |

<a id="r-post-sni-api-v1-spaces-assets-documents-upload"></a>
### POST /sni/api/v1/spaces/assets/documents/upload

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Envoi : `multipart/form-data`
- En-tête `Idempotency-Key` : conseillé (opération `SPACE_ASSET_UPLOAD`, voir le README racine)
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [SpaceDocumentUploadRequest](#m-SpaceDocumentUploadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `uploadAsset`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `request` | requête (champs de l'objet) | `SpaceDocumentUploadRequest` |  |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-get-sni-api-v1-spaces-contracts-dashboard"></a>
### GET /sni/api/v1/spaces/contracts/dashboard

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `DocumentDashboardResponse`
- Modèles : [DocumentDashboardResponse](#m-DocumentDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `contractDashboard`

<a id="r-get-sni-api-v1-spaces-contracts-documents"></a>
### GET /sni/api/v1/spaces/contracts/documents

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse), [DocumentOwnerType](#m-DocumentOwnerType), [DocumentStatus](#m-DocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `listContracts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `statuses` | requête | `List<DocumentStatus>` |  |  |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=uploadedAt,DESC |

<a id="r-post-sni-api-v1-spaces-contracts-documents-upload"></a>
### POST /sni/api/v1/spaces/contracts/documents/upload

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Envoi : `multipart/form-data`
- En-tête `Idempotency-Key` : conseillé (opération `SPACE_CONTRACT_UPLOAD`, voir le README racine)
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [SpaceDocumentUploadRequest](#m-SpaceDocumentUploadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `uploadContract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `request` | requête (champs de l'objet) | `SpaceDocumentUploadRequest` |  |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-get-sni-api-v1-spaces-financial-dashboard"></a>
### GET /sni/api/v1/spaces/financial/dashboard

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `DocumentDashboardResponse`
- Modèles : [DocumentDashboardResponse](#m-DocumentDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `financialDashboard`

<a id="r-get-sni-api-v1-spaces-financial-documents"></a>
### GET /sni/api/v1/spaces/financial/documents

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse), [DocumentOwnerType](#m-DocumentOwnerType), [DocumentStatus](#m-DocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `listFinancial`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `statuses` | requête | `List<DocumentStatus>` |  |  |
| `q` | requête | `String` |  |  |
| `uploadedAfter` | requête | `LocalDate` |  |  |
| `uploadedBefore` | requête | `LocalDate` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=uploadedAt,DESC |

<a id="r-post-sni-api-v1-spaces-financial-documents-upload"></a>
### POST /sni/api/v1/spaces/financial/documents/upload

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Envoi : `multipart/form-data`
- En-tête `Idempotency-Key` : conseillé (opération `SPACE_FINANCIAL_UPLOAD`, voir le README racine)
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [SpaceDocumentUploadRequest](#m-SpaceDocumentUploadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `uploadFinancial`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `request` | requête (champs de l'objet) | `SpaceDocumentUploadRequest` |  |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-get-sni-api-v1-spaces-general-dashboard"></a>
### GET /sni/api/v1/spaces/general/dashboard

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `DocumentDashboardResponse`
- Modèles : [DocumentDashboardResponse](#m-DocumentDashboardResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `generalDashboard`

<a id="r-get-sni-api-v1-spaces-general-documents"></a>
### GET /sni/api/v1/spaces/general/documents

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse), [DocumentOwnerType](#m-DocumentOwnerType), [DocumentStatus](#m-DocumentStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `listGeneral`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `statuses` | requête | `List<DocumentStatus>` |  |  |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=uploadedAt,DESC |

<a id="r-post-sni-api-v1-spaces-general-documents-upload"></a>
### POST /sni/api/v1/spaces/general/documents/upload

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Envoi : `multipart/form-data`
- En-tête `Idempotency-Key` : conseillé (opération `SPACE_GENERAL_UPLOAD`, voir le README racine)
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [SpaceDocumentUploadRequest](#m-SpaceDocumentUploadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSpaceController.java`, méthode `uploadGeneral`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `request` | requête (champs de l'objet) | `SpaceDocumentUploadRequest` |  |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-DocumentStatus"></a>
#### DocumentStatus

Énumération : `DRAFT`, `UPLOADED`, `PENDING_REVIEW`, `NEEDS_CORRECTION`, `APPROVED`, `REJECTED`, `SIGNED`, `EXPIRED`, `ARCHIVED`, `SUPERSEDED`

<a id="m-SpaceDocumentUploadRequest"></a>
#### SpaceDocumentUploadRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `documentTypeCode` | texte | oui | longueur min `1` |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `documentNumber` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `referenceCode` | texte |  |  |
| `folderCode` | texte |  |  |
| `tagCodes` | texte[] |  |  |
| `metadata` | objet { clé : texte } |  |  |

<a id="m-DocumentDashboardResponse-TagCount"></a>
#### DocumentDashboardResponse.TagCount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `tagCode` | texte |  |  |
| `tagLabel` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |

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
