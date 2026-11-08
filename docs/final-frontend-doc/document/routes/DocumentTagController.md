# DocumentTagController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 11 route(s).

## Routes

<a id="r-get-sni-api-v1-document-tags"></a>
### GET /sni/api/v1/document-tags

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<DocumentTagResponse>`
- Modèles : [DocumentTagResponse](#m-DocumentTagResponse), [DocumentSpace](#m-DocumentSpace)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `listTags`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `space` | requête | `DocumentSpace` |  |  |

<a id="r-post-sni-api-v1-document-tags"></a>
### POST /sni/api/v1/document-tags

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `DocumentTagRequest`
- Réponse : `DocumentTagResponse`
- Modèles : [DocumentTagResponse](#m-DocumentTagResponse), [DocumentTagRequest](#m-DocumentTagRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `createTag`

<a id="r-delete-sni-api-v1-document-tags-code"></a>
### DELETE /sni/api/v1/document-tags/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `deleteTag`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-tags-code"></a>
### GET /sni/api/v1/document-tags/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `DocumentTagResponse`
- Modèles : [DocumentTagResponse](#m-DocumentTagResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `getTag`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-document-tags-code"></a>
### PUT /sni/api/v1/document-tags/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `DocumentTagUpdateRequest`
- Réponse : `DocumentTagResponse`
- Modèles : [DocumentTagResponse](#m-DocumentTagResponse), [DocumentTagUpdateRequest](#m-DocumentTagUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `updateTag`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-code-metadata"></a>
### GET /sni/api/v1/documents/{code}/metadata

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `Map<String, String>`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `getMetadata`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-documents-code-metadata"></a>
### PATCH /sni/api/v1/documents/{code}/metadata

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Corps JSON (obligatoire) : `Map<String, String>`
- Réponse : `Map<String, String>`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `setMetadata`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-documents-code-metadata-key"></a>
### DELETE /sni/api/v1/documents/{code}/metadata/{key}

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `deleteMetadataKey`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `key` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-code-tags"></a>
### GET /sni/api/v1/documents/{code}/tags

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `List<DocumentTagResponse>`
- Modèles : [DocumentTagResponse](#m-DocumentTagResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `getDocumentTags`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-code-tags"></a>
### POST /sni/api/v1/documents/{code}/tags

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Corps JSON (obligatoire) : `DocumentTagAssignRequest`
- Réponse : `List<DocumentTagResponse>`
- Modèles : [DocumentTagResponse](#m-DocumentTagResponse), [DocumentTagAssignRequest](#m-DocumentTagAssignRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `assignTags`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-documents-code-tags-tagcode"></a>
### DELETE /sni/api/v1/documents/{code}/tags/{tagCode}

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentTagController.java`, méthode `removeTag`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `tagCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-DocumentSpace"></a>
#### DocumentSpace

Énumération : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC`

<a id="m-DocumentTagRequest"></a>
#### DocumentTagRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `80`, motif `^[a-z0-9-]+$` |
| `label` | texte | oui | longueur min `0`, longueur max `150` |
| `color` | texte |  | motif `^#[0-9A-Fa-f]{6}$` |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |

<a id="m-DocumentTagUpdateRequest"></a>
#### DocumentTagUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  | longueur min `0`, longueur max `150` |
| `color` | texte |  | motif `^#[0-9A-Fa-f]{6}$` |

<a id="m-DocumentTagAssignRequest"></a>
#### DocumentTagAssignRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `tagCodes` | texte[] | oui | éléments min `1` |
