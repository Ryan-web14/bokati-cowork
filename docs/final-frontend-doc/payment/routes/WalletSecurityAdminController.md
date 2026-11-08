# WalletSecurityAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-wallets-walletnumber-limits"></a>
### GET /sni/api/v1/wallets/{walletNumber}/limits

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `WalletLimitService.LimitSnapshot`
- Modèles : [WalletLimitService.LimitSnapshot](#m-WalletLimitService-LimitSnapshot)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/security/controller/WalletSecurityAdminController.java`, méthode `limits`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-walletnumber-security-pin-reset"></a>
### POST /sni/api/v1/wallets/{walletNumber}/security/pin/reset

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/security/controller/WalletSecurityAdminController.java`, méthode `resetPin`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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
