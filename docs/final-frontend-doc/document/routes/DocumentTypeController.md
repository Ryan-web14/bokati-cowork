# DocumentTypeController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-document-types"></a>
### GET /sni/api/v1/document-types

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `List<DocumentTypeResponse>`
- Modèles : [DocumentTypeResponse](#m-DocumentTypeResponse), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTypeController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `active` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-document-types"></a>
### POST /sni/api/v1/document-types

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_TYPE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `DocumentTypeRequest`
- Réponse : `DocumentTypeResponse`
- Modèles : [DocumentTypeResponse](#m-DocumentTypeResponse), [DocumentTypeRequest](#m-DocumentTypeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTypeController.java`, méthode `create`

<a id="r-delete-sni-api-v1-document-types-code"></a>
### DELETE /sni/api/v1/document-types/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_TYPE_DELETE`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTypeController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-types-code"></a>
### GET /sni/api/v1/document-types/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `DocumentTypeResponse`
- Modèles : [DocumentTypeResponse](#m-DocumentTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTypeController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-document-types-code"></a>
### PATCH /sni/api/v1/document-types/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_TYPE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `DocumentTypeRequest`
- Réponse : `DocumentTypeResponse`
- Modèles : [DocumentTypeResponse](#m-DocumentTypeResponse), [DocumentTypeRequest](#m-DocumentTypeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTypeController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-document-types-code-activate"></a>
### PATCH /sni/api/v1/document-types/{code}/activate

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_TYPE_ACTIVATE`, voir le README racine)
- Réponse : `DocumentTypeResponse`
- Modèles : [DocumentTypeResponse](#m-DocumentTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTypeController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-document-types-code-deactivate"></a>
### PATCH /sni/api/v1/document-types/{code}/deactivate

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_TYPE_DEACTIVATE`, voir le README racine)
- Réponse : `DocumentTypeResponse`
- Modèles : [DocumentTypeResponse](#m-DocumentTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTypeController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-types-code-upload-config"></a>
### GET /sni/api/v1/document-types/{code}/upload-config

- Accès : tout compte connecté
- Réponse : `DocumentUploadConfigResponse`
- Modèles : [DocumentUploadConfigResponse](#m-DocumentUploadConfigResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTypeController.java`, méthode `uploadConfig`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DocumentTypeResponse"></a>
#### DocumentTypeResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `category` | enum : `KYC`, `LEGAL`, `SYSTEM`, `FINANCIAL`, `ASSET`, `OTHER` |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `description` | texte |  |  |
| `helpText` | texte |  |  |
| `documentDetails` | texte |  |  |
| `required` | booléen |  |  |
| `requiresExpiryDate` | booléen |  |  |
| `requiresIssueDate` | booléen |  |  |
| `requiresDocumentNumber` | booléen |  |  |
| `requiresReview` | booléen |  |  |
| `autoApprove` | booléen |  |  |
| `autoApproveAfterDays` | entier |  |  |
| `requiresSignature` | booléen |  |  |
| `multipleAllowed` | booléen |  |  |
| `requiresBackSide` | booléen |  |  |
| `allowedMimeTypes` | texte |  |  |
| `maxFileSizeBytes` | entier int64 ⚠ |  |  |
| `active` | booléen |  |  |

<a id="m-DocumentOwnerType"></a>
#### DocumentOwnerType

Énumération : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`

<a id="m-DocumentTypeRequest"></a>
#### DocumentTypeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `1` |
| `name` | texte | oui | longueur min `1` |
| `category` | enum : `KYC`, `LEGAL`, `SYSTEM`, `FINANCIAL`, `ASSET`, `OTHER` | oui |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `description` | texte |  |  |
| `helpText` | texte |  |  |
| `documentDetails` | texte |  |  |
| `required` | booléen |  |  |
| `requiresExpiryDate` | booléen |  |  |
| `requiresIssueDate` | booléen |  |  |
| `requiresDocumentNumber` | booléen |  |  |
| `requiresReview` | booléen |  |  |
| `autoApprove` | booléen |  |  |
| `autoApproveAfterDays` | entier |  |  |
| `requiresSignature` | booléen |  |  |
| `multipleAllowed` | booléen |  |  |
| `requiresBackSide` | booléen |  |  |
| `allowedMimeTypes` | texte |  |  |
| `maxFileSizeBytes` | entier int64 ⚠ |  |  |
| `active` | booléen |  |  |

<a id="m-DocumentUploadConfigResponse"></a>
#### DocumentUploadConfigResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentTypeCode` | texte |  |  |
| `documentTypeName` | texte |  |  |
| `helpText` | texte |  |  |
| `requiresDocumentNumber` | booléen |  |  |
| `requiresIssueDate` | booléen |  |  |
| `requiresExpiryDate` | booléen |  |  |
| `requiresBackSide` | booléen |  |  |
| `allowedMimeTypes` | texte[] |  |  |
| `maxFileSizeBytes` | entier int64 ⚠ |  |  |
| `category` | texte |  |  |
