# ClientWalletSecurityController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-client-wallet-walletnumber-security"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/security

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletSecurityStatusResponse`
- Modèles : [WalletSecurityStatusResponse](#m-WalletSecurityStatusResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletSecurityController.java`, méthode `status`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-wallet-walletnumber-security-pin"></a>
### POST /sni/api/v1/client/wallet/{walletNumber}/security/pin

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `SetWalletPinRequest`
- Réponse : `WalletSecurityStatusResponse`
- Modèles : [WalletSecurityStatusResponse](#m-WalletSecurityStatusResponse), [SetWalletPinRequest](#m-SetWalletPinRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletSecurityController.java`, méthode `setPin`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WalletSecurityStatusResponse"></a>
#### WalletSecurityStatusResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte |  |  |
| `pinDefined` | booléen |  |  |
| `pinMustChange` | booléen |  |  |
| `pinExpired` | booléen |  |  |
| `locked` | booléen |  |  |
| `lockedForMinutes` | entier int64 ⚠ |  |  |
| `pinLength` | entier |  |  |
| `operationsRequiringPin` | texte[] |  |  |
| `limits` | [WalletLimitService.LimitSnapshot](#m-WalletLimitService-LimitSnapshot) |  |  |

<a id="m-SetWalletPinRequest"></a>
#### SetWalletPinRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currentPin` | texte |  |  |
| `newPin` | texte | oui | longueur min `1` |

<a id="m-WalletLimitService-LimitSnapshot"></a>
#### WalletLimitService.LimitSnapshot

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `policyCode` | texte |  |  |
| `policyName` | texte |  |  |
| `kycLevel` | entier |  |  |
| `currency` | texte |  |  |
| `maxSingleTransfer` | nombre décimal |  |  |
| `dailyTransferUsed` | nombre décimal |  |  |
| `maxDailyTransfer` | nombre décimal |  |  |
| `remainingDailyTransfer` | nombre décimal |  |  |
| `monthlyTransferUsed` | nombre décimal |  |  |
| `maxMonthlyTransfer` | nombre décimal |  |  |
| `remainingMonthlyTransfer` | nombre décimal |  |  |
| `dailyTopUpUsed` | nombre décimal |  |  |
| `maxDailyTopUp` | nombre décimal |  |  |
| `remainingDailyTopUp` | nombre décimal |  |  |
| `maxBalance` | nombre décimal |  |  |
| `maxDailyOperations` | entier |  |  |
| `dailyOperationsUsed` | entier int64 ⚠ |  |  |
| `upgradePath` | texte |  |  |
