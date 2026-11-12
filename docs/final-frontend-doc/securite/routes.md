# securite : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

54 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/admin/users` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-get-sni-api-v1-admin-users) |
| POST | `/sni/api/v1/admin/users` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-post-sni-api-v1-admin-users) |
| GET | `/sni/api/v1/admin/users/by-email` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-get-sni-api-v1-admin-users-by-email) |
| GET | `/sni/api/v1/admin/users/search/by-name` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-get-sni-api-v1-admin-users-search-by-name) |
| DELETE | `/sni/api/v1/admin/users/{userCode}` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-delete-sni-api-v1-admin-users-usercode) |
| GET | `/sni/api/v1/admin/users/{userCode}` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-get-sni-api-v1-admin-users-usercode) |
| PUT | `/sni/api/v1/admin/users/{userCode}` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-put-sni-api-v1-admin-users-usercode) |
| PATCH | `/sni/api/v1/admin/users/{userCode}/activate` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-patch-sni-api-v1-admin-users-usercode-activate) |
| PATCH | `/sni/api/v1/admin/users/{userCode}/deactivate` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-patch-sni-api-v1-admin-users-usercode-deactivate) |
| PATCH | `/sni/api/v1/admin/users/{userCode}/disable` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-patch-sni-api-v1-admin-users-usercode-disable) |
| PATCH | `/sni/api/v1/admin/users/{userCode}/enable` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-patch-sni-api-v1-admin-users-usercode-enable) |
| PATCH | `/sni/api/v1/admin/users/{userCode}/password/reset` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-patch-sni-api-v1-admin-users-usercode-password-reset) |
| POST | `/sni/api/v1/admin/users/{userCode}/reset-password` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-post-sni-api-v1-admin-users-usercode-reset-password) |
| PATCH | `/sni/api/v1/admin/users/{userCode}/unlock` | SYSTEM:USERS | [AdminUserController](routes/AdminUserController.md#r-patch-sni-api-v1-admin-users-usercode-unlock) |
| POST | `/sni/api/v1/auth/email/verify/resend` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-email-verify-resend) |
| POST | `/sni/api/v1/auth/login` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-login) |
| POST | `/sni/api/v1/auth/logout` | connecté | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-logout) |
| GET | `/sni/api/v1/auth/me` | connecté | [AuthenticationController](routes/AuthenticationController.md#r-get-sni-api-v1-auth-me) |
| POST | `/sni/api/v1/auth/ott/request` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-ott-request) |
| POST | `/sni/api/v1/auth/ott/validate` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-ott-validate) |
| POST | `/sni/api/v1/auth/password-reset/confirm` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-password-reset-confirm) |
| POST | `/sni/api/v1/auth/password-reset/request` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-password-reset-request) |
| POST | `/sni/api/v1/auth/refresh` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-refresh) |
| POST | `/sni/api/v1/auth/register` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-register) |
| POST | `/sni/api/v1/auth/unlock-account` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-unlock-account) |
| POST | `/sni/api/v1/auth/unlock-account/confirm` | public | [AuthenticationController](routes/AuthenticationController.md#r-post-sni-api-v1-auth-unlock-account-confirm) |
| GET | `/sni/api/v1/admin/one-time-tokens` | SYSTEM:SETTINGS | [OneTimeTokenAdminController](routes/OneTimeTokenAdminController.md#r-get-sni-api-v1-admin-one-time-tokens) |
| GET | `/sni/api/v1/auth/password-reset/form` | public | [PasswordResetFormController](routes/PasswordResetFormController.md#r-get-sni-api-v1-auth-password-reset-form) |
| POST | `/sni/api/v1/auth/password-reset/form` | public | [PasswordResetFormController](routes/PasswordResetFormController.md#r-post-sni-api-v1-auth-password-reset-form) |
| GET | `/sni/api/v1/admin/permissions` | SYSTEM:PERMISSIONS | [PermissionAdminController](routes/PermissionAdminController.md#r-get-sni-api-v1-admin-permissions) |
| POST | `/sni/api/v1/admin/permissions` | SYSTEM:PERMISSIONS | [PermissionAdminController](routes/PermissionAdminController.md#r-post-sni-api-v1-admin-permissions) |
| GET | `/sni/api/v1/admin/permissions/search/by-name` | SYSTEM:PERMISSIONS | [PermissionAdminController](routes/PermissionAdminController.md#r-get-sni-api-v1-admin-permissions-search-by-name) |
| DELETE | `/sni/api/v1/admin/permissions/{name}` | SYSTEM:PERMISSIONS | [PermissionAdminController](routes/PermissionAdminController.md#r-delete-sni-api-v1-admin-permissions-name) |
| GET | `/sni/api/v1/admin/permissions/{name}` | SYSTEM:PERMISSIONS | [PermissionAdminController](routes/PermissionAdminController.md#r-get-sni-api-v1-admin-permissions-name) |
| PUT | `/sni/api/v1/admin/permissions/{name}` | SYSTEM:PERMISSIONS | [PermissionAdminController](routes/PermissionAdminController.md#r-put-sni-api-v1-admin-permissions-name) |
| PATCH | `/sni/api/v1/admin/permissions/{name}/activate` | SYSTEM:PERMISSIONS | [PermissionAdminController](routes/PermissionAdminController.md#r-patch-sni-api-v1-admin-permissions-name-activate) |
| PATCH | `/sni/api/v1/admin/permissions/{name}/deactivate` | SYSTEM:PERMISSIONS | [PermissionAdminController](routes/PermissionAdminController.md#r-patch-sni-api-v1-admin-permissions-name-deactivate) |
| GET | `/sni/api/v1/admin/roles` | SYSTEM:ROLES | [RoleAdminController](routes/RoleAdminController.md#r-get-sni-api-v1-admin-roles) |
| POST | `/sni/api/v1/admin/roles` | SYSTEM:ROLES | [RoleAdminController](routes/RoleAdminController.md#r-post-sni-api-v1-admin-roles) |
| GET | `/sni/api/v1/admin/roles/search/by-name` | SYSTEM:ROLES | [RoleAdminController](routes/RoleAdminController.md#r-get-sni-api-v1-admin-roles-search-by-name) |
| DELETE | `/sni/api/v1/admin/roles/{name}` | SYSTEM:ROLES | [RoleAdminController](routes/RoleAdminController.md#r-delete-sni-api-v1-admin-roles-name) |
| GET | `/sni/api/v1/admin/roles/{name}` | SYSTEM:ROLES | [RoleAdminController](routes/RoleAdminController.md#r-get-sni-api-v1-admin-roles-name) |
| PUT | `/sni/api/v1/admin/roles/{name}` | SYSTEM:ROLES | [RoleAdminController](routes/RoleAdminController.md#r-put-sni-api-v1-admin-roles-name) |
| PATCH | `/sni/api/v1/admin/roles/{name}/activate` | SYSTEM:ROLES | [RoleAdminController](routes/RoleAdminController.md#r-patch-sni-api-v1-admin-roles-name-activate) |
| PATCH | `/sni/api/v1/admin/roles/{name}/deactivate` | SYSTEM:ROLES | [RoleAdminController](routes/RoleAdminController.md#r-patch-sni-api-v1-admin-roles-name-deactivate) |
| PATCH | `/sni/api/v1/admin/roles/{roleId}/permission-names` | SYSTEM:ROLES | [RoleAssignmentAdminController](routes/RoleAssignmentAdminController.md#r-patch-sni-api-v1-admin-roles-roleid-permission-names) |
| DELETE | `/sni/api/v1/admin/roles/{roleId}/permissions` | SYSTEM:ROLES | [RoleAssignmentAdminController](routes/RoleAssignmentAdminController.md#r-delete-sni-api-v1-admin-roles-roleid-permissions) |
| GET | `/sni/api/v1/admin/roles/{roleId}/permissions` | SYSTEM:ROLES | [RoleAssignmentAdminController](routes/RoleAssignmentAdminController.md#r-get-sni-api-v1-admin-roles-roleid-permissions) |
| PATCH | `/sni/api/v1/admin/roles/{roleId}/permissions` | SYSTEM:ROLES | [RoleAssignmentAdminController](routes/RoleAssignmentAdminController.md#r-patch-sni-api-v1-admin-roles-roleid-permissions) |
| GET | `/sni/api/v1/admin/users/{userId}/roles` | SYSTEM:USERS | [RoleAssignmentAdminController](routes/RoleAssignmentAdminController.md#r-get-sni-api-v1-admin-users-userid-roles) |
| POST | `/sni/api/v1/admin/users/{userId}/roles` | SYSTEM:USERS | [RoleAssignmentAdminController](routes/RoleAssignmentAdminController.md#r-post-sni-api-v1-admin-users-userid-roles) |
| DELETE | `/sni/api/v1/admin/users/{userId}/roles/{roleId}` | SYSTEM:USERS | [RoleAssignmentAdminController](routes/RoleAssignmentAdminController.md#r-delete-sni-api-v1-admin-users-userid-roles-roleid) |
| POST | `/sni/api/v1/admin/provisioning/bootstrap-admin` | public | [UserProvisioningController](routes/UserProvisioningController.md#r-post-sni-api-v1-admin-provisioning-bootstrap-admin) |
| POST | `/sni/api/v1/admin/provisioning/staff` | SYSTEM:SETTINGS | [UserProvisioningController](routes/UserProvisioningController.md#r-post-sni-api-v1-admin-provisioning-staff) |
