# TimelineController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-audit-timeline"></a>
### GET /sni/api/v1/admin/audit/timeline

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `TimelineService.Page`
- Modèles : [TimelineService.Page](#m-TimelineService-Page)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/timeline/TimelineController.java`, méthode `read`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `user` | requête | `String` |  |  |
| `entityType` | requête | `String` |  |  |
| `entityCode` | requête | `String` |  |  |
| `requestId` | requête | `String` |  |  |
| `sources` | requête | `String` |  |  |
| `cursor` | requête | `String` |  |  |
| `limit` | requête | `Integer` |  |  |

<a id="r-get-sni-api-v1-admin-audit-timeline-export"></a>
### GET /sni/api/v1/admin/audit/timeline/export

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/core/audit/timeline/TimelineController.java`, méthode `export`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `user` | requête | `String` |  |  |
| `entityType` | requête | `String` |  |  |
| `entityCode` | requête | `String` |  |  |
| `requestId` | requête | `String` |  |  |
| `sources` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-admin-audit-timeline-sources"></a>
### GET /sni/api/v1/admin/audit/timeline/sources

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `Set<String>`
- Code : `src/main/java/com/sni/bokaticowork/core/audit/timeline/TimelineController.java`, méthode `sources`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-TimelineService-Page"></a>
#### TimelineService.Page

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `items` | [TimelineService.Entry](#m-TimelineService-Entry)[] |  |  |
| `nextCursor` | texte |  |  |

<a id="m-TimelineService-Entry"></a>
#### TimelineService.Entry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
| `source` | texte |  |  |
| `refId` | texte |  |  |
| `kind` | texte |  |  |
| `actor` | texte |  |  |
| `entityType` | texte |  |  |
| `entityCode` | texte |  |  |
| `summary` | texte |  |  |
| `requestId` | texte |  |  |
| `ipAddress` | texte |  |  |
| `status` | texte |  |  |
