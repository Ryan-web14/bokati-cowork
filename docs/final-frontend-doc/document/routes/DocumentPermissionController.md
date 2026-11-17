# DocumentPermissionController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 4 route(s).

## Routes

<a id="r-delete-sni-api-v1-document-permissions"></a>
### DELETE /sni/api/v1/document-permissions

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentPermissionController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `targetType` | requête | `String` | oui |  |
| `targetCode` | requête | `String` | oui |  |
| `granteeType` | requête | `String` | oui |  |
| `granteeId` | requête | `Long` (code identifiant ou nombre) | oui |  |
| `permission` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-document-permissions"></a>
### GET /sni/api/v1/document-permissions

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<DocumentPermissionResponse>`
- Modèles : [DocumentPermissionResponse](#m-DocumentPermissionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentPermissionController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `targetType` | requête | `String` | oui |  |
| `targetCode` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-document-permissions"></a>
### POST /sni/api/v1/document-permissions

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `GrantPermissionRequest`
- Réponse : `DocumentPermissionResponse`
- Modèles : [DocumentPermissionResponse](#m-DocumentPermissionResponse), [GrantPermissionRequest](#m-GrantPermissionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentPermissionController.java`, méthode `grant`

<a id="r-get-sni-api-v1-document-permissions-effective"></a>
### GET /sni/api/v1/document-permissions/effective

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Set<String>`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentPermissionController.java`, méthode `effective`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userId` | requête | `Long` (code identifiant ou nombre) | oui |  |
| `documentCode` | requête | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DocumentPermissionResponse"></a>
#### DocumentPermissionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `targetType` | texte |  |  |
| `targetId` | code identifiant (texte) |  |  |
| `targetCode` | texte |  |  |
| `granteeType` | texte |  |  |
| `granteeId` | code identifiant (texte) |  |  |
| `granteeEmail` | texte |  |  |
| `permission` | texte |  |  |
| `grantedBy` | entier int64 (texte au-delà de 2^53) |  |  |
| `grantedByEmail` | texte |  |  |
| `grantedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-GrantPermissionRequest"></a>
#### GrantPermissionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `targetType` | texte | oui | longueur min `1` |
| `targetCode` | texte | oui | longueur min `1` |
| `granteeType` | texte | oui | longueur min `1` |
| `granteeId` | code identifiant (texte) | oui |  |
| `permission` | texte | oui | longueur min `1` |
