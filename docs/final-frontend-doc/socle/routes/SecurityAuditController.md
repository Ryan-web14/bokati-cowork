# SecurityAuditController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-audit-security-events"></a>
### GET /sni/api/v1/admin/audit/security-events

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `Page<SecurityAuditService.EventView>`
- Modèles : [SecurityAuditService.EventView](#m-SecurityAuditService-EventView)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/security/SecurityAuditController.java`, méthode `events`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `type` | requête | `String` |  |  |
| `login` | requête | `String` |  |  |
| `userCode` | requête | `String` |  |  |
| `ip` | requête | `String` |  |  |
| `requestId` | requête | `String` |  |  |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 50 |

<a id="r-delete-sni-api-v1-admin-audit-sessions-sessionid"></a>
### DELETE /sni/api/v1/admin/audit/sessions/{sessionId}

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `SecurityAuditService.SessionView`
- Modèles : [SecurityAuditService.SessionView](#m-SecurityAuditService-SessionView)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/security/SecurityAuditController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionId` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-delete-sni-api-v1-admin-audit-users-usercode-sessions"></a>
### DELETE /sni/api/v1/admin/audit/users/{userCode}/sessions

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/core/audit/security/SecurityAuditController.java`, méthode `revokeAll`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-admin-audit-users-usercode-sessions"></a>
### GET /sni/api/v1/admin/audit/users/{userCode}/sessions

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `List<SecurityAuditService.SessionView>`
- Modèles : [SecurityAuditService.SessionView](#m-SecurityAuditService-SessionView)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/security/SecurityAuditController.java`, méthode `sessions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |
| `activeOnly` | requête | `boolean` |  | true |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SecurityAuditService-EventView"></a>
#### SecurityAuditService.EventView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `eventType` | texte |  |  |
| `userCode` | texte |  |  |
| `login` | texte |  |  |
| `ipAddress` | texte |  |  |
| `userAgent` | texte |  |  |
| `requestId` | texte |  |  |
| `path` | texte |  |  |
| `detailsJson` | texte |  |  |

<a id="m-SecurityAuditService-SessionView"></a>
#### SecurityAuditService.SessionView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionId` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `lastAccessAt` | date-heure ISO 8601 UTC |  |  |
| `expiredAt` | date-heure ISO 8601 UTC |  |  |
| `active` | booléen |  |  |
| `revoked` | booléen |  |  |
| `ipAddress` | texte |  |  |
| `deviceType` | texte |  |  |
| `userAgent` | texte |  |  |
| `location` | texte |  |  |
| `roles` | texte |  |  |
| `revokedAt` | date-heure ISO 8601 UTC |  |  |
| `revokedBy` | texte |  |  |
| `revokedReason` | texte |  |  |
