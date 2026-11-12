# ClientResourceTypeController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-client-resource-types"></a>
### GET /sni/api/v1/client/resource-types

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ResourceTypeResponse>`
- Modèles : [ResourceTypeResponse](#m-ResourceTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/resource/controller/ClientResourceTypeController.java`, méthode `list`

<a id="r-get-sni-api-v1-client-resource-types-search"></a>
### GET /sni/api/v1/client/resource-types/search

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ResourceTypeResponse>`
- Modèles : [ResourceTypeResponse](#m-ResourceTypeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/resource/controller/ClientResourceTypeController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceTypeResponse"></a>
#### ResourceTypeResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `active` | booléen |  |  |
| `bookableSlots` | entier |  |  |
