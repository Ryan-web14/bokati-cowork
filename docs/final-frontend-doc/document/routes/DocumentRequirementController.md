# DocumentRequirementController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-document-requirements"></a>
### GET /sni/api/v1/document-requirements

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `List<DocumentRequirementResponse>`
- Modèles : [DocumentRequirementResponse](#m-DocumentRequirementResponse), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentRequirementController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` |  |  |
| `active` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-document-requirements"></a>
### POST /sni/api/v1/document-requirements

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_REQUIREMENT_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `DocumentRequirementRequest`
- Réponse : `DocumentRequirementResponse`
- Modèles : [DocumentRequirementResponse](#m-DocumentRequirementResponse), [DocumentRequirementRequest](#m-DocumentRequirementRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentRequirementController.java`, méthode `create`

<a id="r-delete-sni-api-v1-document-requirements-id"></a>
### DELETE /sni/api/v1/document-requirements/{id}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_REQUIREMENT_DELETE`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentRequirementController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-document-requirements-id"></a>
### GET /sni/api/v1/document-requirements/{id}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `DocumentRequirementResponse`
- Modèles : [DocumentRequirementResponse](#m-DocumentRequirementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentRequirementController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-document-requirements-id"></a>
### PATCH /sni/api/v1/document-requirements/{id}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_REQUIREMENT_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `DocumentRequirementRequest`
- Réponse : `DocumentRequirementResponse`
- Modèles : [DocumentRequirementResponse](#m-DocumentRequirementResponse), [DocumentRequirementRequest](#m-DocumentRequirementRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentRequirementController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-document-requirements-id-activate"></a>
### PATCH /sni/api/v1/document-requirements/{id}/activate

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_REQUIREMENT_ACTIVATE`, voir le README racine)
- Réponse : `DocumentRequirementResponse`
- Modèles : [DocumentRequirementResponse](#m-DocumentRequirementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentRequirementController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-document-requirements-id-deactivate"></a>
### PATCH /sni/api/v1/document-requirements/{id}/deactivate

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `DOCUMENT_REQUIREMENT_DEACTIVATE`, voir le README racine)
- Réponse : `DocumentRequirementResponse`
- Modèles : [DocumentRequirementResponse](#m-DocumentRequirementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentRequirementController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DocumentRequirementResponse"></a>
#### DocumentRequirementResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |  |  |
| `documentTypeCode` | texte |  |  |
| `documentTypeName` | texte |  |  |
| `customerType` | texte |  |  |
| `businessLegalForm` | texte |  |  |
| `required` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-DocumentOwnerType"></a>
#### DocumentOwnerType

Énumération : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`

<a id="m-DocumentRequirementRequest"></a>
#### DocumentRequirementRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` | oui |  |
| `documentTypeCode` | texte | oui | longueur min `1` |
| `documentTypeName` | texte |  |  |
| `customerType` | texte |  |  |
| `businessLegalForm` | texte |  |  |
| `required` | booléen |  |  |
| `active` | booléen |  |  |
