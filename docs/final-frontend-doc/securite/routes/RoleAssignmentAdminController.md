# RoleAssignmentAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 7 route(s).

## Routes

<a id="r-patch-sni-api-v1-admin-roles-roleid-permission-names"></a>
### PATCH /sni/api/v1/admin/roles/{roleId}/permission-names

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- En-tête `Idempotency-Key` : conseillé (opération `ROLE_PERMISSION_REPLACE_BY_NAME`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateRolePermissionNamesRequest`
- Réponse : `vide`
- Modèles : [UpdateRolePermissionNamesRequest](#m-UpdateRolePermissionNamesRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAssignmentAdminController.java`, méthode `replaceRolePermissionNames`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `roleId` | chemin | `Long` | oui |  |

<a id="r-delete-sni-api-v1-admin-roles-roleid-permissions"></a>
### DELETE /sni/api/v1/admin/roles/{roleId}/permissions

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAssignmentAdminController.java`, méthode `clearRolePermissions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `roleId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-admin-roles-roleid-permissions"></a>
### GET /sni/api/v1/admin/roles/{roleId}/permissions

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- Réponse : `List<PermissionResponse>`
- Modèles : [PermissionResponse](#m-PermissionResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAssignmentAdminController.java`, méthode `getRolePermissions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `roleId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-admin-roles-roleid-permissions"></a>
### PATCH /sni/api/v1/admin/roles/{roleId}/permissions

- Accès : back-office : rôle d'administration et droit `SYSTEM:ROLES` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:PERMISSIONS')`
- En-tête `Idempotency-Key` : conseillé (opération `ROLE_PERMISSION_REPLACE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateRolePermissionsRequest`
- Réponse : `vide`
- Modèles : [UpdateRolePermissionsRequest](#m-UpdateRolePermissionsRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAssignmentAdminController.java`, méthode `replaceRolePermissions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `roleId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-admin-users-userid-roles"></a>
### GET /sni/api/v1/admin/users/{userId}/roles

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- Réponse : `List<RoleResponse>`
- Modèles : [RoleResponse](#m-RoleResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAssignmentAdminController.java`, méthode `getUserRoles`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-admin-users-userid-roles"></a>
### POST /sni/api/v1/admin/users/{userId}/roles

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- En-tête `Idempotency-Key` : conseillé (opération `ROLE_USER_ASSIGN`, voir le README racine)
- Corps JSON (obligatoire) : `AssignRoleToUserRequest`
- Réponse : `vide`
- Modèles : [AssignRoleToUserRequest](#m-AssignRoleToUserRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAssignmentAdminController.java`, méthode `assignRole`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userId` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-admin-users-userid-roles-roleid"></a>
### DELETE /sni/api/v1/admin/users/{userId}/roles/{roleId}

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:ROLES')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/role/controller/RoleAssignmentAdminController.java`, méthode `removeRole`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userId` | chemin | `String` | oui |  |
| `roleId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-UpdateRolePermissionNamesRequest"></a>
#### UpdateRolePermissionNamesRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `permissionNames` | texte[] | oui | éléments min `1` |

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

<a id="m-UpdateRolePermissionsRequest"></a>
#### UpdateRolePermissionsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `permissionIds` | entier int64 ⚠[] | oui | éléments min `1` |

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

<a id="m-AssignRoleToUserRequest"></a>
#### AssignRoleToUserRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `roleName` | texte | oui | longueur min `1` |
