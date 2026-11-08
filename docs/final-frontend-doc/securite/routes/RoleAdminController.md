# RoleAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-roles"></a>
### GET /sni/api/v1/admin/roles

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- Réponse : `List<RoleResponse>`
- Modèles : [RoleResponse](#m-RoleResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAdminController.java`, méthode `list`

<a id="r-post-sni-api-v1-admin-roles"></a>
### POST /sni/api/v1/admin/roles

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- En-tête `Idempotency-Key` : conseillé (opération `ROLE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `RoleRequest`
- Réponse : `RoleResponse`
- Modèles : [RoleResponse](#m-RoleResponse), [RoleRequest](#m-RoleRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAdminController.java`, méthode `create`

<a id="r-get-sni-api-v1-admin-roles-search-by-name"></a>
### GET /sni/api/v1/admin/roles/search/by-name

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- Réponse : `List<RoleResponse>`
- Modèles : [RoleResponse](#m-RoleResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAdminController.java`, méthode `searchByName`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-admin-roles-name"></a>
### DELETE /sni/api/v1/admin/roles/{name}

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAdminController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-admin-roles-name"></a>
### GET /sni/api/v1/admin/roles/{name}

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- Réponse : `RoleResponse`
- Modèles : [RoleResponse](#m-RoleResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAdminController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-admin-roles-name"></a>
### PUT /sni/api/v1/admin/roles/{name}

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- En-tête `Idempotency-Key` : conseillé (opération `ROLE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `RoleRequest`
- Réponse : `RoleResponse`
- Modèles : [RoleResponse](#m-RoleResponse), [RoleRequest](#m-RoleRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAdminController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-roles-name-activate"></a>
### PATCH /sni/api/v1/admin/roles/{name}/activate

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- En-tête `Idempotency-Key` : conseillé (opération `ROLE_ACTIVATE`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAdminController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-roles-name-deactivate"></a>
### PATCH /sni/api/v1/admin/roles/{name}/deactivate

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- En-tête `Idempotency-Key` : conseillé (opération `ROLE_DEACTIVATE`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAdminController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-RoleResponse"></a>
#### RoleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte |  |  |
| `displayName` | texte |  |  |
| `description` | texte |  |  |
| `isActive` | booléen |  |  |
| `permissions` | objet { clé : texte } |  |  |

<a id="m-RoleRequest"></a>
#### RoleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `0`, longueur max `250`, motif `^[A-Z_]+$` |
| `displayName` | texte | oui | longueur min `0`, longueur max `150` |
| `description` | texte |  |  |
| `isActive` | booléen |  |  |
