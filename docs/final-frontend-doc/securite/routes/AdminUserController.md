# AdminUserController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 14 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-users"></a>
### GET /sni/api/v1/admin/users

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- Réponse : `PaginatedResponse<AdminUserResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `email` | requête | `String` |  |  |
| `enabled` | requête | `Boolean` |  |  |
| `locked` | requête | `Boolean` |  |  |
| `deleted` | requête | `Boolean` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  |  |

<a id="r-post-sni-api-v1-admin-users"></a>
### POST /sni/api/v1/admin/users

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- En-tête `Idempotency-Key` : conseillé (opération `ADMIN_USER_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `AdminCreateUserRequest`
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse), [AdminCreateUserRequest](#m-AdminCreateUserRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `create`

<a id="r-get-sni-api-v1-admin-users-by-email"></a>
### GET /sni/api/v1/admin/users/by-email

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `getByEmail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `email` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-admin-users-search-by-name"></a>
### GET /sni/api/v1/admin/users/search/by-name

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- Réponse : `List<AdminUserResponse>`
- Modèles : [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `searchByName`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-admin-users-usercode"></a>
### DELETE /sni/api/v1/admin/users/{userCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `archive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-admin-users-usercode"></a>
### GET /sni/api/v1/admin/users/{userCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-admin-users-usercode"></a>
### PUT /sni/api/v1/admin/users/{userCode}

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- En-tête `Idempotency-Key` : conseillé (opération `ADMIN_USER_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `AdminUpdateUserRequest`
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse), [AdminUpdateUserRequest](#m-AdminUpdateUserRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-users-usercode-activate"></a>
### PATCH /sni/api/v1/admin/users/{userCode}/activate

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- En-tête `Idempotency-Key` : conseillé (opération `ADMIN_USER_ACTIVATE`, voir le README racine)
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-users-usercode-deactivate"></a>
### PATCH /sni/api/v1/admin/users/{userCode}/deactivate

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- En-tête `Idempotency-Key` : conseillé (opération `ADMIN_USER_DEACTIVATE`, voir le README racine)
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-users-usercode-disable"></a>
### PATCH /sni/api/v1/admin/users/{userCode}/disable

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- En-tête `Idempotency-Key` : conseillé (opération `ADMIN_USER_DEACTIVATE`, voir le README racine)
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-users-usercode-enable"></a>
### PATCH /sni/api/v1/admin/users/{userCode}/enable

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- En-tête `Idempotency-Key` : conseillé (opération `ADMIN_USER_ACTIVATE`, voir le README racine)
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-users-usercode-password-reset"></a>
### PATCH /sni/api/v1/admin/users/{userCode}/password/reset

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- En-tête `Idempotency-Key` : conseillé (opération `ADMIN_USER_RESET_PASSWORD`, voir le README racine)
- Corps JSON (facultatif) : `AdminResetUserPasswordRequest`
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse), [AdminResetUserPasswordRequest](#m-AdminResetUserPasswordRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `resetPassword`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-admin-users-usercode-reset-password"></a>
### POST /sni/api/v1/admin/users/{userCode}/reset-password

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `initiatePasswordReset`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-admin-users-usercode-unlock"></a>
### PATCH /sni/api/v1/admin/users/{userCode}/unlock

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- En-tête `Idempotency-Key` : conseillé (opération `ADMIN_USER_UNLOCK`, voir le README racine)
- Réponse : `AdminUserResponse`
- Modèles : [AdminUserResponse](#m-AdminUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/AdminUserController.java`, méthode `unlock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AdminUserResponse"></a>
#### AdminUserResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `userId` | texte |  |  |
| `email` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `accountEnabled` | booléen |  |  |
| `accountLocked` | booléen |  |  |
| `accountExpired` | booléen |  |  |
| `deleted` | booléen |  |  |
| `failedLoginAttempts` | entier |  |  |
| `lastLogin` | date-heure locale sans fuseau |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |
| `roleNames` | texte[] |  |  |
| `generatedPassword` | texte |  |  |

<a id="m-AdminCreateUserRequest"></a>
#### AdminCreateUserRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `email` | string (email) | oui | longueur min `1` |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `password` | texte |  |  |
| `generatePassword` | booléen |  |  |
| `roleNames` | texte[] |  |  |

<a id="m-AdminUpdateUserRequest"></a>
#### AdminUpdateUserRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `email` | string (email) |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `isAccountEnabled` | booléen |  |  |
| `isAccountLocked` | booléen |  |  |
| `roleNames` | texte[] |  |  |

<a id="m-AdminResetUserPasswordRequest"></a>
#### AdminResetUserPasswordRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `newPassword` | texte |  |  |
| `generatePassword` | booléen |  |  |
