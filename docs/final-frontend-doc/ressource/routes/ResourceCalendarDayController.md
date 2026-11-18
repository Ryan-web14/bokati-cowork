# ResourceCalendarDayController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-calendar-days"></a>
### GET /sni/api/v1/resource-calendar-days

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceCalendarDayResponse>`
- Modèles : [ResourceCalendarDayResponse](#m-ResourceCalendarDayResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceCalendarDayController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `resourceCode` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-resource-calendar-days"></a>
### POST /sni/api/v1/resource-calendar-days

- Accès : back-office : rôle d'administration et droit `RESOURCE:CREATE`
- Corps JSON (obligatoire) : `ResourceCalendarDayRequest`
- Réponse : `ResourceCalendarDayResponse`
- Modèles : [ResourceCalendarDayResponse](#m-ResourceCalendarDayResponse), [ResourceCalendarDayRequest](#m-ResourceCalendarDayRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceCalendarDayController.java`, méthode `create`

<a id="r-delete-sni-api-v1-resource-calendar-days-id"></a>
### DELETE /sni/api/v1/resource-calendar-days/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceCalendarDayController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceCalendarDayResponse"></a>
#### ResourceCalendarDayResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `resourceCode` | texte |  |  |
| `kind` | texte |  |  |
| `label` | texte |  |  |
| `openingHours` | texte |  |  |
| `national` | booléen |  |  |

<a id="m-ResourceCalendarDayRequest"></a>
#### ResourceCalendarDayRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `resourceCode` | texte |  |  |
| `kind` | texte |  |  |
| `label` | texte |  |  |
| `openingHours` | texte |  |  |
