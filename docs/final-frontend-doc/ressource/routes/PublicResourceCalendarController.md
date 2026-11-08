# PublicResourceCalendarController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-public-resources-resourcecode-calendar"></a>
### GET /sni/api/v1/public/resources/{resourceCode}/calendar

- Accès : public, sans jeton
- Réponse : `PublicResourceCalendarResponse`
- Modèles : [PublicResourceCalendarResponse](#m-PublicResourceCalendarResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/PublicResourceCalendarController.java`, méthode `calendar`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | chemin | `String` | oui |  |
| `fromDate` | requête | `LocalDate` |  |  |
| `toDate` | requête | `LocalDate` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PublicResourceCalendarResponse"></a>
#### PublicResourceCalendarResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `days` | [PublicResourceCalendarResponse.Day](#m-PublicResourceCalendarResponse-Day)[] |  |  |

<a id="m-PublicResourceCalendarResponse-Day"></a>
#### PublicResourceCalendarResponse.Day

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `totalSlots` | entier |  |  |
| `availableSlots` | entier |  |  |
| `totalCapacity` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `status` | texte |  |  |
| `windows` | [PublicResourceCalendarResponse.Window](#m-PublicResourceCalendarResponse-Window)[] |  |  |

<a id="m-PublicResourceCalendarResponse-Window"></a>
#### PublicResourceCalendarResponse.Window

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `slotDurationMinutes` | entier |  |  |
| `totalCapacity` | entier |  |  |
| `remainingCapacity` | entier |  |  |
| `available` | booléen |  |  |
| `active` | booléen |  |  |
| `subscribersOnly` | booléen |  |  |
