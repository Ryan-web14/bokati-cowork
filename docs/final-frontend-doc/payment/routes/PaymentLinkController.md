# PaymentLinkController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-payment-links-token"></a>
### GET /sni/api/v1/payment-links/{token}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaymentIntentResponse`
- Modèles : [PaymentIntentResponse](#m-PaymentIntentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentLinkController.java`, méthode `resolve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PaymentIntentResponse"></a>
#### PaymentIntentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `intentNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `billingAddressJson` | texte |  |  |
| `customerRegistered` | booléen |  |  |
| `amount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `processingAmount` | nombre décimal |  |  |
| `remainingAmount` | nombre décimal |  |  |
| `payableAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `PENDING`, `PROCESSING`, `AUTHORIZED`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `EXPIRED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `REVERSED` |  |  |
| `purpose` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `resolvedSourceType` | texte |  |  |
| `resolvedSourceCode` | texte |  |  |
| `resolvedSourceLabel` | texte |  |  |
| `resolvedSourceRegistered` | booléen |  |  |
| `paymentLinkToken` | texte |  |  |
| `paymentLinkExpiresAt` | date-heure ISO 8601 UTC |  |  |
| `idempotencyKey` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `metadataJson` | texte |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `cancelledBy` | texte |  |  |
| `cancellationReason` | texte |  |  |
| `relaunchCount` | entier |  |  |
| `lastRelaunchedAt` | date-heure ISO 8601 UTC |  |  |
| `lastRelaunchedBy` | texte |  |  |
| `relaunchable` | booléen |  |  |
| `cancellable` | booléen |  |  |
