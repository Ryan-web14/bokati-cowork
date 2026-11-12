# CashDocumentController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-movements-movementnumber-document"></a>
### GET /sni/api/v1/cash-registers/movements/{movementNumber}/document

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashDocumentController.java`, méthode `movementDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-requests-requestnumber-document"></a>
### GET /sni/api/v1/cash-registers/requests/{requestNumber}/document

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashDocumentController.java`, méthode `requestDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `requestNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-sessions-sessionnumber-closing-report"></a>
### GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/closing-report

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashDocumentController.java`, méthode `sessionClosingReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |
