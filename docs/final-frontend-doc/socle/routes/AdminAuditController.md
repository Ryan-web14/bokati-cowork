# AdminAuditController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-audit-logs"></a>
### GET /sni/api/v1/admin/audit-logs

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `PaginatedResponse<AuditLog>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [AuditLog](#m-AuditLog), [AuditStatus](#m-AuditStatus)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/controller/AdminAuditController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `module` | requête | `String` |  |  |
| `action` | requête | `String` |  |  |
| `actorEmail` | requête | `String` |  |  |
| `status` | requête | `AuditStatus` |  |  |
| `createdFrom` | requête | `Instant` |  |  |
| `createdTo` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AuditLog"></a>
#### AuditLog

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `actorId` | entier int64 ⚠ |  |  |
| `actorEmail` | texte |  |  |
| `action` | texte |  |  |
| `ressource` | texte |  |  |
| `auditStatus` | enum : `SUCCESS`, `FAILURE` |  |  |
| `module` | texte |  |  |
| `ipAddress` | texte |  |  |
| `userAgent` | texte |  |  |
| `sessionId` | texte |  |  |
| `errorCode` | texte |  |  |
| `errorMessage` | texte |  |  |
| `metadata` | objet { clé : objet libre } |  |  |
| `diff` | objet { clé : objet libre } |  |  |

<a id="m-AuditStatus"></a>
#### AuditStatus

Énumération : `SUCCESS`, `FAILURE`
