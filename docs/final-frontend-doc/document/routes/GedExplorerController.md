# GedExplorerController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 18 route(s).

## Routes

<a id="r-post-sni-api-v1-ged-copy"></a>
### POST /sni/api/v1/ged/copy

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (obligatoire) : `GedDtos.GedCopyRequest`
- Réponse : `GedDtos.GedUploadResult`
- Modèles : [GedDtos.GedUploadResult](#m-GedDtos-GedUploadResult), [GedDtos.GedCopyRequest](#m-GedDtos-GedCopyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `copy`

<a id="r-post-sni-api-v1-ged-files"></a>
### POST /sni/api/v1/ged/files

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Envoi : `multipart/form-data`
- Réponse : `GedDtos.GedUploadResult`
- Modèles : [GedDtos.GedUploadResult](#m-GedDtos-GedUploadResult), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `upload`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `files` | partie multipart | `List<MultipartFile>` | oui |  |
| `folderCode` | requête | `String` |  |  |
| `relativePaths` | requête | `List<String>` |  |  |
| `documentTypeCode` | requête | `String` |  |  |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-ged-files-code"></a>
### PATCH /sni/api/v1/ged/files/{code}

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (obligatoire) : `GedDtos.GedRenameRequest`
- Réponse : `GedDtos.GedItem`
- Modèles : [GedDtos.GedItem](#m-GedDtos-GedItem), [GedDtos.GedRenameRequest](#m-GedDtos-GedRenameRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `renameFile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-ged-files-code-text"></a>
### GET /sni/api/v1/ged/files/{code}/text

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `GedDtos.GedFileText`
- Modèles : [GedDtos.GedFileText](#m-GedDtos-GedFileText)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `fileText`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-ged-files-code-text-refresh"></a>
### POST /sni/api/v1/ged/files/{code}/text/refresh

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `GedDtos.GedFileText`
- Modèles : [GedDtos.GedFileText](#m-GedDtos-GedFileText)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `refreshText`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-ged-filing-run"></a>
### POST /sni/api/v1/ged/filing/run

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `GedDtos.GedFilingResult`
- Modèles : [GedDtos.GedFilingResult](#m-GedDtos-GedFilingResult)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `runFiling`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 1000 |

<a id="r-post-sni-api-v1-ged-folders"></a>
### POST /sni/api/v1/ged/folders

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (obligatoire) : `GedDtos.GedCreateFolderRequest`
- Réponse : `GedDtos.GedItem`
- Modèles : [GedDtos.GedItem](#m-GedDtos-GedItem), [GedDtos.GedCreateFolderRequest](#m-GedDtos-GedCreateFolderRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `createFolder`

<a id="r-patch-sni-api-v1-ged-folders-code"></a>
### PATCH /sni/api/v1/ged/folders/{code}

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (obligatoire) : `GedDtos.GedRenameRequest`
- Réponse : `GedDtos.GedItem`
- Modèles : [GedDtos.GedItem](#m-GedDtos-GedItem), [GedDtos.GedRenameRequest](#m-GedDtos-GedRenameRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `renameFolder`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-ged-items"></a>
### GET /sni/api/v1/ged/items

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `GedDtos.GedListing`
- Modèles : [GedDtos.GedListing](#m-GedDtos-GedListing)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `items`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `folderCode` | requête | `String` |  |  |
| `search` | requête | `String` |  |  |
| `sort` | requête | `String` |  | name |
| `direction` | requête | `String` |  | asc |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 50 |

<a id="r-post-sni-api-v1-ged-move"></a>
### POST /sni/api/v1/ged/move

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (obligatoire) : `GedDtos.GedMoveRequest`
- Réponse : `vide`
- Modèles : [GedDtos.GedMoveRequest](#m-GedDtos-GedMoveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `move`

<a id="r-post-sni-api-v1-ged-scans"></a>
### POST /sni/api/v1/ged/scans

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Envoi : `multipart/form-data`
- Réponse : `GedDtos.GedItem`
- Modèles : [GedDtos.GedItem](#m-GedDtos-GedItem), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `scan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `pages` | partie multipart | `List<MultipartFile>` | oui |  |
| `folderCode` | requête | `String` |  |  |
| `title` | requête | `String` |  |  |
| `documentTypeCode` | requête | `String` |  |  |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-ged-search"></a>
### GET /sni/api/v1/ged/search

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `GedDtos.GedSearchResult`
- Modèles : [GedDtos.GedSearchResult](#m-GedDtos-GedSearchResult)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` | oui |  |
| `folderCode` | requête | `String` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-post-sni-api-v1-ged-text-run"></a>
### POST /sni/api/v1/ged/text/run

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `GedDtos.GedTextRunResult`
- Modèles : [GedDtos.GedTextRunResult](#m-GedDtos-GedTextRunResult)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `runText`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 20 |

<a id="r-get-sni-api-v1-ged-text-status"></a>
### GET /sni/api/v1/ged/text/status

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `GedDtos.GedTextStatus`
- Modèles : [GedDtos.GedTextStatus](#m-GedDtos-GedTextStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `textStatus`

<a id="r-get-sni-api-v1-ged-trash"></a>
### GET /sni/api/v1/ged/trash

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `List<GedDtos.GedTrashBatch>`
- Modèles : [GedDtos.GedTrashBatch](#m-GedDtos-GedTrashBatch)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `trashContent`

<a id="r-post-sni-api-v1-ged-trash"></a>
### POST /sni/api/v1/ged/trash

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (obligatoire) : `GedDtos.GedSelectionRequest`
- Réponse : `GedDtos.GedTrashResult`
- Modèles : [GedDtos.GedTrashResult](#m-GedDtos-GedTrashResult), [GedDtos.GedSelectionRequest](#m-GedDtos-GedSelectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `trash`

<a id="r-post-sni-api-v1-ged-trash-batch-restore"></a>
### POST /sni/api/v1/ged/trash/{batch}/restore

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `GedDtos.GedTrashResult`
- Modèles : [GedDtos.GedTrashResult](#m-GedDtos-GedTrashResult)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `restore`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batch` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-ged-tree"></a>
### GET /sni/api/v1/ged/tree

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `List<DocumentFolderTreeNode>`
- Modèles : [DocumentFolderTreeNode](#m-DocumentFolderTreeNode)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `tree`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-GedDtos-GedUploadResult"></a>
#### GedDtos.GedUploadResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `uploaded` | [GedDtos.GedItem](#m-GedDtos-GedItem)[] |  |  |
| `failed` | [GedDtos.GedUploadFailure](#m-GedDtos-GedUploadFailure)[] |  |  |

<a id="m-GedDtos-GedCopyRequest"></a>
#### GedDtos.GedCopyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCodes` | texte[] |  |  |
| `targetFolderCode` | texte |  |  |

<a id="m-DocumentOwnerType"></a>
#### DocumentOwnerType

Énumération : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL`

<a id="m-GedDtos-GedItem"></a>
#### GedDtos.GedItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `folderCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `childrenCount` | entier int64 ⚠ |  |  |
| `documentsCount` | entier int64 ⚠ |  |  |
| `color` | texte |  |  |
| `icon` | texte |  |  |
| `fileName` | texte |  |  |
| `extension` | texte |  |  |
| `mimeType` | texte |  |  |
| `size` | entier int64 ⚠ |  |  |
| `status` | texte |  |  |
| `documentTypeCode` | texte |  |  |
| `documentTypeName` | texte |  |  |
| `ownerType` | texte |  |  |
| `version` | entier |  |  |
| `protectedItem` | booléen |  |  |
| `previewUrl` | texte |  |  |
| `downloadUrl` | texte |  |  |

<a id="m-GedDtos-GedRenameRequest"></a>
#### GedDtos.GedRenameRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |

<a id="m-GedDtos-GedFileText"></a>
#### GedDtos.GedFileText

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `status` | texte |  |  |
| `method` | texte |  |  |
| `pageCount` | entier |  |  |
| `ocrPages` | entier |  |  |
| `length` | entier |  |  |
| `truncated` | booléen |  |  |
| `extractedAt` | date-heure ISO 8601 UTC |  |  |
| `error` | texte |  |  |
| `upToDate` | booléen |  |  |
| `content` | texte |  |  |

<a id="m-GedDtos-GedFilingResult"></a>
#### GedDtos.GedFilingResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `filed` | entier |  |  |
| `skipped` | entier |  |  |
| `remaining` | entier int64 ⚠ |  |  |

<a id="m-GedDtos-GedCreateFolderRequest"></a>
#### GedDtos.GedCreateFolderRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `parentCode` | texte |  |  |
| `description` | texte |  |  |
| `color` | texte |  |  |
| `icon` | texte |  |  |

<a id="m-GedDtos-GedListing"></a>
#### GedDtos.GedListing

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `folder` | [GedDtos.GedFolderRef](#m-GedDtos-GedFolderRef) |  |  |
| `breadcrumb` | [BreadcrumbItem](#m-BreadcrumbItem)[] |  |  |
| `items` | [GedDtos.GedItem](#m-GedDtos-GedItem)[] |  |  |
| `page` | [GedDtos.GedPage](#m-GedDtos-GedPage) |  |  |

<a id="m-GedDtos-GedMoveRequest"></a>
#### GedDtos.GedMoveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCodes` | texte[] |  |  |
| `folderCodes` | texte[] |  |  |
| `targetFolderCode` | texte |  |  |

<a id="m-GedDtos-GedSearchResult"></a>
#### GedDtos.GedSearchResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `query` | texte |  |  |
| `folderCode` | texte |  |  |
| `hits` | [GedDtos.GedSearchHit](#m-GedDtos-GedSearchHit)[] |  |  |
| `page` | entier |  |  |
| `size` | entier |  |  |
| `totalElements` | entier int64 ⚠ |  |  |
| `totalPages` | entier |  |  |

<a id="m-GedDtos-GedTextRunResult"></a>
#### GedDtos.GedTextRunResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `processed` | entier |  |  |
| `done` | entier |  |  |
| `empty` | entier |  |  |
| `unsupported` | entier |  |  |
| `failed` | entier |  |  |
| `remaining` | entier int64 ⚠ |  |  |

<a id="m-GedDtos-GedTextStatus"></a>
#### GedDtos.GedTextStatus

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `pending` | entier int64 ⚠ |  |  |
| `byStatus` | objet { clé : entier int64 ⚠ } |  |  |

<a id="m-GedDtos-GedTrashBatch"></a>
#### GedDtos.GedTrashBatch

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `batch` | texte |  |  |
| `trashedAt` | date-heure ISO 8601 UTC |  |  |
| `trashedBy` | entier int64 ⚠ |  |  |
| `folders` | entier int64 ⚠ |  |  |
| `documents` | entier int64 ⚠ |  |  |
| `items` | [GedDtos.GedItem](#m-GedDtos-GedItem)[] |  |  |

<a id="m-GedDtos-GedTrashResult"></a>
#### GedDtos.GedTrashResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `batch` | texte |  |  |
| `folders` | entier int64 ⚠ |  |  |
| `documents` | entier int64 ⚠ |  |  |

<a id="m-GedDtos-GedSelectionRequest"></a>
#### GedDtos.GedSelectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCodes` | texte[] |  |  |
| `folderCodes` | texte[] |  |  |

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

<a id="m-GedDtos-GedUploadFailure"></a>
#### GedDtos.GedUploadFailure

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fileName` | texte |  |  |
| `message` | texte |  |  |

<a id="m-GedDtos-GedFolderRef"></a>
#### GedDtos.GedFolderRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `parentCode` | texte |  |  |

<a id="m-BreadcrumbItem"></a>
#### BreadcrumbItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `depth` | entier |  |  |

<a id="m-GedDtos-GedPage"></a>
#### GedDtos.GedPage

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `page` | entier |  |  |
| `size` | entier |  |  |
| `totalElements` | entier int64 ⚠ |  |  |
| `totalPages` | entier |  |  |
| `folderCount` | entier int64 ⚠ |  |  |
| `fileCount` | entier int64 ⚠ |  |  |

<a id="m-GedDtos-GedSearchHit"></a>
#### GedDtos.GedSearchHit

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `item` | [GedDtos.GedItem](#m-GedDtos-GedItem) |  |  |
| `snippet` | texte |  |  |
| `rank` | nombre décimal |  |  |
| `location` | texte |  |  |
