# ContractAmendmentController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [contract](../README.md) · 18 route(s).

## Routes

<a id="r-get-sni-api-v1-amendments-amendmentcode"></a>
### GET /sni/api/v1/amendments/{amendmentCode}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `ContractAmendmentResponse`
- Modèles : [ContractAmendmentResponse](#m-ContractAmendmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-amendments-amendmentcode-approve"></a>
### POST /sni/api/v1/amendments/{amendmentCode}/approve

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (facultatif) : `ReviewAmendmentRequest`
- Réponse : `ContractAmendmentResponse`
- Modèles : [ContractAmendmentResponse](#m-ContractAmendmentResponse), [ReviewAmendmentRequest](#m-ReviewAmendmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `approveReview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |
| `reviewedBy` | requête | `Long` | oui |  |

<a id="r-post-sni-api-v1-amendments-amendmentcode-cancel"></a>
### POST /sni/api/v1/amendments/{amendmentCode}/cancel

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (facultatif) : `CancelAmendmentRequest`
- Réponse : `ContractAmendmentResponse`
- Modèles : [ContractAmendmentResponse](#m-ContractAmendmentResponse), [CancelAmendmentRequest](#m-CancelAmendmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |
| `actorId` | requête | `Long` | oui |  |

<a id="r-post-sni-api-v1-amendments-amendmentcode-generate-pdf"></a>
### POST /sni/api/v1/amendments/{amendmentCode}/generate-pdf

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `DocumentResponse`
- Modèles : [DocumentResponse](#m-DocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `generatePdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |
| `uploadedBy` | requête | `Long` | oui |  |

<a id="r-get-sni-api-v1-amendments-amendmentcode-preview"></a>
### GET /sni/api/v1/amendments/{amendmentCode}/preview

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Renvoie : `text/html`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `previewHtml`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-amendments-amendmentcode-reject"></a>
### POST /sni/api/v1/amendments/{amendmentCode}/reject

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (facultatif) : `ReviewAmendmentRequest`
- Réponse : `ContractAmendmentResponse`
- Modèles : [ContractAmendmentResponse](#m-ContractAmendmentResponse), [ReviewAmendmentRequest](#m-ReviewAmendmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `rejectReview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |
| `reviewedBy` | requête | `Long` | oui |  |

<a id="r-get-sni-api-v1-amendments-amendmentcode-sections"></a>
### GET /sni/api/v1/amendments/{amendmentCode}/sections

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<ContractAmendmentSectionResponse>`
- Modèles : [ContractAmendmentSectionResponse](#m-ContractAmendmentSectionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `listSections`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-amendments-amendmentcode-sections"></a>
### POST /sni/api/v1/amendments/{amendmentCode}/sections

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ContractAmendmentSectionRequest`
- Réponse : `ContractAmendmentSectionResponse`
- Modèles : [ContractAmendmentSectionResponse](#m-ContractAmendmentSectionResponse), [ContractAmendmentSectionRequest](#m-ContractAmendmentSectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `addSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-amendments-amendmentcode-sections-sectionid"></a>
### DELETE /sni/api/v1/amendments/{amendmentCode}/sections/{sectionId}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `removeSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |
| `sectionId` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-amendments-amendmentcode-sections-sectionid"></a>
### PUT /sni/api/v1/amendments/{amendmentCode}/sections/{sectionId}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ContractAmendmentSectionRequest`
- Réponse : `ContractAmendmentSectionResponse`
- Modèles : [ContractAmendmentSectionResponse](#m-ContractAmendmentSectionResponse), [ContractAmendmentSectionRequest](#m-ContractAmendmentSectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `updateSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |
| `sectionId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-amendments-amendmentcode-sign"></a>
### POST /sni/api/v1/amendments/{amendmentCode}/sign

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (facultatif) : `SignAmendmentRequest`
- Réponse : `ContractAmendmentResponse`
- Modèles : [ContractAmendmentResponse](#m-ContractAmendmentResponse), [SignAmendmentRequest](#m-SignAmendmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `sign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |
| `actorId` | requête | `Long` | oui |  |

<a id="r-post-sni-api-v1-amendments-amendmentcode-submit"></a>
### POST /sni/api/v1/amendments/{amendmentCode}/submit

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `ContractAmendmentResponse`
- Modèles : [ContractAmendmentResponse](#m-ContractAmendmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `submitForReview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-amendments-amendmentcode-variables"></a>
### GET /sni/api/v1/amendments/{amendmentCode}/variables

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<ContractAmendmentVariableResponse>`
- Modèles : [ContractAmendmentVariableResponse](#m-ContractAmendmentVariableResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `listVariables`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-amendments-amendmentcode-variables"></a>
### PUT /sni/api/v1/amendments/{amendmentCode}/variables

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `List<ContractAmendmentVariableRequest>`
- Réponse : `List<ContractAmendmentVariableResponse>`
- Modèles : [ContractAmendmentVariableResponse](#m-ContractAmendmentVariableResponse), [ContractAmendmentVariableRequest](#m-ContractAmendmentVariableRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `setVariables`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `amendmentCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-contracts-contractcode-amendments"></a>
### GET /sni/api/v1/contracts/{contractCode}/amendments

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ContractAmendmentResponse>`
- Modèles : [ContractAmendmentResponse](#m-ContractAmendmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `listByContract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-contracts-contractcode-amendments"></a>
### POST /sni/api/v1/contracts/{contractCode}/amendments

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `ProposeAmendmentRequest`
- Réponse : `ContractAmendmentResponse`
- Modèles : [ContractAmendmentResponse](#m-ContractAmendmentResponse), [ProposeAmendmentRequest](#m-ProposeAmendmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `propose`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |
| `proposedBy` | requête | `Long` | oui |  |

<a id="r-get-sni-api-v1-contracts-contractcode-audit-events"></a>
### GET /sni/api/v1/contracts/{contractCode}/audit-events

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ContractAuditEventResponse>`
- Modèles : [ContractAuditEventResponse](#m-ContractAuditEventResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `getAuditTrail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-contracts-contractcode-versions"></a>
### GET /sni/api/v1/contracts/{contractCode}/versions

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ContractVersionHistory.Version>`
- Modèles : [ContractVersionHistory.Version](#m-ContractVersionHistory-Version)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractAmendmentController.java`, méthode `versions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ContractAmendmentResponse"></a>
#### ContractAmendmentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `originalContractCode` | texte |  |  |
| `status` | enum : `DRAFT`, `UNDER_REVIEW`, `PENDING_SIGNATURE`, `ACTIVE`, `REJECTED`, `CANCELLED` |  |  |
| `description` | texte |  |  |
| `proposedBy` | entier int64 ⚠ |  |  |
| `proposedAt` | date-heure ISO 8601 UTC |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `draftDocumentCode` | texte |  |  |
| `signedDocumentCode` | texte |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `activatedAt` | date-heure ISO 8601 UTC |  |  |
| `reviewedBy` | entier int64 ⚠ |  |  |
| `reviewComment` | texte |  |  |
| `rejectionReason` | texte |  |  |
| `cancellationReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `sections` | [ContractAmendmentSectionResponse](#m-ContractAmendmentSectionResponse)[] |  |  |
| `variableChanges` | [ContractAmendmentVariableResponse](#m-ContractAmendmentVariableResponse)[] |  |  |

<a id="m-ReviewAmendmentRequest"></a>
#### ReviewAmendmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `comment` | texte |  |  |

<a id="m-CancelAmendmentRequest"></a>
#### CancelAmendmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

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

<a id="m-ContractAmendmentSectionResponse"></a>
#### ContractAmendmentSectionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `action` | enum : `ADD`, `MODIFY`, `REMOVE` |  |  |
| `targetSectionRef` | texte |  |  |
| `sectionType` | enum : `PREAMBLE`, `ARTICLE`, `CLAUSE`, `SIGNATURE_BLOCK`, `ANNEXE` |  |  |
| `title` | texte |  |  |
| `content` | texte |  |  |
| `sectionOrder` | entier |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ContractAmendmentSectionRequest"></a>
#### ContractAmendmentSectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `action` | enum : `ADD`, `MODIFY`, `REMOVE` | oui |  |
| `targetSectionRef` | texte |  |  |
| `sectionType` | enum : `PREAMBLE`, `ARTICLE`, `CLAUSE`, `SIGNATURE_BLOCK`, `ANNEXE` |  |  |
| `title` | texte |  |  |
| `content` | texte |  |  |
| `sectionOrder` | entier |  |  |

<a id="m-SignAmendmentRequest"></a>
#### SignAmendmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signedDocumentCode` | texte |  |  |
| `justification` | texte |  |  |

<a id="m-ContractAmendmentVariableResponse"></a>
#### ContractAmendmentVariableResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `variableKey` | texte |  |  |
| `previousValue` | texte |  |  |
| `newValue` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ContractAmendmentVariableRequest"></a>
#### ContractAmendmentVariableRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `variableKey` | texte | oui | longueur min `1` |
| `previousValue` | texte |  |  |
| `newValue` | texte | oui | longueur min `1` |

<a id="m-ProposeAmendmentRequest"></a>
#### ProposeAmendmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `description` | texte | oui | longueur min `1` |
| `effectiveDate` | date AAAA-MM-JJ |  |  |

<a id="m-ContractAuditEventResponse"></a>
#### ContractAuditEventResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `contractCode` | texte |  |  |
| `amendmentCode` | texte |  |  |
| `eventType` | texte |  |  |
| `actorId` | entier int64 ⚠ |  |  |
| `actorType` | texte |  |  |
| `actorName` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
| `previousHash` | texte |  |  |
| `eventHash` | texte |  |  |
| `payload` | texte |  |  |
| `justification` | texte |  |  |

<a id="m-ContractVersionHistory-Version"></a>
#### ContractVersionHistory.Version

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `version` | entier |  |  |
| `kind` | enum : `ORIGINAL`, `AMENDMENT`, `EXTENSION` |  |  |
| `amendmentCode` | texte |  |  |
| `description` | texte |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `previousEndDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `at` | date-heure ISO 8601 UTC |  |  |
| `documentCode` | texte |  |  |

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
