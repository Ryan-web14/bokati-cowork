# DocumentFolderController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 11 route(s).

## Routes

<a id="r-post-sni-api-v1-document-folders"></a>
### POST /sni/api/v1/document-folders

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `CreateFolderRequest`
- Réponse : `FolderDetailResponse`
- Modèles : [FolderDetailResponse](#m-FolderDetailResponse), [CreateFolderRequest](#m-CreateFolderRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `create`

<a id="r-get-sni-api-v1-document-folders-roots"></a>
### GET /sni/api/v1/document-folders/roots

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<FolderDetailResponse>`
- Modèles : [FolderDetailResponse](#m-FolderDetailResponse), [DocumentSpace](#m-DocumentSpace)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `roots`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | requête | `DocumentSpace` |  |  |

<a id="r-delete-sni-api-v1-document-folders-code"></a>
### DELETE /sni/api/v1/document-folders/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-folders-code"></a>
### GET /sni/api/v1/document-folders/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `FolderDetailResponse`
- Modèles : [FolderDetailResponse](#m-FolderDetailResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-document-folders-code"></a>
### PATCH /sni/api/v1/document-folders/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `UpdateFolderRequest`
- Réponse : `FolderDetailResponse`
- Modèles : [FolderDetailResponse](#m-FolderDetailResponse), [UpdateFolderRequest](#m-UpdateFolderRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-folders-code-breadcrumb"></a>
### GET /sni/api/v1/document-folders/{code}/breadcrumb

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<BreadcrumbItem>`
- Modèles : [BreadcrumbItem](#m-BreadcrumbItem)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `breadcrumb`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-folders-code-children"></a>
### GET /sni/api/v1/document-folders/{code}/children

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<FolderDetailResponse>`
- Modèles : [FolderDetailResponse](#m-FolderDetailResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `children`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-folders-code-documents"></a>
### GET /sni/api/v1/document-folders/{code}/documents

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<DocumentResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DocumentResponse](#m-DocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `documents`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=uploadedAt,DESC |

<a id="r-post-sni-api-v1-document-folders-code-move"></a>
### POST /sni/api/v1/document-folders/{code}/move

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `MoveFolderRequest`
- Réponse : `FolderDetailResponse`
- Modèles : [FolderDetailResponse](#m-FolderDetailResponse), [MoveFolderRequest](#m-MoveFolderRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `move`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-folders-code-tree"></a>
### GET /sni/api/v1/document-folders/{code}/tree

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `DocumentFolderTreeNode`
- Modèles : [DocumentFolderTreeNode](#m-DocumentFolderTreeNode)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `tree`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-document-folders-documentcode-move-document"></a>
### POST /sni/api/v1/document-folders/{documentCode}/move-document

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `MoveDocumentToFolderRequest`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [MoveDocumentToFolderRequest](#m-MoveDocumentToFolderRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentFolderController.java`, méthode `moveDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-FolderDetailResponse"></a>
#### FolderDetailResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `parentCode` | texte |  |  |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `ownerId` | entier int64 ⚠ |  |  |
| `ownerName` | texte |  |  |
| `path` | texte |  |  |
| `depth` | entier |  |  |
| `sortOrder` | entier |  |  |
| `color` | texte |  |  |
| `icon` | texte |  |  |
| `childrenCount` | entier int64 ⚠ |  |  |
| `documentsCount` | entier int64 ⚠ |  |  |
| `createdBy` | entier int64 ⚠ |  |  |
| `createdByEmail` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CreateFolderRequest"></a>
#### CreateFolderRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `parentCode` | texte |  |  |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `ownerCode` | texte |  |  |
| `color` | texte |  |  |
| `icon` | texte |  |  |

<a id="m-DocumentSpace"></a>
#### DocumentSpace

Énumération : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC`

<a id="m-UpdateFolderRequest"></a>
#### UpdateFolderRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `color` | texte |  |  |
| `icon` | texte |  |  |
| `sortOrder` | entier |  |  |

<a id="m-BreadcrumbItem"></a>
#### BreadcrumbItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `depth` | entier |  |  |

<a id="m-DocumentResponse"></a>
#### DocumentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `ownerId` | entier int64 ⚠ |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
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

<a id="m-MoveFolderRequest"></a>
#### MoveFolderRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `targetParentCode` | texte |  |  |

<a id="m-DocumentFolderTreeNode"></a>
#### DocumentFolderTreeNode

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `icon` | texte |  |  |
| `color` | texte |  |  |
| `depth` | entier |  |  |
| `documentsCount` | entier int64 ⚠ |  |  |
| `children` | [DocumentFolderTreeNode](#m-DocumentFolderTreeNode)[] |  |  |

<a id="m-MoveDocumentToFolderRequest"></a>
#### MoveDocumentToFolderRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `folderCode` | texte |  |  |

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
