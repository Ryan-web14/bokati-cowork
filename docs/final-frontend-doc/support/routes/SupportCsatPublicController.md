# SupportCsatPublicController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [support](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-public-support-csat"></a>
### GET /sni/api/v1/public/support/csat

- Accès : public, sans jeton
- Renvoie : `text/html`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportCsatPublicController.java`, méthode `submit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ticket` | requête | `String` | oui |  |
| `score` | requête | `Integer` | oui |  |
| `token` | requête | `String` | oui |  |
