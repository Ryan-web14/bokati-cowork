# PasswordResetFormController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-auth-password-reset-form"></a>
### GET /sni/api/v1/auth/password-reset/form

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/PasswordResetFormController.java`, méthode `showForm`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-auth-password-reset-form"></a>
### POST /sni/api/v1/auth/password-reset/form

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/PasswordResetFormController.java`, méthode `processForm`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | requête | `String` | oui |  |
| `newPassword` | requête | `String` |  |  |
| `password` | requête | `String` |  |  |
| `confirmPassword` | requête | `String` | oui |  |
| `_csrf` | requête | `String` |  |  |
