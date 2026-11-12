# WalletIntegrityController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-wallets-walletnumber-integrity"></a>
### GET /sni/api/v1/wallets/{walletNumber}/integrity

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `WalletIntegrityService.IntegrityReport`
- Modèles : [WalletIntegrityService.IntegrityReport](#m-WalletIntegrityService-IntegrityReport)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/integrity/controller/WalletIntegrityController.java`, méthode `verify`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WalletIntegrityService-IntegrityReport"></a>
#### WalletIntegrityService.IntegrityReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte |  |  |
| `entriesChecked` | entier |  |  |
| `brokenLinks` | texte[] |  |  |
| `unchained` | entier |  |  |
| `expectedBalance` | nombre décimal |  |  |
| `actualBalance` | nombre décimal |  |  |
| `balanceDrift` | nombre décimal |  |  |
