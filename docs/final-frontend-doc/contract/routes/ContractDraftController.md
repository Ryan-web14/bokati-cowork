# ContractDraftController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [contract](../README.md) · 13 route(s).

## Routes

<a id="r-get-sni-api-v1-contract-drafts"></a>
### GET /sni/api/v1/contract-drafts

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<ContractDraftResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ContractDraftResponse](#m-ContractDraftResponse), [ContractDraftStatus](#m-ContractDraftStatus), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `ContractDraftStatus` |  |  |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-contract-drafts"></a>
### POST /sni/api/v1/contract-drafts

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_DRAFT_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateContractDraftRequest`
- Réponse : `ContractDraftResponse`
- Modèles : [ContractDraftResponse](#m-ContractDraftResponse), [CreateContractDraftRequest](#m-CreateContractDraftRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `create`

<a id="r-post-sni-api-v1-contract-drafts-from-template-templatecode"></a>
### POST /sni/api/v1/contract-drafts/from-template/{templateCode}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_DRAFT_CREATE_FROM_TEMPLATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateContractDraftRequest`
- Réponse : `ContractDraftResponse`
- Modèles : [ContractDraftResponse](#m-ContractDraftResponse), [CreateContractDraftRequest](#m-CreateContractDraftRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `createFromTemplate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `templateCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-contract-drafts-search"></a>
### GET /sni/api/v1/contract-drafts/search

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<ContractDraftResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ContractDraftResponse](#m-ContractDraftResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-delete-sni-api-v1-contract-drafts-code"></a>
### DELETE /sni/api/v1/contract-drafts/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-contract-drafts-code"></a>
### GET /sni/api/v1/contract-drafts/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `ContractDraftResponse`
- Modèles : [ContractDraftResponse](#m-ContractDraftResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-contract-drafts-code"></a>
### PUT /sni/api/v1/contract-drafts/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_DRAFT_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateContractDraftRequest`
- Réponse : `ContractDraftResponse`
- Modèles : [ContractDraftResponse](#m-ContractDraftResponse), [UpdateContractDraftRequest](#m-UpdateContractDraftRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-contract-drafts-code-generate"></a>
### POST /sni/api/v1/contract-drafts/{code}/generate

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_DRAFT_GENERATE_PDF`, voir le README racine)
- Corps JSON (facultatif) : `PreviewContractDraftRequest`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [PreviewContractDraftRequest](#m-PreviewContractDraftRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `generatePdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `uploadedBy` | requête | `Long` |  |  |

<a id="r-post-sni-api-v1-contract-drafts-code-preview"></a>
### POST /sni/api/v1/contract-drafts/{code}/preview

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (facultatif) : `PreviewContractDraftRequest`
- Réponse : `ContractPreviewResponse`
- Modèles : [ContractPreviewResponse](#m-ContractPreviewResponse), [PreviewContractDraftRequest](#m-PreviewContractDraftRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-contract-drafts-code-sections"></a>
### POST /sni/api/v1/contract-drafts/{code}/sections

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ContractDraftSectionRequest`
- Réponse : `ContractDraftSectionResponse`
- Modèles : [ContractDraftSectionResponse](#m-ContractDraftSectionResponse), [ContractDraftSectionRequest](#m-ContractDraftSectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `addSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-contract-drafts-code-sections-sectionid"></a>
### DELETE /sni/api/v1/contract-drafts/{code}/sections/{sectionId}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `removeSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `sectionId` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-contract-drafts-code-sections-sectionid"></a>
### PUT /sni/api/v1/contract-drafts/{code}/sections/{sectionId}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ContractDraftSectionRequest`
- Réponse : `ContractDraftSectionResponse`
- Modèles : [ContractDraftSectionResponse](#m-ContractDraftSectionResponse), [ContractDraftSectionRequest](#m-ContractDraftSectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `updateSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `sectionId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-contract-drafts-code-status"></a>
### PATCH /sni/api/v1/contract-drafts/{code}/status

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `ContractDraftResponse`
- Modèles : [ContractDraftResponse](#m-ContractDraftResponse), [ContractDraftStatus](#m-ContractDraftStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractDraftController.java`, méthode `updateStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `status` | requête | `ContractDraftStatus` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ContractDraftResponse"></a>
#### ContractDraftResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `templateCode` | texte |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `ownerCode` | texte |  |  |
| `businessCode` | texte |  |  |
| `cssContent` | texte |  |  |
| `headerHtml` | texte |  |  |
| `footerHtml` | texte |  |  |
| `status` | enum : `DRAFTING`, `READY`, `GENERATED` |  |  |
| `createdBy` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `sections` | [ContractDraftSectionResponse](#m-ContractDraftSectionResponse)[] |  |  |

<a id="m-ContractDraftStatus"></a>
#### ContractDraftStatus

Énumération : `DRAFTING`, `READY`, `GENERATED`

<a id="m-DocumentOwnerType"></a>
#### DocumentOwnerType

Énumération : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`

<a id="m-CreateContractDraftRequest"></a>
#### CreateContractDraftRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `templateCode` | texte |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `ownerCode` | texte |  |  |
| `businessCode` | texte |  |  |
| `cssContent` | texte |  |  |
| `headerHtml` | texte |  |  |
| `footerHtml` | texte |  |  |
| `createdBy` | entier int64 ⚠ | oui |  |
| `sections` | [ContractDraftSectionRequest](#m-ContractDraftSectionRequest)[] |  |  |

<a id="m-UpdateContractDraftRequest"></a>
#### UpdateContractDraftRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `ownerCode` | texte |  |  |
| `businessCode` | texte |  |  |
| `cssContent` | texte |  |  |
| `headerHtml` | texte |  |  |
| `footerHtml` | texte |  |  |
| `sections` | [ContractDraftSectionRequest](#m-ContractDraftSectionRequest)[] |  |  |

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

<a id="m-PreviewContractDraftRequest"></a>
#### PreviewContractDraftRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `variables` | objet { clé : texte } |  |  |

<a id="m-ContractPreviewResponse"></a>
#### ContractPreviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte |  |  |
| `html` | texte |  |  |

<a id="m-ContractDraftSectionResponse"></a>
#### ContractDraftSectionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `title` | texte |  |  |
| `content` | texte |  |  |
| `sectionType` | enum : `PREAMBLE`, `ARTICLE`, `CLAUSE`, `SIGNATURE_BLOCK`, `ANNEXE` |  |  |
| `sectionOrder` | entier |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ContractDraftSectionRequest"></a>
#### ContractDraftSectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `content` | texte | oui | longueur min `1` |
| `sectionType` | enum : `PREAMBLE`, `ARTICLE`, `CLAUSE`, `SIGNATURE_BLOCK`, `ANNEXE` | oui |  |
| `sectionOrder` | entier |  |  |
| `active` | booléen |  |  |

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
