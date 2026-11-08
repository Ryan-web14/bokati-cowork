# ResourceOpeningHoursController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 3 route(s).

## Routes

<a id="r-delete-sni-api-v1-resources-resourcecode-opening-hours"></a>
### DELETE /sni/api/v1/resources/{resourceCode}/opening-hours

- Accès : back-office : rôle d'administration et droit `RESOURCE:DELETE` ; contrôle supplémentaire sur la méthode : `hasRole('SUPER_ADMIN') or hasRole('ADMIN')`
- Réponse : `ResourceOpeningHoursService.View`
- Modèles : [ResourceOpeningHoursService.View](#m-ResourceOpeningHoursService-View)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOpeningHoursController.java`, méthode `clear`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resources-resourcecode-opening-hours"></a>
### GET /sni/api/v1/resources/{resourceCode}/opening-hours

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ` ; contrôle supplémentaire sur la méthode : `hasRole('SUPER_ADMIN') or hasRole('ADMIN')`
- Réponse : `ResourceOpeningHoursService.View`
- Modèles : [ResourceOpeningHoursService.View](#m-ResourceOpeningHoursService-View)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOpeningHoursController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-resources-resourcecode-opening-hours"></a>
### PUT /sni/api/v1/resources/{resourceCode}/opening-hours

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE` ; contrôle supplémentaire sur la méthode : `hasRole('SUPER_ADMIN') or hasRole('ADMIN')`
- Corps JSON (obligatoire) : `List<ResourceOpeningHoursService.Range>`
- Réponse : `ResourceOpeningHoursService.View`
- Modèles : [ResourceOpeningHoursService.View](#m-ResourceOpeningHoursService-View), [ResourceOpeningHoursService.Range](#m-ResourceOpeningHoursService-Range)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceOpeningHoursController.java`, méthode `replace`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceOpeningHoursService-View"></a>
#### ResourceOpeningHoursService.View

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `custom` | booléen |  |  |
| `hours` | [ResourceOpeningHoursService.Range](#m-ResourceOpeningHoursService-Range)[] |  |  |

<a id="m-ResourceOpeningHoursService-Range"></a>
#### ResourceOpeningHoursService.Range

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `dayOfWeek` | enum : `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY` |  |  |
| `open` | [LocalTime](#m-LocalTime) |  |  |
| `close` | [LocalTime](#m-LocalTime) |  |  |

<a id="m-LocalTime"></a>
#### LocalTime

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hour` | entier |  |  |
| `minute` | entier |  |  |
| `second` | entier |  |  |
| `nano` | entier |  |  |
