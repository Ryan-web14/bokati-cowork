# WalletHoldController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-wallet-holds"></a>
### GET /sni/api/v1/wallet-holds

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaginatedResponse<WalletHoldResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletHoldResponse](#m-WalletHoldResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletHoldController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | requête | `String` |  |  |
| `status` | requête | `String` |  |  |
| `sourceType` | requête | `String` |  |  |
| `sourceCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-wallet-holds"></a>
### POST /sni/api/v1/wallet-holds

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `CreateWalletHoldRequest`
- Réponse : `WalletHoldResponse`
- Modèles : [WalletHoldResponse](#m-WalletHoldResponse), [CreateWalletHoldRequest](#m-CreateWalletHoldRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletHoldController.java`, méthode `create`

<a id="r-patch-sni-api-v1-wallet-holds-holdnumber-capture"></a>
### PATCH /sni/api/v1/wallet-holds/{holdNumber}/capture

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Réponse : `WalletHoldResponse`
- Modèles : [WalletHoldResponse](#m-WalletHoldResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletHoldController.java`, méthode `capture`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `holdNumber` | chemin | `String` | oui |  |
| `createdBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-wallet-holds-holdnumber-release"></a>
### PATCH /sni/api/v1/wallet-holds/{holdNumber}/release

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Réponse : `WalletHoldResponse`
- Modèles : [WalletHoldResponse](#m-WalletHoldResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletHoldController.java`, méthode `release`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `holdNumber` | chemin | `String` | oui |  |
| `createdBy` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WalletHoldResponse"></a>
#### WalletHoldResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `holdNumber` | texte |  |  |
| `walletNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `ACTIVE`, `CAPTURED`, `RELEASED`, `EXPIRED` |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CreateWalletHoldRequest"></a>
#### CreateWalletHoldRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
