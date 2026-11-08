# OneTimeTokenAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-one-time-tokens"></a>
### GET /sni/api/v1/admin/one-time-tokens

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<OneTimeTokenAdminController.OneTimeTokenView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [OneTimeTokenAdminController.OneTimeTokenView](#m-OneTimeTokenAdminController-OneTimeTokenView)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/OneTimeTokenAdminController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `email` | requête | `String` |  |  |
| `used` | requête | `Boolean` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-OneTimeTokenAdminController-OneTimeTokenView"></a>
#### OneTimeTokenAdminController.OneTimeTokenView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `token` | texte |  |  |
| `email` | texte |  |  |
| `userId` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `expiredAt` | date-heure ISO 8601 UTC |  |  |
| `used` | booléen |  |  |
| `expired` | booléen |  |  |
| `status` | texte |  |  |
