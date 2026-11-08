# ContractController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [contract](../README.md) · 18 route(s).

## Routes

<a id="r-get-sni-api-v1-contracts"></a>
### GET /sni/api/v1/contracts

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<ContractResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ContractResponse](#m-ContractResponse), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `status` | requête | `String` |  |  |
| `businessCode` | requête | `String` |  |  |
| `templateCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  |  |

<a id="r-post-sni-api-v1-contracts"></a>
### POST /sni/api/v1/contracts

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateContractRequest`
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse), [CreateContractRequest](#m-CreateContractRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `create`

<a id="r-post-sni-api-v1-contracts-generate"></a>
### POST /sni/api/v1/contracts/generate

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_GENERATE_DRAFT`, voir le README racine)
- Corps JSON (obligatoire) : `GenerateContractRequest`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse), [GenerateContractRequest](#m-GenerateContractRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `generate`

<a id="r-post-sni-api-v1-contracts-preview"></a>
### POST /sni/api/v1/contracts/preview

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `GenerateContractRequest`
- Réponse : `ContractPreviewResponse`
- Modèles : [ContractPreviewResponse](#m-ContractPreviewResponse), [GenerateContractRequest](#m-GenerateContractRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `preview`

<a id="r-get-sni-api-v1-contracts-templates"></a>
### GET /sni/api/v1/contracts/templates

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ContractTemplateResponse>`
- Modèles : [ContractTemplateResponse](#m-ContractTemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `listTemplates`

<a id="r-get-sni-api-v1-contracts-templates-drafting"></a>
### GET /sni/api/v1/contracts/templates-drafting

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `getTemplateDraftingReference`

<a id="r-delete-sni-api-v1-contracts-contractcode"></a>
### DELETE /sni/api/v1/contracts/{contractCode}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-contracts-contractcode"></a>
### GET /sni/api/v1/contracts/{contractCode}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-contracts-contractcode"></a>
### PUT /sni/api/v1/contracts/{contractCode}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateContractRequest`
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse), [UpdateContractRequest](#m-UpdateContractRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-contracts-contractcode-activate"></a>
### PATCH /sni/api/v1/contracts/{contractCode}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_ACTIVATE`, voir le README racine)
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-contracts-contractcode-approve"></a>
### POST /sni/api/v1/contracts/{contractCode}/approve

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `approveReview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |
| `reviewedBy` | requête | `Long` | oui |  |
| `comment` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-contracts-contractcode-cancel"></a>
### PATCH /sni/api/v1/contracts/{contractCode}/cancel

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_CANCEL`, voir le README racine)
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-contracts-contractcode-generate-draft"></a>
### POST /sni/api/v1/contracts/{contractCode}/generate-draft

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_GENERATE_DRAFT_FROM_RECORD`, voir le README racine)
- Corps JSON (obligatoire) : `GenerateStoredContractDraftRequest`
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse), [GenerateStoredContractDraftRequest](#m-GenerateStoredContractDraftRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `generateFromContract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-contracts-contractcode-reject-review"></a>
### POST /sni/api/v1/contracts/{contractCode}/reject-review

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `rejectReview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |
| `reviewedBy` | requête | `Long` | oui |  |
| `comment` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-contracts-contractcode-signed-document"></a>
### PATCH /sni/api/v1/contracts/{contractCode}/signed-document

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_MARK_SIGNED`, voir le README racine)
- Corps JSON (obligatoire) : `AttachContractDocumentRequest`
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse), [AttachContractDocumentRequest](#m-AttachContractDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `markSigned`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-contracts-contractcode-status"></a>
### PATCH /sni/api/v1/contracts/{contractCode}/status

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_STATUS_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateContractStatusRequest`
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse), [UpdateContractStatusRequest](#m-UpdateContractStatusRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `updateStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-contracts-contractcode-suspend"></a>
### PATCH /sni/api/v1/contracts/{contractCode}/suspend

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_SUSPEND`, voir le README racine)
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `suspend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-contracts-contractcode-terminate"></a>
### PATCH /sni/api/v1/contracts/{contractCode}/terminate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_TERMINATE`, voir le README racine)
- Réponse : `ContractResponse`
- Modèles : [ContractResponse](#m-ContractResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractController.java`, méthode `terminate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ContractResponse"></a>
#### ContractResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `contractCode` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `templateCode` | texte |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` |  |  |
| `ownerCode` | texte |  |  |
| `businessCode` | texte |  |  |
| `status` | enum : `DRAFT`, `GENERATED`, `UNDER_REVIEW`, `AWAITING_SIGNATURE`, `SIGNED`, `ACTIVE`, `SUSPENDED`, `AMENDED`, `EXPIRED`, `TERMINATED`, `CANCELLED` |  |  |
| `renewalType` | enum : `NONE`, `FIXED_TERM`, `AUTO_RENEW` |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `activatedAt` | date-heure ISO 8601 UTC |  |  |
| `terminatedAt` | date-heure ISO 8601 UTC |  |  |
| `terminationReason` | texte |  |  |
| `draftDocumentCode` | texte |  |  |
| `signedDocumentCode` | texte |  |  |
| `createdBy` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `parties` | [ContractPartyResponse](#m-ContractPartyResponse)[] |  |  |

<a id="m-DocumentOwnerType"></a>
#### DocumentOwnerType

Énumération : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL`

<a id="m-CreateContractRequest"></a>
#### CreateContractRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `templateCode` | texte | oui | longueur min `1` |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `businessCode` | texte |  |  |
| `renewalType` | enum : `NONE`, `FIXED_TERM`, `AUTO_RENEW` |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `createdBy` | entier int64 ⚠ | oui |  |
| `parties` | [ContractPartyRequest](#m-ContractPartyRequest)[] |  |  |

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

<a id="m-GenerateContractRequest"></a>
#### GenerateContractRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte | oui | longueur min `1` |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `businessCode` | texte |  |  |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `uploadedBy` | entier int64 ⚠ |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `signatoryName` | texte |  |  |
| `signatoryRole` | texte |  |  |
| `clauses` | texte[] |  |  |
| `variables` | objet { clé : texte } |  |  |

<a id="m-ContractPreviewResponse"></a>
#### ContractPreviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte |  |  |
| `html` | texte |  |  |

<a id="m-ContractTemplateResponse"></a>
#### ContractTemplateResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `language` | texte |  |  |
| `version` | entier |  |  |
| `category` | texte |  |  |
| `htmlContent` | texte |  |  |
| `cssContent` | texte |  |  |
| `headerHtml` | texte |  |  |
| `footerHtml` | texte |  |  |
| `variableDefinitions` | [TemplateVariableDefinition](#m-TemplateVariableDefinition)[] |  |  |
| `sections` | [ContractTemplateSectionResponse](#m-ContractTemplateSectionResponse)[] |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-UpdateContractRequest"></a>
#### UpdateContractRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `templateCode` | texte |  |  |
| `businessCode` | texte |  |  |
| `renewalType` | enum : `NONE`, `FIXED_TERM`, `AUTO_RENEW` |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `parties` | [ContractPartyRequest](#m-ContractPartyRequest)[] |  |  |

<a id="m-GenerateStoredContractDraftRequest"></a>
#### GenerateStoredContractDraftRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `uploadedBy` | entier int64 ⚠ | oui |  |

<a id="m-AttachContractDocumentRequest"></a>
#### AttachContractDocumentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte | oui | longueur min `1` |

<a id="m-UpdateContractStatusRequest"></a>
#### UpdateContractStatusRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | texte | oui | longueur min `1` |
| `reason` | texte |  |  |

<a id="m-ContractPartyResponse"></a>
#### ContractPartyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `partyType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` |  |  |
| `partyCode` | texte |  |  |
| `displayName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `role` | enum : `BENEFICIARY`, `OPERATOR`, `SIGNATORY`, `BILLING_CONTACT` |  |  |
| `signOrder` | entier |  |  |
| `mustSign` | booléen |  |  |

<a id="m-ContractPartyRequest"></a>
#### ContractPartyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `partyType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL` | oui |  |
| `partyCode` | texte | oui | longueur min `1` |
| `displayName` | texte | oui | longueur min `1` |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `role` | enum : `BENEFICIARY`, `OPERATOR`, `SIGNATORY`, `BILLING_CONTACT` | oui |  |
| `signOrder` | entier |  |  |
| `mustSign` | booléen |  |  |

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

<a id="m-TemplateVariableDefinition"></a>
#### TemplateVariableDefinition

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `label` | texte |  |  |
| `type` | texte |  |  |
| `required` | booléen |  |  |
| `defaultValue` | texte |  |  |
| `source` | texte |  |  |
| `description` | texte |  |  |

<a id="m-ContractTemplateSectionResponse"></a>
#### ContractTemplateSectionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `templateCode` | texte |  |  |
| `title` | texte |  |  |
| `content` | texte |  |  |
| `sectionType` | enum : `PREAMBLE`, `ARTICLE`, `CLAUSE`, `SIGNATURE_BLOCK`, `ANNEXE` |  |  |
| `sectionOrder` | entier |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
