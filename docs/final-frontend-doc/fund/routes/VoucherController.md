# VoucherController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [fund](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-fund-needs-number-pdf"></a>
### GET /sni/api/v1/fund-needs/{number}/pdf

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/fund/voucher/VoucherController.java`, méthode `need`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-requests-number-disbursements-movementnumber-pdf"></a>
### GET /sni/api/v1/fund-requests/{number}/disbursements/{movementNumber}/pdf

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/fund/voucher/VoucherController.java`, méthode `disbursement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `movementNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-requests-number-justification-pdf"></a>
### GET /sni/api/v1/fund-requests/{number}/justification/pdf

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/fund/voucher/VoucherController.java`, méthode `justification`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-requests-number-pdf"></a>
### GET /sni/api/v1/fund-requests/{number}/pdf

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/fund/voucher/VoucherController.java`, méthode `request`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-verify-voucher-number"></a>
### GET /verify/voucher/{number}

- Accès : public, sans jeton
- Réponse : `VoucherController.Verification`
- Modèles : [VoucherController.Verification](#m-VoucherController-Verification)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/voucher/VoucherController.java`, méthode `verify`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-VoucherController-Verification"></a>
#### VoucherController.Verification

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `type` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |
| `printCount` | entier |  |  |
| `status` | texte |  |  |
