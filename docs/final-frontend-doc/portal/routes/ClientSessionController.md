# ClientSessionController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 3 route(s).

## Routes

<a id="r-delete-sni-api-v1-client-sessions"></a>
### DELETE /sni/api/v1/client/sessions

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/session/controller/ClientSessionController.java`, méthode `revokeAllSessions`

<a id="r-get-sni-api-v1-client-sessions"></a>
### GET /sni/api/v1/client/sessions

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientSessionResponse>`
- Modèles : [ClientSessionResponse](#m-ClientSessionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/session/controller/ClientSessionController.java`, méthode `listSessions`

<a id="r-delete-sni-api-v1-client-sessions-sessionid"></a>
### DELETE /sni/api/v1/client/sessions/{sessionId}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/session/controller/ClientSessionController.java`, méthode `revokeSession`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClientSessionResponse"></a>
#### ClientSessionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `active` | booléen |  |  |
