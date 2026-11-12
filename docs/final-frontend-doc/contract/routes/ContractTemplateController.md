# ContractTemplateController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [contract](../README.md) · 13 route(s).

## Routes

<a id="r-get-sni-api-v1-contract-templates"></a>
### GET /sni/api/v1/contract-templates

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<ContractTemplateResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ContractTemplateResponse](#m-ContractTemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-contract-templates"></a>
### POST /sni/api/v1/contract-templates

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_TEMPLATE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateContractTemplateRequest`
- Réponse : `ContractTemplateResponse`
- Modèles : [ContractTemplateResponse](#m-ContractTemplateResponse), [CreateContractTemplateRequest](#m-CreateContractTemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `create`

<a id="r-get-sni-api-v1-contract-templates-category-category"></a>
### GET /sni/api/v1/contract-templates/category/{category}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<ContractTemplateResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ContractTemplateResponse](#m-ContractTemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `listByCategory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `category` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-contract-templates-search"></a>
### GET /sni/api/v1/contract-templates/search

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<ContractTemplateResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ContractTemplateResponse](#m-ContractTemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-delete-sni-api-v1-contract-templates-code"></a>
### DELETE /sni/api/v1/contract-templates/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-contract-templates-code"></a>
### GET /sni/api/v1/contract-templates/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `ContractTemplateResponse`
- Modèles : [ContractTemplateResponse](#m-ContractTemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-contract-templates-code"></a>
### PUT /sni/api/v1/contract-templates/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- En-tête `Idempotency-Key` : conseillé (opération `CONTRACT_TEMPLATE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateContractTemplateRequest`
- Réponse : `ContractTemplateResponse`
- Modèles : [ContractTemplateResponse](#m-ContractTemplateResponse), [UpdateContractTemplateRequest](#m-UpdateContractTemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-contract-templates-code-duplicate"></a>
### POST /sni/api/v1/contract-templates/{code}/duplicate

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `ContractTemplateResponse`
- Modèles : [ContractTemplateResponse](#m-ContractTemplateResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `duplicate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-contract-templates-code-preview"></a>
### POST /sni/api/v1/contract-templates/{code}/preview

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (facultatif) : `Map<String, String>`
- Réponse : `ContractPreviewResponse`
- Modèles : [ContractPreviewResponse](#m-ContractPreviewResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-contract-templates-code-sections"></a>
### GET /sni/api/v1/contract-templates/{code}/sections

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<ContractTemplateSectionResponse>`
- Modèles : [ContractTemplateSectionResponse](#m-ContractTemplateSectionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `listSections`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-contract-templates-code-sections"></a>
### POST /sni/api/v1/contract-templates/{code}/sections

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ContractDraftSectionRequest`
- Réponse : `ContractTemplateSectionResponse`
- Modèles : [ContractTemplateSectionResponse](#m-ContractTemplateSectionResponse), [ContractDraftSectionRequest](#m-ContractDraftSectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `addSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-contract-templates-code-sections-sectionid"></a>
### DELETE /sni/api/v1/contract-templates/{code}/sections/{sectionId}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `removeSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `sectionId` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-contract-templates-code-sections-sectionid"></a>
### PUT /sni/api/v1/contract-templates/{code}/sections/{sectionId}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ContractDraftSectionRequest`
- Réponse : `ContractTemplateSectionResponse`
- Modèles : [ContractTemplateSectionResponse](#m-ContractTemplateSectionResponse), [ContractDraftSectionRequest](#m-ContractDraftSectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractTemplateController.java`, méthode `updateSection`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `sectionId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-CreateContractTemplateRequest"></a>
#### CreateContractTemplateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `language` | texte |  |  |
| `category` | texte |  |  |
| `htmlContent` | texte |  |  |
| `cssContent` | texte |  |  |
| `headerHtml` | texte |  |  |
| `footerHtml` | texte |  |  |
| `variableDefinitions` | [TemplateVariableDefinition](#m-TemplateVariableDefinition)[] |  |  |

<a id="m-UpdateContractTemplateRequest"></a>
#### UpdateContractTemplateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `language` | texte |  |  |
| `category` | texte |  |  |
| `htmlContent` | texte |  |  |
| `cssContent` | texte |  |  |
| `headerHtml` | texte |  |  |
| `footerHtml` | texte |  |  |
| `variableDefinitions` | [TemplateVariableDefinition](#m-TemplateVariableDefinition)[] |  |  |
| `active` | booléen |  |  |

<a id="m-ContractPreviewResponse"></a>
#### ContractPreviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte |  |  |
| `html` | texte |  |  |

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

<a id="m-ContractDraftSectionRequest"></a>
#### ContractDraftSectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `content` | texte | oui | longueur min `1` |
| `sectionType` | enum : `PREAMBLE`, `ARTICLE`, `CLAUSE`, `SIGNATURE_BLOCK`, `ANNEXE` | oui |  |
| `sectionOrder` | entier |  |  |
| `active` | booléen |  |  |

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
