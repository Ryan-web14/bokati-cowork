# GedExplorerController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 36 route(s).

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

<a id="r-post-sni-api-v1-ged-files-code-lock"></a>
### POST /sni/api/v1/ged/files/{code}/lock

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `GedDtos.GedItem`
- Modèles : [GedDtos.GedItem](#m-GedDtos-GedItem)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `lock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `hours` | requête | `Integer` |  |  |

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

<a id="r-post-sni-api-v1-ged-files-code-unlock"></a>
### POST /sni/api/v1/ged/files/{code}/unlock

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `GedDtos.GedItem`
- Modèles : [GedDtos.GedItem](#m-GedDtos-GedItem)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `unlock`

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

<a id="r-get-sni-api-v1-ged-folders-code-access"></a>
### GET /sni/api/v1/ged/folders/{code}/access

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `GedDtos.GedFolderAccess`
- Modèles : [GedDtos.GedFolderAccess](#m-GedDtos-GedFolderAccess)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `folderAccess`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-ged-folders-code-grants"></a>
### DELETE /sni/api/v1/ged/folders/{code}/grants

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `GedDtos.GedFolderAccess`
- Modèles : [GedDtos.GedFolderAccess](#m-GedDtos-GedFolderAccess)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `granteeType` | requête | `String` | oui |  |
| `grantee` | requête | `String` | oui |  |

<a id="r-put-sni-api-v1-ged-folders-code-grants"></a>
### PUT /sni/api/v1/ged/folders/{code}/grants

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (obligatoire) : `GedDtos.GedGrantRequest`
- Réponse : `GedDtos.GedFolderAccess`
- Modèles : [GedDtos.GedFolderAccess](#m-GedDtos-GedFolderAccess), [GedDtos.GedGrantRequest](#m-GedDtos-GedGrantRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `grant`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-ged-folders-code-quota"></a>
### PUT /sni/api/v1/ged/folders/{code}/quota

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `GedDtos.GedQuotaRequest`
- Réponse : `GedDtos.GedUsage`
- Modèles : [GedDtos.GedUsage](#m-GedDtos-GedUsage), [GedDtos.GedQuotaRequest](#m-GedDtos-GedQuotaRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `setQuota`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-ged-folders-code-restrict"></a>
### POST /sni/api/v1/ged/folders/{code}/restrict

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (facultatif) : `GedDtos.GedRestrictRequest`
- Réponse : `GedDtos.GedFolderAccess`
- Modèles : [GedDtos.GedFolderAccess](#m-GedDtos-GedFolderAccess), [GedDtos.GedRestrictRequest](#m-GedDtos-GedRestrictRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `restrict`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-ged-folders-code-unrestrict"></a>
### POST /sni/api/v1/ged/folders/{code}/unrestrict

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `GedDtos.GedFolderAccess`
- Modèles : [GedDtos.GedFolderAccess](#m-GedDtos-GedFolderAccess)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `unrestrict`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-ged-holds"></a>
### GET /sni/api/v1/ged/holds

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `List<GedDtos.GedHold>`
- Modèles : [GedDtos.GedHold](#m-GedDtos-GedHold)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `holds`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `boolean` |  | true |

<a id="r-post-sni-api-v1-ged-holds"></a>
### POST /sni/api/v1/ged/holds

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `GedDtos.GedHoldRequest`
- Réponse : `GedDtos.GedHold`
- Modèles : [GedDtos.GedHold](#m-GedDtos-GedHold), [GedDtos.GedHoldRequest](#m-GedDtos-GedHoldRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `placeHold`

<a id="r-post-sni-api-v1-ged-holds-code-release"></a>
### POST /sni/api/v1/ged/holds/{code}/release

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `GedDtos.GedReasonRequest`
- Réponse : `GedDtos.GedHold`
- Modèles : [GedDtos.GedHold](#m-GedDtos-GedHold), [GedDtos.GedReasonRequest](#m-GedDtos-GedReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `releaseHold`

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

<a id="r-post-sni-api-v1-ged-retention-destroy"></a>
### POST /sni/api/v1/ged/retention/destroy

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `GedDtos.GedDocumentsDecision`
- Réponse : `GedDtos.GedPurgeResult`
- Modèles : [GedDtos.GedPurgeResult](#m-GedDtos-GedPurgeResult), [GedDtos.GedDocumentsDecision](#m-GedDtos-GedDocumentsDecision)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `destroy`

<a id="r-get-sni-api-v1-ged-retention-due"></a>
### GET /sni/api/v1/ged/retention/due

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `List<GedDtos.GedDestructionCandidate>`
- Modèles : [GedDtos.GedDestructionCandidate](#m-GedDtos-GedDestructionCandidate)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `destructionDue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 200 |

<a id="r-post-sni-api-v1-ged-retention-keep"></a>
### POST /sni/api/v1/ged/retention/keep

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `GedDtos.GedDocumentsDecision`
- Réponse : `Map<String, Integer>`
- Modèles : [GedDtos.GedDocumentsDecision](#m-GedDtos-GedDocumentsDecision)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `keep`

<a id="r-post-sni-api-v1-ged-retention-run"></a>
### POST /sni/api/v1/ged/retention/run

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `runRetention`

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

<a id="r-post-sni-api-v1-ged-trash-empty"></a>
### POST /sni/api/v1/ged/trash/empty

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `GedDtos.GedReasonRequest`
- Réponse : `GedDtos.GedPurgeResult`
- Modèles : [GedDtos.GedPurgeResult](#m-GedDtos-GedPurgeResult), [GedDtos.GedReasonRequest](#m-GedDtos-GedReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `emptyTrash`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `olderThanDays` | requête | `int` |  | 0 |

<a id="r-post-sni-api-v1-ged-trash-batch-purge"></a>
### POST /sni/api/v1/ged/trash/{batch}/purge

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `GedDtos.GedReasonRequest`
- Réponse : `GedDtos.GedPurgeResult`
- Modèles : [GedDtos.GedPurgeResult](#m-GedDtos-GedPurgeResult), [GedDtos.GedReasonRequest](#m-GedDtos-GedReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `purgeBatch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batch` | chemin | `String` | oui |  |

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

<a id="r-get-sni-api-v1-ged-usage"></a>
### GET /sni/api/v1/ged/usage

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `GedDtos.GedUsage`
- Modèles : [GedDtos.GedUsage](#m-GedDtos-GedUsage)
- Code : `src/main/java/com/sni/bokaticowork/features/document/explorer/controller/GedExplorerController.java`, méthode `usage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `folderCode` | requête | `String` |  |  |

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
| `restricted` | booléen |  |  |
| `lock` | [GedDtos.GedLock](#m-GedDtos-GedLock) |  |  |
| `held` | booléen |  |  |
| `destructionDueAt` | date-heure ISO 8601 UTC |  |  |

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

<a id="m-GedDtos-GedFolderAccess"></a>
#### GedDtos.GedFolderAccess

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `folderCode` | texte |  |  |
| `restricted` | booléen |  |  |
| `restrictedAt` | date-heure ISO 8601 UTC |  |  |
| `restrictedBy` | texte |  |  |
| `systemFolder` | booléen |  |  |
| `inheritedFrom` | [GedDtos.GedFolderRef](#m-GedDtos-GedFolderRef)[] |  |  |
| `grants` | [GedDtos.GedGrant](#m-GedDtos-GedGrant)[] |  |  |
| `myLevel` | enum : `NONE`, `READ`, `DOWNLOAD`, `EDIT`, `DELETE`, `ADMIN` |  |  |
| `canManage` | booléen |  |  |

<a id="m-GedDtos-GedGrantRequest"></a>
#### GedDtos.GedGrantRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `granteeType` | texte | oui | longueur min `1` |
| `grantee` | texte | oui | longueur min `1` |
| `level` | enum : `NONE`, `READ`, `DOWNLOAD`, `EDIT`, `DELETE`, `ADMIN` | oui |  |

<a id="m-GedDtos-GedUsage"></a>
#### GedDtos.GedUsage

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `folderCode` | texte |  |  |
| `folderName` | texte |  |  |
| `usedBytes` | entier int64 ⚠ |  |  |
| `files` | entier int64 ⚠ |  |  |
| `quotaBytes` | entier int64 ⚠ |  |  |
| `remainingBytes` | entier int64 ⚠ |  |  |
| `limitingFolderCode` | texte |  |  |
| `totalUsedBytes` | entier int64 ⚠ |  |  |
| `totalQuotaBytes` | entier int64 ⚠ |  |  |

<a id="m-GedDtos-GedQuotaRequest"></a>
#### GedDtos.GedQuotaRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `quotaBytes` | entier int64 ⚠ |  |  |

<a id="m-GedDtos-GedRestrictRequest"></a>
#### GedDtos.GedRestrictRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `grants` | [GedDtos.GedGrantRequest](#m-GedDtos-GedGrantRequest)[] |  |  |

<a id="m-GedDtos-GedHold"></a>
#### GedDtos.GedHold

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `targetType` | texte |  |  |
| `targetCode` | texte |  |  |
| `targetName` | texte |  |  |
| `reason` | texte |  |  |
| `reference` | texte |  |  |
| `placedBy` | texte |  |  |
| `placedAt` | date-heure ISO 8601 UTC |  |  |
| `releasedBy` | texte |  |  |
| `releasedAt` | date-heure ISO 8601 UTC |  |  |
| `releaseReason` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-GedDtos-GedHoldRequest"></a>
#### GedDtos.GedHoldRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `targetType` | texte | oui | longueur min `1` |
| `targetCode` | texte | oui | longueur min `1` |
| `reason` | texte | oui | longueur min `1` |
| `reference` | texte |  |  |

<a id="m-GedDtos-GedReasonRequest"></a>
#### GedDtos.GedReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |

<a id="m-GedDtos-GedListing"></a>
#### GedDtos.GedListing

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `folder` | [GedDtos.GedFolderRef](#m-GedDtos-GedFolderRef) |  |  |
| `breadcrumb` | [BreadcrumbItem](#m-BreadcrumbItem)[] |  |  |
| `items` | [GedDtos.GedItem](#m-GedDtos-GedItem)[] |  |  |
| `page` | [GedDtos.GedPage](#m-GedDtos-GedPage) |  |  |
| `access` | [GedDtos.GedAccessSummary](#m-GedDtos-GedAccessSummary) |  |  |

<a id="m-GedDtos-GedMoveRequest"></a>
#### GedDtos.GedMoveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCodes` | texte[] |  |  |
| `folderCodes` | texte[] |  |  |
| `targetFolderCode` | texte |  |  |

<a id="m-GedDtos-GedPurgeResult"></a>
#### GedDtos.GedPurgeResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `batches` | entier |  |  |
| `folders` | entier |  |  |
| `documents` | entier |  |  |
| `filesErased` | entier |  |  |
| `heldSkipped` | entier |  |  |
| `failed` | entier |  |  |

<a id="m-GedDtos-GedDocumentsDecision"></a>
#### GedDtos.GedDocumentsDecision

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCodes` | texte[] | oui | éléments min `1` |
| `reason` | texte | oui | longueur min `1` |

<a id="m-GedDtos-GedDestructionCandidate"></a>
#### GedDtos.GedDestructionCandidate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `item` | [GedDtos.GedItem](#m-GedDtos-GedItem) |  |  |
| `policyCode` | texte |  |  |
| `dueAt` | date-heure ISO 8601 UTC |  |  |
| `location` | texte |  |  |

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

<a id="m-GedDtos-GedLock"></a>
#### GedDtos.GedLock

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lockedBy` | texte |  |  |
| `lockedAt` | date-heure ISO 8601 UTC |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `mine` | booléen |  |  |

<a id="m-GedDtos-GedFolderRef"></a>
#### GedDtos.GedFolderRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `parentCode` | texte |  |  |

<a id="m-GedDtos-GedGrant"></a>
#### GedDtos.GedGrant

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `granteeType` | texte |  |  |
| `grantee` | texte |  |  |
| `granteeLabel` | texte |  |  |
| `level` | enum : `NONE`, `READ`, `DOWNLOAD`, `EDIT`, `DELETE`, `ADMIN` |  |  |
| `grantedAt` | date-heure ISO 8601 UTC |  |  |
| `grantedBy` | texte |  |  |

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

<a id="m-GedDtos-GedAccessSummary"></a>
#### GedDtos.GedAccessSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `level` | enum : `NONE`, `READ`, `DOWNLOAD`, `EDIT`, `DELETE`, `ADMIN` |  |  |
| `canDownload` | booléen |  |  |
| `canEdit` | booléen |  |  |
| `canDelete` | booléen |  |  |
| `canManage` | booléen |  |  |

<a id="m-GedDtos-GedSearchHit"></a>
#### GedDtos.GedSearchHit

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `item` | [GedDtos.GedItem](#m-GedDtos-GedItem) |  |  |
| `snippet` | texte |  |  |
| `rank` | nombre décimal |  |  |
| `location` | texte |  |  |
