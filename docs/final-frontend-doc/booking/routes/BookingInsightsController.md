# BookingInsightsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-bookings-client-report-pdf"></a>
### GET /sni/api/v1/bookings/client-report.pdf

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Renvoie : `application/pdf`
- Réponse : `fichier (octets)`
- Modèles : [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingInsightsController.java`, méthode `clientReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `SubscriberType` | oui |  |
| `ownerCode` | requête | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-bookings-dashboard"></a>
### GET /sni/api/v1/bookings/dashboard

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingInsights.Dashboard`
- Modèles : [BookingInsights.Dashboard](#m-BookingInsights-Dashboard)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingInsightsController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-bookings-reviews"></a>
### GET /sni/api/v1/bookings/reviews

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingInsights.ReviewLine>`
- Modèles : [BookingInsights.ReviewLine](#m-BookingInsights-ReviewLine)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingInsightsController.java`, méthode `reviews`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `resourceCode` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-BookingInsights-Dashboard"></a>
#### BookingInsights.Dashboard

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `bookings` | entier int64 (texte au-delà de 2^53) |  |  |
| `confirmed` | entier int64 (texte au-delà de 2^53) |  |  |
| `completed` | entier int64 (texte au-delà de 2^53) |  |  |
| `cancelled` | entier int64 (texte au-delà de 2^53) |  |  |
| `lateCancellations` | entier int64 (texte au-delà de 2^53) |  |  |
| `noShows` | entier int64 (texte au-delà de 2^53) |  |  |
| `expired` | entier int64 (texte au-delà de 2^53) |  |  |
| `noShowRate` | nombre décimal |  |  |
| `averageLeadHours` | nombre décimal |  |  |
| `revenue` | nombre décimal |  |  |
| `channels` | [BookingInsights.ChannelLine](#m-BookingInsights-ChannelLine)[] |  |  |
| `topResources` | [BookingInsights.ResourceLine](#m-BookingInsights-ResourceLine)[] |  |  |

<a id="m-BookingInsights-ReviewLine"></a>
#### BookingInsights.ReviewLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `reviews` | entier int64 (texte au-delà de 2^53) |  |  |
| `averageScore` | nombre décimal |  |  |
| `lowScores` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-BookingInsights-ChannelLine"></a>
#### BookingInsights.ChannelLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `channel` | texte |  |  |
| `bookings` | entier int64 (texte au-delà de 2^53) |  |  |
| `revenue` | nombre décimal |  |  |

<a id="m-BookingInsights-ResourceLine"></a>
#### BookingInsights.ResourceLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `resourceName` | texte |  |  |
| `bookings` | entier int64 (texte au-delà de 2^53) |  |  |
| `revenue` | nombre décimal |  |  |
