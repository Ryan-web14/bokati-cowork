# VerifyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [verify](../README.md) · 3 route(s).

## Routes

<a id="r-get-verify-doc-documentnumber"></a>
### GET /verify/doc/{documentNumber}

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/verify/VerifyController.java`, méthode `verifyDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `k` | requête | `String` |  |  |

<a id="r-post-verify-doc-documentnumber-compare"></a>
### POST /verify/doc/{documentNumber}/compare

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/verify/VerifyController.java`, méthode `compareDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-get-verify-receipt-receiptnumber"></a>
### GET /verify/receipt/{receiptNumber}

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/verify/VerifyController.java`, méthode `verifyReceipt`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `receiptNumber` | chemin | `String` | oui |  |
| `k` | requête | `String` |  |  |
