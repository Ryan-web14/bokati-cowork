# SuperAdminSetupFormController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-auth-super-admin-setup-form"></a>
### GET /sni/api/v1/auth/super-admin-setup/form

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/security/superadmin/SuperAdminSetupFormController.java`, méthode `form`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-auth-super-admin-setup-form"></a>
### POST /sni/api/v1/auth/super-admin-setup/form

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/security/superadmin/SuperAdminSetupFormController.java`, méthode `submit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | requête | `String` | oui |  |
| `firstname` | requête | `String` |  |  |
| `lastname` | requête | `String` |  |  |
| `email` | requête | `String` |  |  |
| `password` | requête | `String` |  |  |
| `confirmPassword` | requête | `String` |  |  |
| `_csrf` | requête | `String` |  |  |
