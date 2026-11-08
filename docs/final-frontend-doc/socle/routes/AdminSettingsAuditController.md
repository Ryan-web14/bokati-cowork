# AdminSettingsAuditController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-settings-audit-logs"></a>
### GET /sni/api/v1/admin/settings-audit-logs

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `PaginatedResponse<SettingsAuditLogs>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SettingsAuditLogs](#m-SettingsAuditLogs)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/controller/AdminSettingsAuditController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `settingKey` | requête | `String` |  |  |
| `changedBy` | requête | `Long` |  |  |
| `changedFrom` | requête | `Instant` |  |  |
| `changedTo` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=changedAt,DESC |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SettingsAuditLogs"></a>
#### SettingsAuditLogs

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `settingKey` | texte |  |  |
| `oldValue` | texte |  |  |
| `newValue` | texte |  |  |
| `changedBy` | entier int64 ⚠ |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |
| `reason` | texte |  |  |
