# ResourcePhotoController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-resources-resourcecode-photos"></a>
### GET /sni/api/v1/resources/{resourceCode}/photos

- Accès : back-office : rôle d'administration et droit `RESOURCE:GALLERY`
- Réponse : `List<ResourcePhotoResponse>`
- Modèles : [ResourcePhotoResponse](#m-ResourcePhotoResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePhotoController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-resources-resourcecode-photos"></a>
### POST /sni/api/v1/resources/{resourceCode}/photos

- Accès : back-office : rôle d'administration et droit `RESOURCE:GALLERY` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('RESOURCE:GALLERY','RESOURCE_GALLERY')`
- Corps JSON (obligatoire) : `ResourcePhotoRequest`
- Réponse : `ResourcePhotoResponse`
- Modèles : [ResourcePhotoResponse](#m-ResourcePhotoResponse), [ResourcePhotoRequest](#m-ResourcePhotoRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePhotoController.java`, méthode `add`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-resources-resourcecode-photos-id"></a>
### DELETE /sni/api/v1/resources/{resourceCode}/photos/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:GALLERY` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('RESOURCE:GALLERY','RESOURCE_GALLERY')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePhotoController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-resources-resourcecode-photos-id"></a>
### PATCH /sni/api/v1/resources/{resourceCode}/photos/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:GALLERY` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('RESOURCE:GALLERY','RESOURCE_GALLERY')`
- Corps JSON (obligatoire) : `ResourcePhotoRequest`
- Réponse : `ResourcePhotoResponse`
- Modèles : [ResourcePhotoResponse](#m-ResourcePhotoResponse), [ResourcePhotoRequest](#m-ResourcePhotoRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePhotoController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourcePhotoResponse"></a>
#### ResourcePhotoResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `resourceCode` | texte |  |  |
| `documentCode` | texte |  |  |
| `caption` | texte |  |  |
| `cover` | booléen |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-ResourcePhotoRequest"></a>
#### ResourcePhotoRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte | oui | longueur min `1` |
| `caption` | texte |  |  |
| `cover` | booléen |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |
