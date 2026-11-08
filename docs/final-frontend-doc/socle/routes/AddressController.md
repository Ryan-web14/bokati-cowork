# AddressController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 4 route(s).

## Routes

<a id="r-post-sni-api-v1-addresses"></a>
### POST /sni/api/v1/addresses

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `AddressRequest`
- Réponse : `AddressResponse`
- Modèles : [AddressResponse](#m-AddressResponse), [AddressRequest](#m-AddressRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/AddressController.java`, méthode `create`

<a id="r-delete-sni-api-v1-addresses-id"></a>
### DELETE /sni/api/v1/addresses/{id}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/AddressController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `long` | oui |  |

<a id="r-get-sni-api-v1-addresses-id"></a>
### GET /sni/api/v1/addresses/{id}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `AddressResponse`
- Modèles : [AddressResponse](#m-AddressResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/AddressController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `long` | oui |  |

<a id="r-put-sni-api-v1-addresses-id"></a>
### PUT /sni/api/v1/addresses/{id}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `AddressRequest`
- Réponse : `AddressResponse`
- Modèles : [AddressResponse](#m-AddressResponse), [AddressRequest](#m-AddressRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/baseClasses/controller/AddressController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AddressResponse"></a>
#### AddressResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `country` | texte |  |  |
| `fullAddress` | texte |  |  |

<a id="m-AddressRequest"></a>
#### AddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |
