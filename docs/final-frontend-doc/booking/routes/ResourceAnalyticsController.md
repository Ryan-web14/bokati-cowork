# ResourceAnalyticsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-analytics-bookings-number-margin"></a>
### GET /sni/api/v1/resource-analytics/bookings/{number}/margin

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `ResourceAnalytics.BookingMargin`
- Modèles : [ResourceAnalytics.BookingMargin](#m-ResourceAnalytics-BookingMargin)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/ResourceAnalyticsController.java`, méthode `bookingMargin`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resource-analytics-margins"></a>
### GET /sni/api/v1/resource-analytics/margins

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `ResourceAnalytics.MarginReport`
- Modèles : [ResourceAnalytics.MarginReport](#m-ResourceAnalytics-MarginReport)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/ResourceAnalyticsController.java`, méthode `margins`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `resourceCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-resource-analytics-occupancy"></a>
### GET /sni/api/v1/resource-analytics/occupancy

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `ResourceAnalytics.OccupancyReport`
- Modèles : [ResourceAnalytics.OccupancyReport](#m-ResourceAnalytics-OccupancyReport)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/ResourceAnalyticsController.java`, méthode `occupancy`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `groupBy` | requête | `String` |  |  |
| `siteCode` | requête | `String` |  |  |

<a id="r-delete-sni-api-v1-resource-costs-id"></a>
### DELETE /sni/api/v1/resource-costs/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/ResourceAnalyticsController.java`, méthode `deleteCost`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-get-sni-api-v1-resources-code-costs"></a>
### GET /sni/api/v1/resources/{code}/costs

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `List<ResourceAnalytics.CostResponse>`
- Modèles : [ResourceAnalytics.CostResponse](#m-ResourceAnalytics-CostResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/ResourceAnalyticsController.java`, méthode `costs`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-resources-code-costs"></a>
### POST /sni/api/v1/resources/{code}/costs

- Accès : back-office : rôle d'administration et droit `RESOURCE:UPDATE`
- Corps JSON (obligatoire) : `ResourceAnalytics.CostRequest`
- Réponse : `ResourceAnalytics.CostResponse`
- Modèles : [ResourceAnalytics.CostResponse](#m-ResourceAnalytics-CostResponse), [ResourceAnalytics.CostRequest](#m-ResourceAnalytics-CostRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/ResourceAnalyticsController.java`, méthode `addCost`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-resources-code-price-history"></a>
### GET /sni/api/v1/resources/{code}/price-history

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- Réponse : `List<ResourceAnalytics.AppliedPrice>`
- Modèles : [ResourceAnalytics.AppliedPrice](#m-ResourceAnalytics-AppliedPrice)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/ResourceAnalyticsController.java`, méthode `priceHistory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourceAnalytics-BookingMargin"></a>
#### ResourceAnalytics.BookingMargin

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `resourceCode` | texte |  |  |
| `revenue` | nombre décimal |  |  |
| `costs` | nombre décimal |  |  |
| `margin` | nombre décimal |  |  |
| `costsByKind` | objet { clé : nombre décimal } |  |  |

<a id="m-ResourceAnalytics-MarginReport"></a>
#### ResourceAnalytics.MarginReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `rows` | [ResourceAnalytics.MarginRow](#m-ResourceAnalytics-MarginRow)[] |  |  |
| `revenue` | nombre décimal |  |  |
| `costs` | nombre décimal |  |  |
| `margin` | nombre décimal |  |  |

<a id="m-ResourceAnalytics-OccupancyReport"></a>
#### ResourceAnalytics.OccupancyReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `groupBy` | texte |  |  |
| `peakHours` | texte |  |  |
| `rows` | [ResourceAnalytics.OccupancyRow](#m-ResourceAnalytics-OccupancyRow)[] |  |  |

<a id="m-ResourceAnalytics-CostResponse"></a>
#### ResourceAnalytics.CostResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `resourceCode` | texte |  |  |
| `kind` | texte |  |  |
| `basis` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `label` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |

<a id="m-ResourceAnalytics-CostRequest"></a>
#### ResourceAnalytics.CostRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte |  |  |
| `basis` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `label` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |

<a id="m-ResourceAnalytics-AppliedPrice"></a>
#### ResourceAnalytics.AppliedPrice

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `status` | texte |  |  |
| `unit` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `subtotal` | nombre décimal |  |  |
| `total` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `rules` | texte[] |  |  |

<a id="m-ResourceAnalytics-MarginRow"></a>
#### ResourceAnalytics.MarginRow

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `bookings` | entier |  |  |
| `bookedMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `revenue` | nombre décimal |  |  |
| `costs` | nombre décimal |  |  |
| `margin` | nombre décimal |  |  |
| `marginRate` | nombre décimal |  |  |
| `costsByKind` | objet { clé : nombre décimal } |  |  |

<a id="m-ResourceAnalytics-OccupancyRow"></a>
#### ResourceAnalytics.OccupancyRow

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `label` | texte |  |  |
| `openMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `bookedMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `occupancyRate` | nombre décimal |  |  |
| `peakOpenMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `peakBookedMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `peakOccupancyRate` | nombre décimal |  |  |
| `offPeakOpenMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `offPeakBookedMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `offPeakOccupancyRate` | nombre décimal |  |  |
| `bookings` | entier |  |  |
| `revenue` | nombre décimal |  |  |
