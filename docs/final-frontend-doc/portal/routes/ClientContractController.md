# ClientContractController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-client-documents-contracts"></a>
### GET /sni/api/v1/client/documents/contracts

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<ClientContractSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ClientContractSummaryResponse](#m-ClientContractSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/contract/controller/ClientContractController.java`, méthode `listContracts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-client-documents-contracts-contractcode"></a>
### GET /sni/api/v1/client/documents/contracts/{contractCode}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientContractResponse`
- Modèles : [ClientContractResponse](#m-ClientContractResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/contract/controller/ClientContractController.java`, méthode `getContract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-documents-contracts-contractcode-amendments-amendmentcode-pdf"></a>
### GET /sni/api/v1/client/documents/contracts/{contractCode}/amendments/{amendmentCode}/pdf

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/contract/controller/ClientContractController.java`, méthode `downloadAmendmentPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |
| `amendmentCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-documents-contracts-contractcode-pdf"></a>
### GET /sni/api/v1/client/documents/contracts/{contractCode}/pdf

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/contract/controller/ClientContractController.java`, méthode `downloadPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-documents-contracts-contractcode-sign"></a>
### POST /sni/api/v1/client/documents/contracts/{contractCode}/sign

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientContractSignRequest`
- Réponse : `ClientContractResponse`
- Modèles : [ClientContractResponse](#m-ClientContractResponse), [ClientContractSignRequest](#m-ClientContractSignRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/document/contract/controller/ClientContractController.java`, méthode `signContract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClientContractSummaryResponse"></a>
#### ClientContractSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `contractCode` | texte |  |  |
| `title` | texte |  |  |
| `status` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `hasPdf` | booléen |  |  |

<a id="m-ClientContractResponse"></a>
#### ClientContractResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `contractCode` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `status` | texte |  |  |
| `renewalType` | texte |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `activatedAt` | date-heure ISO 8601 UTC |  |  |
| `terminatedAt` | date-heure ISO 8601 UTC |  |  |
| `terminationReason` | texte |  |  |
| `draftDocumentCode` | texte |  |  |
| `signedDocumentCode` | texte |  |  |
| `canSign` | booléen |  |  |
| `canDownloadPdf` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `versions` | [ContractVersionHistory.Version](#m-ContractVersionHistory-Version)[] |  |  |

<a id="m-ClientContractSignRequest"></a>
#### ClientContractSignRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accepted` | booléen |  |  |
| `signatureData` | texte |  |  |

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
