# PublicPaymentPageController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 4 route(s).

## Routes

<a id="r-get-public-pay-token"></a>
### GET /public/pay/{token}

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PublicPaymentPageController.java`, méthode `open`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |

<a id="r-post-public-pay-token"></a>
### POST /public/pay/{token}

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PublicPaymentPageController.java`, méthode `pay`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |
| `country` | requête | `String` |  |  |
| `phone` | requête | `String` |  |  |
| `operator` | requête | `String` |  |  |

<a id="r-get-public-pay-token-suivi-depositid"></a>
### GET /public/pay/{token}/suivi/{depositId}

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PublicPaymentPageController.java`, méthode `status`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |
| `depositId` | chemin | `String` | oui |  |

<a id="r-get-public-pay-token-suivi-depositid-etat"></a>
### GET /public/pay/{token}/suivi/{depositId}/etat

- Accès : public, sans jeton
- Renvoie : `application/json`
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PublicPaymentPageController.java`, méthode `state`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |
| `depositId` | chemin | `String` | oui |  |
