# ClientKycController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 30 route(s).

## Routes

<a id="r-get-sni-api-v1-client-documents-kyc"></a>
### GET /sni/api/v1/client/documents/kyc

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycStatusResponse`
- Modèles : [ClientKycStatusResponse](#m-ClientKycStatusResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getMyCase`

<a id="r-get-sni-api-v1-client-documents-kyc-completion"></a>
### GET /sni/api/v1/client/documents/kyc/completion

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycCompletionResponse`
- Modèles : [ClientKycCompletionResponse](#m-ClientKycCompletionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getCompletion`

<a id="r-get-sni-api-v1-client-documents-kyc-documents"></a>
### GET /sni/api/v1/client/documents/kyc/documents

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientKycDocumentResponse>`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `listDocuments`

<a id="r-post-sni-api-v1-client-documents-kyc-documents"></a>
### POST /sni/api/v1/client/documents/kyc/documents

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `multipart/form-data`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `uploadDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `documentType` | requête | `String` | oui |  |
| `documentNumber` | requête | `String` |  |  |
| `issueDate` | requête | `LocalDate` |  |  |
| `expiryDate` | requête | `LocalDate` |  |  |
| `side` | requête | `String` |  | FRONT |

<a id="r-post-sni-api-v1-client-documents-kyc-documents"></a>
### POST /sni/api/v1/client/documents/kyc/documents

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `application/json`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `uploadDocumentJson`

