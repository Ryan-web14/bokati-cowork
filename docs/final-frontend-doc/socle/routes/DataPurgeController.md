# DataPurgeController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 1 route(s).

## Routes

<a id="r-delete-sni-api-v1-admin-maintenance-purge-data"></a>
### DELETE /sni/api/v1/admin/maintenance/purge-data

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('ROLE_ADMIN', 'ADMIN')`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/core/maintenance/DataPurgeController.java`, méthode `purgeAllData`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `confirm` | requête | `boolean` |  | false |
