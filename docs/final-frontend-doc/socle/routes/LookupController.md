# LookupController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-lookup-users"></a>
### GET /sni/api/v1/lookup/users

- Accès : back-office : rôle d'administration et droit `STAFF`
- Réponse : `PaginatedResponse<LookupItem>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [LookupItem](#m-LookupItem)
- Code : `src/main/java/com/sni/bokaticowork/core/reference/lookup/LookupController.java`, méthode `users`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |
| `includeInactive` | requête | `boolean` |  | false |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-LookupItem"></a>
#### LookupItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `hint` | texte |  |  |
| `active` | booléen |  |  |
