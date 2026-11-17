# AlertController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-audit-alerts"></a>
### GET /sni/api/v1/admin/audit/alerts

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `PaginatedResponse<AlertService.Alert>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [AlertService.Alert](#m-AlertService-Alert)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/alert/AlertController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `String` |  |  |
| `rule` | requête | `String` |  |  |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 50 |

<a id="r-post-sni-api-v1-admin-audit-alerts-evaluate"></a>
### POST /sni/api/v1/admin/audit/alerts/evaluate

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `List<AlertService.RuleView>`
- Modèles : [AlertService.RuleView](#m-AlertService-RuleView)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/alert/AlertController.java`, méthode `evaluate`

<a id="r-get-sni-api-v1-admin-audit-alerts-rules"></a>
### GET /sni/api/v1/admin/audit/alerts/rules

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `List<AlertService.RuleView>`
- Modèles : [AlertService.RuleView](#m-AlertService-RuleView)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/alert/AlertController.java`, méthode `rules`

<a id="r-get-sni-api-v1-admin-audit-alerts-id"></a>
### GET /sni/api/v1/admin/audit/alerts/{id}

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `AlertService.Alert`
- Modèles : [AlertService.Alert](#m-AlertService-Alert)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/alert/AlertController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `long` (code identifiant ou nombre) | oui |  |

<a id="r-post-sni-api-v1-admin-audit-alerts-id-acknowledge"></a>
### POST /sni/api/v1/admin/audit/alerts/{id}/acknowledge

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Corps JSON (facultatif) : `Map<String, String>`
- Réponse : `AlertService.Alert`
- Modèles : [AlertService.Alert](#m-AlertService-Alert)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/alert/AlertController.java`, méthode `acknowledge`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `long` (code identifiant ou nombre) | oui |  |

<a id="r-get-sni-api-v1-admin-audit-dashboard"></a>
### GET /sni/api/v1/admin/audit/dashboard

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/core/audit/alert/AlertController.java`, méthode `dashboard`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AlertService-Alert"></a>
#### AlertService.Alert

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `ruleCode` | texte |  |  |
| `severity` | texte |  |  |
| `status` | texte |  |  |
| `title` | texte |  |  |
| `message` | texte |  |  |
| `metricValue` | nombre décimal |  |  |
| `threshold` | nombre décimal |  |  |
| `openedAt` | date-heure ISO 8601 UTC |  |  |
| `lastSeenAt` | date-heure ISO 8601 UTC |  |  |
| `resolvedAt` | date-heure ISO 8601 UTC |  |  |
| `acknowledgedAt` | date-heure ISO 8601 UTC |  |  |
| `acknowledgedBy` | texte |  |  |
| `acknowledgeNote` | texte |  |  |
| `notifications` | texte |  |  |

<a id="m-AlertService-RuleView"></a>
#### AlertService.RuleView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `settingKey` | texte |  |  |
| `threshold` | entier int64 (texte au-delà de 2^53) |  |  |
| `enabled` | booléen |  |  |
| `severity` | texte |  |  |
| `title` | texte |  |  |
| `currentValue` | nombre décimal |  |  |
| `detail` | texte |  |  |