<a id="r-post-sni-api-v1-client-documents-kyc-documents-upload"></a>
### POST /sni/api/v1/client/documents/kyc/documents/upload

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `multipart/form-data`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `uploadDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `documentType` | requête | `String` | oui |  |
| `documentNumber` | requête | `String` |  |  |
| `issueDate` | requête | `LocalDate` |  |  |
| `expiryDate` | requête | `LocalDate` |  |  |
| `side` | requête | `String` |  | FRONT |

<a id="r-post-sni-api-v1-client-documents-kyc-documents-upload"></a>
### POST /sni/api/v1/client/documents/kyc/documents/upload

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `application/json`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `uploadDocumentJson`

<a id="r-delete-sni-api-v1-client-documents-kyc-documents-id"></a>
### DELETE /sni/api/v1/client/documents/kyc/documents/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `deleteDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-documents-kyc-documents-id"></a>
### GET /sni/api/v1/client/documents/kyc/documents/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-client-documents-kyc-documents-id"></a>
### PUT /sni/api/v1/client/documents/kyc/documents/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `multipart/form-data`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `resubmitDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-put-sni-api-v1-client-documents-kyc-documents-id"></a>
### PUT /sni/api/v1/client/documents/kyc/documents/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `application/json`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `resubmitDocumentJson`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-documents-kyc-documents-id-download"></a>
### GET /sni/api/v1/client/documents/kyc/documents/{id}/download

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `downloadDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-documents-kyc-my-case"></a>
### GET /sni/api/v1/client/documents/kyc/my-case

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycStatusResponse`
- Modèles : [ClientKycStatusResponse](#m-ClientKycStatusResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getMyCase`

<a id="r-get-sni-api-v1-client-documents-kyc-requirements"></a>
### GET /sni/api/v1/client/documents/kyc/requirements

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientKycRequirementResponse>`
- Modèles : [ClientKycRequirementResponse](#m-ClientKycRequirementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getRequirements`

<a id="r-post-sni-api-v1-client-documents-kyc-submit"></a>
### POST /sni/api/v1/client/documents/kyc/submit

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycStatusResponse`
- Modèles : [ClientKycStatusResponse](#m-ClientKycStatusResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `submitCase`

<a id="r-get-sni-api-v1-portal-kyc"></a>
### GET /sni/api/v1/portal/kyc

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycStatusResponse`
- Modèles : [ClientKycStatusResponse](#m-ClientKycStatusResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getMyCase`

<a id="r-get-sni-api-v1-portal-kyc-completion"></a>
### GET /sni/api/v1/portal/kyc/completion

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycCompletionResponse`
- Modèles : [ClientKycCompletionResponse](#m-ClientKycCompletionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getCompletion`

<a id="r-get-sni-api-v1-portal-kyc-documents"></a>
### GET /sni/api/v1/portal/kyc/documents

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientKycDocumentResponse>`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `listDocuments`

<a id="r-post-sni-api-v1-portal-kyc-documents"></a>
### POST /sni/api/v1/portal/kyc/documents

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `multipart/form-data`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `uploadDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `documentType` | requête | `String` | oui |  |
| `documentNumber` | requête | `String` |  |  |
| `issueDate` | requête | `LocalDate` |  |  |
| `expiryDate` | requête | `LocalDate` |  |  |
| `side` | requête | `String` |  | FRONT |

<a id="r-post-sni-api-v1-portal-kyc-documents"></a>
### POST /sni/api/v1/portal/kyc/documents

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `application/json`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `uploadDocumentJson`

<a id="r-post-sni-api-v1-portal-kyc-documents-upload"></a>
### POST /sni/api/v1/portal/kyc/documents/upload

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `multipart/form-data`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `uploadDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `documentType` | requête | `String` | oui |  |
| `documentNumber` | requête | `String` |  |  |
| `issueDate` | requête | `LocalDate` |  |  |
| `expiryDate` | requête | `LocalDate` |  |  |
| `side` | requête | `String` |  | FRONT |

<a id="r-post-sni-api-v1-portal-kyc-documents-upload"></a>
### POST /sni/api/v1/portal/kyc/documents/upload

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `application/json`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `uploadDocumentJson`

<a id="r-delete-sni-api-v1-portal-kyc-documents-id"></a>
### DELETE /sni/api/v1/portal/kyc/documents/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `deleteDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-portal-kyc-documents-id"></a>
### GET /sni/api/v1/portal/kyc/documents/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-portal-kyc-documents-id"></a>
### PUT /sni/api/v1/portal/kyc/documents/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `multipart/form-data`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `resubmitDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-put-sni-api-v1-portal-kyc-documents-id"></a>
### PUT /sni/api/v1/portal/kyc/documents/{id}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Envoi : `application/json`
- Réponse : `ClientKycDocumentResponse`
- Modèles : [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `resubmitDocumentJson`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-portal-kyc-documents-id-download"></a>
### GET /sni/api/v1/portal/kyc/documents/{id}/download

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `downloadDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-portal-kyc-my-case"></a>
### GET /sni/api/v1/portal/kyc/my-case

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycStatusResponse`
- Modèles : [ClientKycStatusResponse](#m-ClientKycStatusResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getMyCase`

<a id="r-get-sni-api-v1-portal-kyc-requirements"></a>
### GET /sni/api/v1/portal/kyc/requirements

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientKycRequirementResponse>`
- Modèles : [ClientKycRequirementResponse](#m-ClientKycRequirementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `getRequirements`

<a id="r-post-sni-api-v1-portal-kyc-submit"></a>
### POST /sni/api/v1/portal/kyc/submit

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientKycStatusResponse`
- Modèles : [ClientKycStatusResponse](#m-ClientKycStatusResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/kyc/controller/ClientKycController.java`, méthode `submitCase`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClientKycStatusResponse"></a>
#### ClientKycStatusResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `caseCode` | texte |  |  |
| `status` | texte |  |  |
| `riskLevel` | texte |  |  |
| `kycLevel` | entier |  |  |
| `completionPercent` | entier |  |  |
| `complete` | booléen |  |  |
| `approved` | booléen |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `submittedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `decisionComment` | texte |  |  |
| `missingDocumentTypeCodes` | texte[] |  |  |
| `requirements` | [ClientKycRequirementResponse](#m-ClientKycRequirementResponse)[] |  |  |
| `documents` | [ClientKycDocumentResponse](#m-ClientKycDocumentResponse)[] |  |  |

<a id="m-ClientKycCompletionResponse"></a>
#### ClientKycCompletionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `caseCode` | texte |  |  |
| `caseStatus` | texte |  |  |
| `completionPercent` | entier |  |  |
| `totalRequired` | entier |  |  |
| `totalSubmitted` | entier |  |  |
| `totalVerified` | entier |  |  |
| `missingDocumentTypeCodes` | texte[] |  |  |
| `requirements` | [ClientKycRequirementResponse](#m-ClientKycRequirementResponse)[] |  |  |

<a id="m-ClientKycDocumentResponse"></a>
#### ClientKycDocumentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `documentCode` | texte |  |  |
| `documentType` | texte |  |  |
| `documentTypeName` | texte |  |  |
| `documentNumber` | texte |  |  |
| `fileName` | texte |  |  |
| `fileSize` | entier int64 ⚠ |  |  |
| `mimeType` | texte |  |  |
| `status` | texte |  |  |
| `requiresBackSide` | booléen |  |  |
| `backDocumentCode` | texte |  |  |
| `backFileName` | texte |  |  |
| `backFileSize` | entier int64 ⚠ |  |  |
| `backMimeType` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `uploadedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ClientKycRequirementResponse"></a>
#### ClientKycRequirementResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentTypeCode` | texte |  |  |
| `documentTypeName` | texte |  |  |
| `required` | booléen |  |  |
| `requiresBackSide` | booléen |  |  |
| `status` | texte |  |  |
| `documentCode` | texte |  |  |
| `backDocumentCode` | texte |  |  |
