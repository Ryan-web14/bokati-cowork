# TrafficController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-audit-traffic"></a>
### GET /sni/api/v1/admin/audit/traffic

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `List<TrafficService.TrafficPoint>`
- Modèles : [TrafficService.TrafficPoint](#m-TrafficService-TrafficPoint)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/traffic/TrafficController.java`, méthode `series`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `granularity` | requête | `String` |  |  |
| `route` | requête | `String` |  |  |
| `method` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-admin-audit-traffic-requests"></a>
### GET /sni/api/v1/admin/audit/traffic/requests

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `PaginatedResponse<TrafficService.RequestEntry>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [TrafficService.RequestEntry](#m-TrafficService-RequestEntry)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/traffic/TrafficController.java`, méthode `requests`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `reason` | requête | `String` |  |  |
| `route` | requête | `String` |  |  |
| `user` | requête | `String` |  |  |
| `requestId` | requête | `String` |  |  |
| `minStatus` | requête | `Integer` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 50 |

<a id="r-get-sni-api-v1-admin-audit-traffic-routes"></a>
### GET /sni/api/v1/admin/audit/traffic/routes

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `List<TrafficService.RouteTraffic>`
- Modèles : [TrafficService.RouteTraffic](#m-TrafficService-RouteTraffic)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/traffic/TrafficController.java`, méthode `routes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `sort` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-TrafficService-TrafficPoint"></a>
#### TrafficService.TrafficPoint

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `period` | date-heure ISO 8601 UTC |  |  |
| `requests` | entier int64 (texte au-delà de 2^53) |  |  |
| `clientErrors` | entier int64 (texte au-delà de 2^53) |  |  |
| `serverErrors` | entier int64 (texte au-delà de 2^53) |  |  |
| `averageMs` | entier int64 (texte au-delà de 2^53) |  |  |
| `p50Ms` | entier int64 (texte au-delà de 2^53) |  |  |
| `p95Ms` | entier int64 (texte au-delà de 2^53) |  |  |
| `maxMs` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-TrafficService-RequestEntry"></a>
#### TrafficService.RequestEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `requestId` | texte |  |  |
| `method` | texte |  |  |
| `path` | texte |  |  |
| `route` | texte |  |  |
| `status` | entier |  |  |
| `durationMs` | entier int64 (texte au-delà de 2^53) |  |  |
| `userEmail` | texte |  |  |
| `clientIp` | texte |  |  |
| `reason` | texte |  |  |

<a id="m-TrafficService-RouteTraffic"></a>
#### TrafficService.RouteTraffic

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `method` | texte |  |  |
| `route` | texte |  |  |
| `requests` | entier int64 (texte au-delà de 2^53) |  |  |
| `clientErrors` | entier int64 (texte au-delà de 2^53) |  |  |
| `serverErrors` | entier int64 (texte au-delà de 2^53) |  |  |
| `averageMs` | entier int64 (texte au-delà de 2^53) |  |  |
| `p50Ms` | entier int64 (texte au-delà de 2^53) |  |  |
| `p95Ms` | entier int64 (texte au-delà de 2^53) |  |  |
| `maxMs` | entier int64 (texte au-delà de 2^53) |  |  |
