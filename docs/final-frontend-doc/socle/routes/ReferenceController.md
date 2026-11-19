# ReferenceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-reference-lists"></a>
### GET /sni/api/v1/reference-lists

- Accès : back-office : rôle d'administration et droit `STAFF`
- Réponse : `List<ReferenceDtos.ListSummary>`
- Modèles : [ReferenceDtos.ListSummary](#m-ReferenceDtos-ListSummary)
- Code : `src/main/java/com/sni/bokaticowork/core/reference/controller/ReferenceController.java`, méthode `lists`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `module` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-reference-lists-listcode"></a>
### GET /sni/api/v1/reference-lists/{listCode}

- Accès : back-office : rôle d'administration et droit `STAFF`
- Réponse : `List<ReferenceDtos.Value>`
- Modèles : [ReferenceDtos.Value](#m-ReferenceDtos-Value)
- Code : `src/main/java/com/sni/bokaticowork/core/reference/controller/ReferenceController.java`, méthode `values`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `listCode` | chemin | `String` | oui |  |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-patch-sni-api-v1-reference-lists-listcode"></a>
### PATCH /sni/api/v1/reference-lists/{listCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:REFERENCE_LISTS`
- Corps JSON (obligatoire) : `ReferenceDtos.ListPatch`
- Réponse : `ReferenceDtos.ListSummary`
- Modèles : [ReferenceDtos.ListSummary](#m-ReferenceDtos-ListSummary), [ReferenceDtos.ListPatch](#m-ReferenceDtos-ListPatch)
- Code : `src/main/java/com/sni/bokaticowork/core/reference/controller/ReferenceController.java`, méthode `updateList`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `listCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-reference-lists-listcode"></a>
### PUT /sni/api/v1/reference-lists/{listCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:REFERENCE_LISTS`
- Corps JSON (obligatoire) : `List<ReferenceDtos.ValueRequest>`
- Réponse : `List<ReferenceDtos.Value>`
- Modèles : [ReferenceDtos.Value](#m-ReferenceDtos-Value), [ReferenceDtos.ValueRequest](#m-ReferenceDtos-ValueRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/reference/controller/ReferenceController.java`, méthode `replace`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `listCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-reference-lists-listcode-values"></a>
### POST /sni/api/v1/reference-lists/{listCode}/values

- Accès : back-office : rôle d'administration et droit `SYSTEM:REFERENCE_LISTS`
- Corps JSON (obligatoire) : `ReferenceDtos.ValueRequest`
- Réponse : `ReferenceDtos.Value`
- Modèles : [ReferenceDtos.Value](#m-ReferenceDtos-Value), [ReferenceDtos.ValueRequest](#m-ReferenceDtos-ValueRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/reference/controller/ReferenceController.java`, méthode `add`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `listCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-reference-lists-listcode-values-code"></a>
### PATCH /sni/api/v1/reference-lists/{listCode}/values/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:REFERENCE_LISTS`
- Corps JSON (obligatoire) : `ReferenceDtos.ValuePatch`
- Réponse : `ReferenceDtos.Value`
- Modèles : [ReferenceDtos.Value](#m-ReferenceDtos-Value), [ReferenceDtos.ValuePatch](#m-ReferenceDtos-ValuePatch)
- Code : `src/main/java/com/sni/bokaticowork/core/reference/controller/ReferenceController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `listCode` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-reference-enums"></a>
### GET /sni/api/v1/reference/enums

- Accès : back-office : rôle d'administration et droit `STAFF`
- Réponse : `List<ReferenceDtos.EnumGroup>`
- Modèles : [ReferenceDtos.EnumGroup](#m-ReferenceDtos-EnumGroup)
- Code : `src/main/java/com/sni/bokaticowork/core/reference/controller/ReferenceController.java`, méthode `enums`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `module` | requête | `String` |  |  |
| `group` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ReferenceDtos-ListSummary"></a>
#### ReferenceDtos.ListSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `module` | texte |  |  |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `strict` | booléen |  |  |
| `valueCount` | entier |  |  |
| `activeValueCount` | entier |  |  |
| `autoAddedCount` | entier |  |  |
| `usageCount` | entier int64 (texte au-delà de 2^53) |  |  |
| `otherUsageCount` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-ReferenceDtos-Value"></a>
#### ReferenceDtos.Value

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `listCode` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |
| `requiresText` | booléen |  |  |
| `systemValue` | booléen |  |  |
| `autoAdded` | booléen |  |  |
| `usageCount` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-ReferenceDtos-ListPatch"></a>
#### ReferenceDtos.ListPatch

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  | longueur min `0`, longueur max `160` |
| `description` | texte |  | longueur min `0`, longueur max `500` |
| `strict` | booléen |  |  |

<a id="m-ReferenceDtos-ValueRequest"></a>
#### ReferenceDtos.ValueRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `60` |
| `label` | texte | oui | longueur min `0`, longueur max `255` |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |
| `requiresText` | booléen |  |  |

<a id="m-ReferenceDtos-ValuePatch"></a>
#### ReferenceDtos.ValuePatch

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  | longueur min `0`, longueur max `255` |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |
| `requiresText` | booléen |  |  |

<a id="m-ReferenceDtos-EnumGroup"></a>
#### ReferenceDtos.EnumGroup

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `module` | texte |  |  |
| `group` | texte |  |  |
| `label` | texte |  |  |
| `values` | [ReferenceDtos.EnumValue](#m-ReferenceDtos-EnumValue)[] |  |  |

<a id="m-ReferenceDtos-EnumValue"></a>
#### ReferenceDtos.EnumValue

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `order` | entier |  |  |
| `selectable` | booléen |  |  |
