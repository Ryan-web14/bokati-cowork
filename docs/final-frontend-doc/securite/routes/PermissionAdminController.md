# PermissionAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-permissions"></a>
### GET /sni/api/v1/admin/permissions

- Accès : back-office : rôle d'administration et droit `SYSTEM:PERMISSIONS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- Réponse : `PaginatedResponse<PermissionResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [PermissionResponse](#m-PermissionResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/PermissionAdminController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `activeOnly` | requête | `boolean` |  | false |
| `systemOnly` | requête | `boolean` |  | false |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=name,ASC |

<a id="r-post-sni-api-v1-admin-permissions"></a>
### POST /sni/api/v1/admin/permissions

- Accès : back-office : rôle d'administration et droit `SYSTEM:PERMISSIONS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- En-tête `Idempotency-Key` : conseillé (opération `PERMISSION_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `PermissionRequest`
- Réponse : `PermissionResponse`
- Modèles : [PermissionResponse](#m-PermissionResponse), [PermissionRequest](#m-PermissionRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/PermissionAdminController.java`, méthode `create`

<a id="r-get-sni-api-v1-admin-permissions-search-by-name"></a>
### GET /sni/api/v1/admin/permissions/search/by-name

- Accès : back-office : rôle d'administration et droit `SYSTEM:PERMISSIONS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- Réponse : `List<PermissionResponse>`
- Modèles : [PermissionResponse](#m-PermissionResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/PermissionAdminController.java`, méthode `searchByName`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-admin-permissions-name"></a>
### DELETE /sni/api/v1/admin/permissions/{name}

- Accès : back-office : rôle d'administration et droit `SYSTEM:PERMISSIONS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/PermissionAdminController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-admin-permissions-name"></a>
### GET /sni/api/v1/admin/permissions/{name}

- Accès : back-office : rôle d'administration et droit `SYSTEM:PERMISSIONS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- Réponse : `PermissionResponse`
- Modèles : [PermissionResponse](#m-PermissionResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/PermissionAdminController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-admin-permissions-name"></a>
### PUT /sni/api/v1/admin/permissions/{name}

- Accès : back-office : rôle d'administration et droit `SYSTEM:PERMISSIONS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- En-tête `Idempotency-Key` : conseillé (opération `PERMISSION_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `PermissionRequest`
- Réponse : `PermissionResponse`
- Modèles : [PermissionResponse](#m-PermissionResponse), [PermissionRequest](#m-PermissionRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/PermissionAdminController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-permissions-name-activate"></a>
### PATCH /sni/api/v1/admin/permissions/{name}/activate

- Accès : back-office : rôle d'administration et droit `SYSTEM:PERMISSIONS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- En-tête `Idempotency-Key` : conseillé (opération `PERMISSION_ACTIVATE`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/PermissionAdminController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-permissions-name-deactivate"></a>
### PATCH /sni/api/v1/admin/permissions/{name}/deactivate

- Accès : back-office : rôle d'administration et droit `SYSTEM:PERMISSIONS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- En-tête `Idempotency-Key` : conseillé (opération `PERMISSION_DEACTIVATE`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/PermissionAdminController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PermissionResponse"></a>
#### PermissionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte |  |  |
| `displayName` | texte |  |  |
| `module` | texte |  |  |
| `action` | texte |  |  |
| `fullPermissionName` | texte |  |  |
| `isActive` | booléen |  |  |

<a id="m-PermissionRequest"></a>
#### PermissionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `displayName` | texte |  |  |
| `module` | texte |  |  |
| `action` | texte |  |  |
| `isActive` | booléen |  |  |
