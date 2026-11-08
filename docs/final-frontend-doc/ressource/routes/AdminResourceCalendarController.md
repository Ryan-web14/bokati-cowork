# AdminResourceCalendarController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-resources-occupied-slots"></a>
### GET /sni/api/v1/resources/occupied-slots

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `List<ResourceOccupiedSlotResponse>`
- Modèles : [ResourceOccupiedSlotResponse](#m-ResourceOccupiedSlotResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/AdminResourceCalendarController.java`, méthode `occupiedSlots`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | requête | `String` | oui |  |
| `from` | requête | `LocalDateTime` |  |  |
| `to` | requête | `LocalDateTime` |  |  |

<a id="r-get-sni-api-v1-resources-resourcecode-calendar-admin"></a>
### GET /sni/api/v1/resources/{resourceCode}/calendar/admin

- Accès : back-office : rôle d'administration et droit `RESOURCE:READ`
- Réponse : `AdminResourceCalendarResponse`
- Modèles : [AdminResourceCalendarResponse](#m-AdminResourceCalendarResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/AdminResourceCalendarController.java`, méthode `calendar`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | chemin | `String` | oui |  |
| `fromDate` | requête | `LocalDate` |  |  |
| `toDate` | requête | `LocalDate` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceOccupiedSlotResponse"></a>
#### ResourceOccupiedSlotResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `slotDurationMinutes` | entier |  |  |
| `totalCapacity` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `usedCapacity` | entier |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `occupancyLevel` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-AdminResourceCalendarResponse"></a>
#### AdminResourceCalendarResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `days` | [AdminResourceCalendarResponse.Day](#m-AdminResourceCalendarResponse-Day)[] |  |  |

<a id="m-AdminResourceCalendarResponse-Day"></a>
#### AdminResourceCalendarResponse.Day

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `totalSlots` | entier |  |  |
| `availableSlots` | entier |  |  |
| `occupiedSlots` | entier |  |  |
| `totalCapacity` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `usedCapacity` | entier |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `occupancyLevel` | texte |  |  |
| `status` | texte |  |  |
| `windows` | [AdminResourceCalendarResponse.Window](#m-AdminResourceCalendarResponse-Window)[] |  |  |

<a id="m-AdminResourceCalendarResponse-Window"></a>
#### AdminResourceCalendarResponse.Window

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `slotDurationMinutes` | entier |  |  |
| `totalCapacity` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `usedCapacity` | entier |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `occupancyLevel` | texte |  |  |
| `available` | booléen |  |  |
| `active` | booléen |  |  |
| `subscribersOnly` | booléen |  |  |
