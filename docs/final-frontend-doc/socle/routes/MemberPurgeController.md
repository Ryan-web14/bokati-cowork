# MemberPurgeController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 4 route(s).

## Routes

<a id="r-post-sni-api-v1-admin-maintenance-members-purge-batch"></a>
### POST /sni/api/v1/admin/maintenance/members/purge-batch

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('ROLE_ADMIN', 'ADMIN')`
- Corps JSON (obligatoire) : `MemberPurgeController.BatchPurgeRequest`
- Réponse : `MemberPurgeService.BatchPurgeReport`
- Modèles : [MemberPurgeService.BatchPurgeReport](#m-MemberPurgeService-BatchPurgeReport), [MemberPurgeController.BatchPurgeRequest](#m-MemberPurgeController-BatchPurgeRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/maintenance/MemberPurgeController.java`, méthode `purgeBatch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `confirm` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-admin-maintenance-members-purge-candidates"></a>
### GET /sni/api/v1/admin/maintenance/members/purge-candidates

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('ROLE_ADMIN', 'ADMIN')`
- Réponse : `List<MemberPurgeService.MemberCandidate>`
- Modèles : [MemberPurgeService.MemberCandidate](#m-MemberPurgeService-MemberCandidate)
- Code : `src/main/java/com/sni/bokaticowork/core/maintenance/MemberPurgeController.java`, méthode `candidates`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `createdBefore` | requête | `Instant` | oui |  |
| `limit` | requête | `int` |  | 200 |

<a id="r-delete-sni-api-v1-admin-maintenance-members-membercode"></a>
### DELETE /sni/api/v1/admin/maintenance/members/{memberCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('ROLE_ADMIN', 'ADMIN')`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/core/maintenance/MemberPurgeController.java`, méthode `purge`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberCode` | chemin | `String` | oui |  |
| `confirm` | requête | `boolean` |  | false |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-admin-maintenance-members-membercode-purge-preview"></a>
### GET /sni/api/v1/admin/maintenance/members/{memberCode}/purge-preview

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('ROLE_ADMIN', 'ADMIN')`
- Réponse : `MemberPurgeService.PurgeReport`
- Modèles : [MemberPurgeService.PurgeReport](#m-MemberPurgeService-PurgeReport)
- Code : `src/main/java/com/sni/bokaticowork/core/maintenance/MemberPurgeController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-MemberPurgeService-BatchPurgeReport"></a>
#### MemberPurgeService.BatchPurgeReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `membersPurged` | entier |  |  |
| `rowsAffected` | entier int64 ⚠ |  |  |
| `reports` | [MemberPurgeService.PurgeReport](#m-MemberPurgeService-PurgeReport)[] |  |  |

<a id="m-MemberPurgeController-BatchPurgeRequest"></a>
#### MemberPurgeController.BatchPurgeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberCodes` | texte[] | oui | éléments min `1` |
| `reason` | texte |  |  |

<a id="m-MemberPurgeService-MemberCandidate"></a>
#### MemberPurgeService.MemberCandidate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberCode` | texte |  |  |
| `email` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-MemberPurgeService-PurgeReport"></a>
#### MemberPurgeService.PurgeReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberCode` | texte |  |  |
| `email` | texte |  |  |
| `dryRun` | booléen |  |  |
| `rowsAffected` | entier int64 ⚠ |  |  |
| `rowsByTable` | objet { clé : entier } |  |  |
| `skippedTables` | texte[] |  |  |
| `durationMs` | entier int64 ⚠ |  |  |
