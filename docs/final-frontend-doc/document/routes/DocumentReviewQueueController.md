# DocumentReviewQueueController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-review-queue"></a>
### GET /sni/api/v1/review/queue

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentReviewQueueController.java`, méthode `globalQueue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-review-space"></a>
### GET /sni/api/v1/review/{space}

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `@docSpaceSecurity.canReview(authentication, #space)`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse), [DocumentSpace](#m-DocumentSpace)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentReviewQueueController.java`, méthode `spaceQueue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | chemin | `DocumentSpace` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-review-space-bulk-approve"></a>
### POST /sni/api/v1/review/{space}/bulk-approve

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `@docSpaceSecurity.canReview(authentication, #space)`
- Corps JSON (obligatoire) : `DocumentBulkApproveRequest`
- Réponse : `DocumentBulkActionResponse`
- Modèles : [DocumentBulkActionResponse](#m-DocumentBulkActionResponse), [DocumentBulkApproveRequest](#m-DocumentBulkApproveRequest), [DocumentSpace](#m-DocumentSpace)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentReviewQueueController.java`, méthode `bulkApprove`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | chemin | `DocumentSpace` | oui |  |

<a id="r-post-sni-api-v1-review-space-bulk-reject"></a>
### POST /sni/api/v1/review/{space}/bulk-reject

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `@docSpaceSecurity.canReview(authentication, #space)`
- Corps JSON (obligatoire) : `DocumentBulkRejectRequest`
- Réponse : `DocumentBulkActionResponse`
- Modèles : [DocumentBulkActionResponse](#m-DocumentBulkActionResponse), [DocumentBulkRejectRequest](#m-DocumentBulkRejectRequest), [DocumentSpace](#m-DocumentSpace)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentReviewQueueController.java`, méthode `bulkReject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | chemin | `DocumentSpace` | oui |  |

<a id="r-post-sni-api-v1-review-space-bulk-request-correction"></a>
### POST /sni/api/v1/review/{space}/bulk-request-correction

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW` ; contrôle supplémentaire sur la méthode : `@docSpaceSecurity.canReview(authentication, #space)`
- Corps JSON (obligatoire) : `DocumentBulkCorrectionRequest`
- Réponse : `DocumentBulkActionResponse`
- Modèles : [DocumentBulkActionResponse](#m-DocumentBulkActionResponse), [DocumentBulkCorrectionRequest](#m-DocumentBulkCorrectionRequest), [DocumentSpace](#m-DocumentSpace)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentReviewQueueController.java`, méthode `bulkRequestCorrection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | chemin | `DocumentSpace` | oui |  |

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

<a id="m-DocumentSpace"></a>
#### DocumentSpace

Énumération : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC`

<a id="m-DocumentBulkActionResponse"></a>
#### DocumentBulkActionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `total` | entier |  |  |
| `succeeded` | entier |  |  |
| `failed` | entier |  |  |
| `results` | [DocumentBulkItemResult](#m-DocumentBulkItemResult)[] |  |  |

<a id="m-DocumentBulkApproveRequest"></a>
#### DocumentBulkApproveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `codes` | texte[] | oui | éléments min `1` |
| `reviewedBy` | entier int64 ⚠ | oui |  |
| `comment` | texte |  |  |

<a id="m-DocumentBulkRejectRequest"></a>
#### DocumentBulkRejectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `codes` | texte[] | oui | éléments min `1` |
| `reviewedBy` | entier int64 ⚠ | oui |  |
| `rejectionReasonCode` | texte |  |  |
| `rejectionReasonDetail` | texte |  |  |
| `comment` | texte |  |  |

<a id="m-DocumentBulkCorrectionRequest"></a>
#### DocumentBulkCorrectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `codes` | texte[] | oui | éléments min `1` |
| `reviewedBy` | entier int64 ⚠ | oui |  |
| `correctionNote` | texte | oui | longueur min `1` |
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

<a id="m-DocumentBulkItemResult"></a>
#### DocumentBulkItemResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `status` | texte |  |  |
| `reason` | texte |  |  |
